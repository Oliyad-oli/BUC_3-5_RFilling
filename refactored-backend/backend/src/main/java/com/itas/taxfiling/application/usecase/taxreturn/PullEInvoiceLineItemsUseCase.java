package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EInvoiceServicePort;
import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.LineItem;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.EntrySpecificData;
import com.itas.taxfiling.domain.valueobject.EntryTypeStatus;
import com.itas.taxfiling.domain.valueobject.LineItemSource;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Pulls e-invoice line items for the taxpayer/period and appends them to the
 * given Schedule (BUC-FIL-005). Each line carries source = E_INVOICE so the
 * audit trail records they came from outside.
 *
 * <p>The pull is always scoped to the tax return's filing period — no
 * separate user-supplied date window. Dedup still applies: every line carries
 * an {@code externalInvoiceId} on its entryData and any incoming line whose
 * id already lives on the target schedule is silently skipped.
 */
@Service
@RequiredArgsConstructor
public class PullEInvoiceLineItemsUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final LineItemEntryTypeRepositoryPort entryTypes;
    private final EInvoiceServicePort eInvoice;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public PullOutcome execute(UUID taxReturnId, UUID scheduleId, UUID entryTypeId) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        LineItemEntryType type = entryTypes.findById(entryTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("entry type not found: " + entryTypeId));
        if (type.getStatus() == EntryTypeStatus.RETIRED) {
            throw new DomainException("entry type retired: " + type.getCode());
        }
        ScheduleKind kind = type.getScheduleKind();
        if (kind != ScheduleKind.SALES && kind != ScheduleKind.PURCHASES) {
            throw new DomainException("e-invoice pull requires SALES or PURCHASES entry type");
        }

        // Dedup snapshot: every externalInvoiceId already on this schedule.
        Set<String> existingExternalIds = existingExternalInvoiceIds(t, scheduleId);

        List<EInvoiceServicePort.EInvoiceLine> incoming = eInvoice.pullForTaxpayer(
            t.getTaxpayer().tin(), t.getPeriod(), kind);

        int added = 0;
        int skipped = 0;
        for (var line : incoming) {
            if (line.externalInvoiceId() != null
                && existingExternalIds.contains(line.externalInvoiceId())) {
                skipped++;
                continue;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            // Adapter delivers entry-type-shaped keys in `extra`; copy those
            // first, then add the audit-trail backstops (externalInvoiceId is
            // also what powers dedup on the next pull).
            data.putAll(line.extra());
            data.put("externalInvoiceId", line.externalInvoiceId());
            t.addLineItem(scheduleId, type.getId(), type.getVersion(),
                line.amount(), LineItemSource.E_INVOICE, new EntrySpecificData(data));
            existingExternalIds.add(line.externalInvoiceId());
            added++;
        }

        var events = t.pullEvents();
        taxReturns.save(t);
        events.forEach(eventPublisher::publish);
        return new PullOutcome(added, skipped);
    }

    /** Walk the target schedule and collect every {@code externalInvoiceId} already on a line. */
    private Set<String> existingExternalInvoiceIds(TaxReturn t, UUID scheduleId) {
        Set<String> ids = new HashSet<>();
        Schedule schedule = t.getSchedules().stream()
            .filter(s -> s.getId().equals(scheduleId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                "schedule not found: " + scheduleId));
        for (LineItem li : schedule.getLineItems()) {
            Object externalId = li.getEntryData().get("externalInvoiceId");
            if (externalId != null) ids.add(Objects.toString(externalId));
        }
        return ids;
    }

    /**
     * Outcome of a single pull call. {@code added} is the number of new line
     * items appended to the schedule; {@code skipped} is the number of incoming
     * e-invoices that were dropped because their externalInvoiceId was already
     * on the schedule.
     */
    public record PullOutcome(int added, int skipped) {}
}
