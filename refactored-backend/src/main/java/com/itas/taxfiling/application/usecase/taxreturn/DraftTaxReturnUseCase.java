package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.RegistrationProjectionPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
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

/**
 * Drafts a new TaxReturn (BUC-FIL-001/002). Reads the rule package from
 * tax-type-engine SDK (Rule 9), the taxpayer status from the registration
 * projection (Rule 10), persists the aggregate, and drains its events.
 */
@Service
@RequiredArgsConstructor
public class DraftTaxReturnUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final RegistrationProjectionPort registration;
    private final TaxTypeEnginePort taxTypeEngine;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public TaxReturn execute(String tin, TaxTypeCode taxType, Period period, FilingMethod method) {
        var snapshot = registration.findByTin(tin)
            .orElseThrow(() -> new DomainException("taxpayer not found: " + tin));
        if (!snapshot.isActive()) {
            throw new DomainException("taxpayer not active: " + tin);
        }

        taxReturns.findByTinAndTaxTypeAndPeriod(tin, taxType, period).ifPresent(existing -> {
            throw new DomainException("return already exists for " + tin + "/" + taxType + "/" + period.label());
        });

        RulePackageVersion pkg = taxTypeEngine.currentRulePackage(taxType, period.start());
        TaxpayerReference taxpayer = new TaxpayerReference(tin, snapshot.partyId());

        TaxReturn draft = TaxReturn.draft(taxpayer, taxType, period, method, pkg);
        // Pre-seed the schedules the tax-type-engine says this tax type needs
        // (Rule 9). The wizard reads details.schedules and renders one step
        // per schedule — no per-tax-type hard-coding in the UI.
        for (var spec : taxTypeEngine.schedulesFor(taxType, pkg)) {
            draft.addSchedule(spec.kind(), spec.label());
        }
        TaxReturn saved = taxReturns.save(draft);
        saved.pullEvents().forEach(eventPublisher::publish);
        return saved;
    }
}
