package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort.ScheduleSpec;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.LineItem;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.EntrySpecificData;
import com.itas.taxfiling.domain.valueobject.LineItemSource;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * BUC-FIL-005 — copies standing data from the prior period's TaxReturn into
 * the newly opened return. Only schedules flagged supports_carry_forward in
 * the rule package are copied. Transactional amounts are reset to zero — only
 * identifying fields (counterparty TIN, NID, asset id) survive.
 *
 * Idempotent: if the new return already has line items in any
 * carry-forward-eligible schedule, that schedule is skipped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarryForwardLineItemsUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final TaxTypeEnginePort taxTypeEngine;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public int execute(UUID newReturnId) {
        TaxReturn target = taxReturns.findById(newReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + newReturnId));

        var carryForwardKinds = taxTypeEngine.schedulesFor(target.getTaxType(), target.getRulePackage()).stream()
            .filter(ScheduleSpec::supportsCarryForward)
            .map(ScheduleSpec::kind)
            .collect(Collectors.toSet());
        if (carryForwardKinds.isEmpty()) return 0;

        var prior = taxReturns.findPriorCompleted(
            target.getTaxpayer().tin(), target.getTaxType(), target.getPeriod().start());
        if (prior.isEmpty()) {
            log.debug("Carry-forward: no prior period for taxReturnId={}", newReturnId);
            return 0;
        }

        int copied = copyEligibleLines(prior.get(), target, carryForwardKinds);
        if (copied > 0) {
            TaxReturn saved = taxReturns.save(target);
            saved.pullEvents().forEach(eventPublisher::publish);
        }
        log.info("Carry-forward: copied {} line items into taxReturnId={}", copied, newReturnId);
        return copied;
    }

    private int copyEligibleLines(TaxReturn prior, TaxReturn target, Set<ScheduleKind> kinds) {
        int copied = 0;
        for (Schedule priorSched : prior.getSchedules()) {
            if (!kinds.contains(priorSched.getKind())) continue;
            Schedule targetSched = target.getSchedules().stream()
                .filter(s -> s.getKind() == priorSched.getKind())
                .findFirst().orElse(null);
            if (targetSched == null) continue;
            if (!targetSched.getLineItems().isEmpty()) continue;

            for (LineItem priorLi : priorSched.getLineItems()) {
                Map<String, Object> resetData = new LinkedHashMap<>(priorLi.getEntryData().values());
                resetData.put("__carry_forward_from", priorLi.getId().toString());
                target.addLineItem(
                    targetSched.getId(),
                    priorLi.getEntryTypeId(),
                    priorLi.getEntryTypeVersion(),
                    Money.zero(priorLi.getAmount().currency()),
                    LineItemSource.CARRY_FORWARD,
                    new EntrySpecificData(resetData));
                copied++;
            }
        }
        return copied;
    }

    @SuppressWarnings("unused")
    private List<LineItem> noop() { return List.of(); }
}
