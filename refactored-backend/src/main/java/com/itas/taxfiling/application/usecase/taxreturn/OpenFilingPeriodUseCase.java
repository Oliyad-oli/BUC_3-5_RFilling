package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.RegistrationProjectionPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort.ScheduleSpec;
import com.itas.taxfiling.domain.event.TaxReturnPeriodOpenedEvent;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.FilingMethod;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.domain.valueobject.TaxpayerReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * BUC-FIL-001 — opens a filing period for a single (TIN × tax-type × period).
 * Creates the TaxReturn in DRAFT, pre-populates empty schedules per the rule
 * package, and emits TaxReturnPeriodOpenedEvent. Idempotent: if a return
 * already exists for the (TIN × tax-type × period), returns the existing one.
 */
@Service
@RequiredArgsConstructor
public class OpenFilingPeriodUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final RegistrationProjectionPort registration;
    private final TaxTypeEnginePort taxTypeEngine;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public TaxReturn execute(String tin, TaxTypeCode taxType, Period period) {
        Optional<TaxReturn> existing = taxReturns.findByTinAndTaxTypeAndPeriod(tin, taxType, period);
        if (existing.isPresent()) return existing.get();

        var snapshot = registration.findByTin(tin)
            .orElseThrow(() -> new DomainException("taxpayer not found: " + tin));
        if (!snapshot.isActive()) {
            throw new DomainException("taxpayer not active: " + tin);
        }

        RulePackageVersion pkg = taxTypeEngine.currentRulePackage(taxType, period.start());
        TaxpayerReference taxpayer = new TaxpayerReference(tin, snapshot.partyId());
        TaxReturn t = TaxReturn.draft(taxpayer, taxType, period, FilingMethod.PORTAL, pkg);

        for (ScheduleSpec spec : taxTypeEngine.schedulesFor(taxType, pkg)) {
            t.addSchedule(spec.kind(), spec.label());
        }

        TaxReturn saved = taxReturns.save(t);
        saved.pullEvents().forEach(eventPublisher::publish);
        // Additionally emit the period-opened event for downstream handlers (carry-forward).
        eventPublisher.publish(new TaxReturnPeriodOpenedEvent(
            UUID.randomUUID(), Instant.now(), saved.getId(), taxpayer, taxType, period));
        return saved;
    }
}
