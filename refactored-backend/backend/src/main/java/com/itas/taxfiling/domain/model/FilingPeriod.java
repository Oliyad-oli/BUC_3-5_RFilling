package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.aggregate.AggregateRoot;
import com.itas.taxfiling.domain.event.FilingPeriodGeneratedEvent;
import com.itas.taxfiling.domain.event.FilingPeriodStatusChangedEvent;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.valueobject.FilingPeriodStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One filing slot (period) for a single taxpayer obligation. Layer 2 of the
 * calendar model — taxpayer-specific anchor on top of the global calendar.
 *
 * <p>Lazy materialization (Rule 5 follow-up): a row is created only on first
 * use — when the taxpayer clicks "File Now", when a return is filed via
 * bulk-upload, or when compliance opens a case. The dashboard derives status
 * for un-materialized periods on the fly from the calendar. {@code FILED} is
 * set by the handler that reacts to a TaxReturn reaching COMPLETED.
 */
public class FilingPeriod extends AggregateRoot {

    private final UUID id;
    private final UUID obligationId;
    private final String tin;
    private final String taxTypeCode;
    private final String periodLabel;
    private final LocalDate coversFrom;
    private final LocalDate coversTo;
    private final LocalDate dueDate;
    private final boolean isPartial;
    private FilingPeriodStatus status;
    private UUID taxReturnId;
    private Instant filedAt;
    private final Instant createdAt;
    private Long version;

    private FilingPeriod(UUID id, UUID obligationId, String tin, String taxTypeCode,
                         String periodLabel, LocalDate coversFrom, LocalDate coversTo,
                         LocalDate dueDate, boolean isPartial, FilingPeriodStatus status,
                         UUID taxReturnId, Instant filedAt, Instant createdAt) {
        this.id = id;
        this.obligationId = obligationId;
        this.tin = tin;
        this.taxTypeCode = taxTypeCode;
        this.periodLabel = periodLabel;
        this.coversFrom = coversFrom;
        this.coversTo = coversTo;
        this.dueDate = dueDate;
        this.isPartial = isPartial;
        this.status = status;
        this.taxReturnId = taxReturnId;
        this.filedAt = filedAt;
        this.createdAt = createdAt;
    }

    /**
     * Factory called by {@code GenerateInitialFilingPeriodsUseCase}. The status
     * is derived from {@code today} so a period generated retroactively for a
     * past period starts in the right state.
     */
    public static FilingPeriod generate(UUID obligationId, String tin, String taxTypeCode,
                                        String periodLabel, LocalDate coversFrom,
                                        LocalDate coversTo, LocalDate dueDate,
                                        boolean isPartial, LocalDate today) {
        Objects.requireNonNull(obligationId, "obligationId");
        Objects.requireNonNull(coversFrom, "coversFrom");
        Objects.requireNonNull(coversTo, "coversTo");
        Objects.requireNonNull(dueDate, "dueDate");
        if (coversTo.isBefore(coversFrom)) throw new DomainException("coversTo before coversFrom");

        FilingPeriodStatus initial = statusFor(coversFrom, coversTo, dueDate, today);
        Instant now = Instant.now();
        FilingPeriod p = new FilingPeriod(
            UUID.randomUUID(), obligationId, tin, taxTypeCode, periodLabel,
            coversFrom, coversTo, dueDate, isPartial, initial, null, null, now);
        p.registerEvent(new FilingPeriodGeneratedEvent(
            UUID.randomUUID(), now, p.id, obligationId, tin, taxTypeCode, periodLabel,
            coversFrom, coversTo, dueDate, isPartial, initial));
        return p;
    }

    /** Compute the date-driven status as of {@code today}. */
    public static FilingPeriodStatus statusFor(LocalDate coversFrom, LocalDate coversTo,
                                               LocalDate dueDate, LocalDate today) {
        if (today.isBefore(coversFrom)) return FilingPeriodStatus.FUTURE;
        if (today.isAfter(dueDate))     return FilingPeriodStatus.OVERDUE;
        if (today.isAfter(coversTo))    return FilingPeriodStatus.DUE;
        return FilingPeriodStatus.OPEN;
    }

    /** Date-driven status flip applied by the daily job. No-op if FILED. */
    public void recomputeStatus(LocalDate today) {
        if (status == FilingPeriodStatus.FILED) return;
        FilingPeriodStatus next = statusFor(coversFrom, coversTo, dueDate, today);
        if (next == status) return;
        FilingPeriodStatus prev = status;
        this.status = next;
        registerEvent(new FilingPeriodStatusChangedEvent(
            UUID.randomUUID(), Instant.now(), id, tin, taxTypeCode, periodLabel, prev, next));
    }

    /** Link this period to a tax return when the wizard starts. Idempotent. */
    public void linkTaxReturn(UUID taxReturnId) {
        Objects.requireNonNull(taxReturnId, "taxReturnId");
        if (status == FilingPeriodStatus.FILED) {
            throw new DomainException("cannot link to FILED period " + id);
        }
        if (this.taxReturnId != null && !this.taxReturnId.equals(taxReturnId)) {
            throw new DomainException("period already linked to tax return " + this.taxReturnId);
        }
        this.taxReturnId = taxReturnId;
    }

    /** Mark the period FILED on TaxReturn completion. Idempotent. */
    public void markFiled(UUID taxReturnId, Instant filedAt) {
        if (status == FilingPeriodStatus.FILED) return;
        Objects.requireNonNull(taxReturnId, "taxReturnId");
        Objects.requireNonNull(filedAt, "filedAt");
        FilingPeriodStatus prev = status;
        this.status = FilingPeriodStatus.FILED;
        this.taxReturnId = taxReturnId;
        this.filedAt = filedAt;
        registerEvent(new FilingPeriodStatusChangedEvent(
            UUID.randomUUID(), Instant.now(), id, tin, taxTypeCode, periodLabel,
            prev, FilingPeriodStatus.FILED));
    }

    @Override public UUID getId() { return id; }
    public UUID getObligationId() { return obligationId; }
    public String getTin() { return tin; }
    public String getTaxTypeCode() { return taxTypeCode; }
    public String getPeriodLabel() { return periodLabel; }
    public LocalDate getCoversFrom() { return coversFrom; }
    public LocalDate getCoversTo() { return coversTo; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isPartial() { return isPartial; }
    public FilingPeriodStatus getStatus() { return status; }
    public Optional<UUID> getTaxReturnId() { return Optional.ofNullable(taxReturnId); }
    public Optional<Instant> getFiledAt() { return Optional.ofNullable(filedAt); }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }

    /** Adapter-only rehydration. */
    public static FilingPeriod rehydrate(UUID id, UUID obligationId, String tin, String taxTypeCode,
                                         String periodLabel, LocalDate coversFrom, LocalDate coversTo,
                                         LocalDate dueDate, boolean isPartial, FilingPeriodStatus status,
                                         UUID taxReturnId, Instant filedAt, Instant createdAt,
                                         Long version) {
        FilingPeriod p = new FilingPeriod(id, obligationId, tin, taxTypeCode, periodLabel,
            coversFrom, coversTo, dueDate, isPartial, status, taxReturnId, filedAt, createdAt);
        p.version = version;
        p.pullEvents();
        return p;
    }
}
