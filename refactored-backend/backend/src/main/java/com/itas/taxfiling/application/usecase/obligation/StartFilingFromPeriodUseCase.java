package com.itas.taxfiling.application.usecase.obligation;

import com.itas.taxfiling.application.port.CalendarPeriodRepositoryPort;
import com.itas.taxfiling.application.port.CalendarPeriodRepositoryPort.CalendarPeriod;
import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.FilingPeriodRepositoryPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.port.TaxpayerObligationRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.DraftTaxReturnUseCase;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.FilingPeriod;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.model.TaxpayerObligation;
import com.itas.taxfiling.domain.valueobject.FilingMethod;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Bridges the dashboard's "File Now" click to a draft TaxReturn. Lazy
 * materialization model: takes {@code (tin, taxType, periodLabel)} and
 * resolves a {@code FilingPeriod} row, materializing one from the calendar
 * projection if it doesn't exist yet. Then drafts (or reuses) the TaxReturn
 * and links it to the period.
 *
 * <p>Idempotent on all three inputs — re-clicking returns the existing
 * taxReturnId without creating duplicates.
 */
@Service
@RequiredArgsConstructor
public class StartFilingFromPeriodUseCase {

    private final TaxpayerObligationRepositoryPort obligations;
    private final CalendarPeriodRepositoryPort calendarPeriods;
    private final FilingPeriodRepositoryPort periods;
    private final TaxReturnRepositoryPort taxReturns;
    private final DraftTaxReturnUseCase draftUseCase;
    private final EventPublisherPort eventPublisher;
    @PersistenceContext private EntityManager em;

    @Transactional
    public Result execute(String tin, TaxTypeCode taxType, String periodLabel) {
        // 1. Obligation must exist — created at registration via the webhook.
        TaxpayerObligation obligation = obligations.findByTinAndTaxType(tin, taxType)
            .orElseThrow(() -> new ResourceNotFoundException(
                "no obligation for tin=" + tin + " taxType=" + taxType.value()));

        // 2. Calendar row defines the fiscal window + due date.
        CalendarPeriod calendar = calendarPeriods.findByTaxTypeAndLabel(taxType, periodLabel)
            .orElseThrow(() -> new ResourceNotFoundException(
                "calendar period not found: " + taxType.value() + "/" + periodLabel));

        // 3. Materialize the FilingPeriod row lazily (or reuse).
        FilingPeriod period = periods
            .findByObligationAndLabel(obligation.getId(), periodLabel)
            .orElseGet(() -> materializePeriod(obligation, tin, taxType, calendar));

        // 4. Reuse linked tax return if start was already clicked.
        if (period.getTaxReturnId().isPresent()) {
            return new Result(period.getId(), period.getTaxReturnId().get(), false);
        }

        // 5. Find-or-draft the TaxReturn aligned to the period's window.
        Period trPeriod = new Period(period.getCoversFrom(), period.getCoversTo(),
            calendar.frequency());
        var existingReturn = taxReturns.findByTinAndTaxTypeAndPeriod(tin, taxType, trPeriod);
        UUID taxReturnId;
        boolean newlyCreated;
        if (existingReturn.isPresent()) {
            taxReturnId = existingReturn.get().getId();
            newlyCreated = false;
        } else {
            try {
                TaxReturn draft = draftUseCase.execute(tin, taxType, trPeriod, FilingMethod.PORTAL);
                em.flush();
                taxReturnId = draft.getId();
                newlyCreated = true;
            } catch (org.springframework.dao.DataIntegrityViolationException race) {
                em.clear();
                taxReturnId = taxReturns
                    .findByTinAndTaxTypeAndPeriod(tin, taxType, trPeriod)
                    .orElseThrow(() -> race)
                    .getId();
                newlyCreated = false;
            }
        }

        // 6. Link the period to the return.
        period.linkTaxReturn(taxReturnId);
        FilingPeriod saved = periods.save(period);
        saved.pullEvents().forEach(eventPublisher::publish);
        return new Result(saved.getId(), taxReturnId, newlyCreated);
    }

    private FilingPeriod materializePeriod(TaxpayerObligation obligation, String tin,
                                           TaxTypeCode taxType, CalendarPeriod calendar) {
        // Partial-first-period rule: covers_from = max(effectiveFrom, calendar.startsOn).
        LocalDate coversFrom = obligation.getEffectiveFrom().isAfter(calendar.startsOn())
            ? obligation.getEffectiveFrom()
            : calendar.startsOn();
        boolean isPartial = !coversFrom.equals(calendar.startsOn());
        if (coversFrom.isAfter(calendar.endsOn())) {
            throw new DomainException(
                "obligation effective_from is after period end — taxpayer not liable for "
                    + calendar.periodLabel());
        }
        FilingPeriod fresh = FilingPeriod.generate(
            obligation.getId(), tin, taxType.value(), calendar.periodLabel(),
            coversFrom, calendar.endsOn(), calendar.dueOn(), isPartial, LocalDate.now());
        FilingPeriod saved = periods.save(fresh);
        saved.pullEvents().forEach(eventPublisher::publish);
        return saved;
    }

    public record Result(UUID filingPeriodId, UUID taxReturnId, boolean newlyCreated) {}
}
