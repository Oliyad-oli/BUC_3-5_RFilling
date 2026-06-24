package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.aggregate.AggregateRoot;
import com.itas.taxfiling.domain.event.TaxpayerObligationCreatedEvent;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * "I, taxpayer X, must file returns of type T" — one row per (TIN × tax type).
 * Anchored to the registration's effective_from date; this becomes the lower
 * bound of the first filing period (Layer 2 of the calendar model).
 *
 * <p>Lifecycle: {@code create() → close()}. While open, the daily roll-forward
 * job keeps generating new {@link FilingPeriod}s as the global calendar
 * advances. Closing it (taxpayer deregistration) stops generation and is
 * the FilingPeriod side's signal to clip any open period.
 */
public class TaxpayerObligation extends AggregateRoot {

    private final UUID id;
    private final String tin;
    private final String partyId;
    private final TaxTypeCode taxType;
    private final PeriodFrequency frequency;
    private final LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private final Instant createdAt;
    private Long version;

    private TaxpayerObligation(UUID id, String tin, String partyId, TaxTypeCode taxType,
                               PeriodFrequency frequency, LocalDate effectiveFrom,
                               LocalDate effectiveTo, Instant createdAt) {
        this.id = id;
        this.tin = tin;
        this.partyId = partyId;
        this.taxType = taxType;
        this.frequency = frequency;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
    }

    public static TaxpayerObligation create(String tin, String partyId, TaxTypeCode taxType,
                                            PeriodFrequency frequency, LocalDate effectiveFrom) {
        Objects.requireNonNull(tin, "tin");
        Objects.requireNonNull(partyId, "partyId");
        Objects.requireNonNull(taxType, "taxType");
        Objects.requireNonNull(frequency, "frequency");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        if (tin.isBlank()) throw new DomainException("tin must not be blank");

        Instant now = Instant.now();
        TaxpayerObligation o = new TaxpayerObligation(
            UUID.randomUUID(), tin, partyId, taxType, frequency, effectiveFrom, null, now);
        o.registerEvent(new TaxpayerObligationCreatedEvent(
            UUID.randomUUID(), now, o.id, tin, partyId, taxType, frequency, effectiveFrom));
        return o;
    }

    public void close(LocalDate on) {
        if (effectiveTo != null) throw new DomainException("obligation already closed");
        if (on == null || on.isBefore(effectiveFrom))
            throw new DomainException("close date must be on or after effectiveFrom");
        this.effectiveTo = on;
    }

    public boolean isActive() { return effectiveTo == null; }

    @Override public UUID getId() { return id; }
    public String getTin() { return tin; }
    public String getPartyId() { return partyId; }
    public TaxTypeCode getTaxType() { return taxType; }
    public PeriodFrequency getFrequency() { return frequency; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public Optional<LocalDate> getEffectiveTo() { return Optional.ofNullable(effectiveTo); }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }

    /** Adapter-only rehydration. */
    public static TaxpayerObligation rehydrate(UUID id, String tin, String partyId,
                                               TaxTypeCode taxType, PeriodFrequency frequency,
                                               LocalDate effectiveFrom, LocalDate effectiveTo,
                                               Instant createdAt, Long version) {
        TaxpayerObligation o = new TaxpayerObligation(
            id, tin, partyId, taxType, frequency, effectiveFrom, effectiveTo, createdAt);
        o.version = version;
        o.pullEvents();
        return o;
    }
}
