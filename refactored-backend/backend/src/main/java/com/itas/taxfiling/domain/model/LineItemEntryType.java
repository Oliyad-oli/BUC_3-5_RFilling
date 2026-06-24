package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.aggregate.AggregateRoot;
import com.itas.taxfiling.domain.event.LineItemEntryTypeRegisteredEvent;
import com.itas.taxfiling.domain.event.LineItemEntryTypeRetiredEvent;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntryTypeStatus;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Admin-configurable definition of a line-item shape (Rule 11, BUC-FIL-CONFIG-01).
 * Immutable-rows pattern: rows are never edited in place — a new version row is
 * inserted; older versions are RETIRED so historical line items keep their
 * original definition for audit and validation replay.
 */
public class LineItemEntryType extends AggregateRoot {

    private final UUID id;
    private final String code;
    private final TaxTypeCode taxType;
    private final ScheduleKind scheduleKind;
    private final int version;
    private final List<EntryFieldDefinition> fields;
    private EntryTypeStatus status;
    private final Instant createdAt;
    private final String createdByActorId;
    private Long jpaVersion;

    private LineItemEntryType(UUID id, String code, TaxTypeCode taxType, ScheduleKind scheduleKind,
                              int version, List<EntryFieldDefinition> fields, EntryTypeStatus status,
                              Instant createdAt, String createdByActorId) {
        this.id = id;
        this.code = code;
        this.taxType = taxType;
        this.scheduleKind = scheduleKind;
        this.version = version;
        this.fields = List.copyOf(fields);
        this.status = status;
        this.createdAt = createdAt;
        this.createdByActorId = createdByActorId;
    }

    public static LineItemEntryType register(String code, TaxTypeCode taxType,
                                             ScheduleKind scheduleKind, int version,
                                             List<EntryFieldDefinition> fields, String adminActorId) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(taxType, "taxType");
        Objects.requireNonNull(scheduleKind, "scheduleKind");
        Objects.requireNonNull(fields, "fields");
        Objects.requireNonNull(adminActorId, "adminActorId");
        if (code.isBlank()) throw new DomainException("code must be non-blank");
        if (version < 1)   throw new DomainException("version must be >= 1");

        LineItemEntryType type = new LineItemEntryType(
            UUID.randomUUID(), code, taxType, scheduleKind, version, fields,
            EntryTypeStatus.ACTIVE, Instant.now(), adminActorId);
        type.registerEvent(new LineItemEntryTypeRegisteredEvent(
            UUID.randomUUID(), Instant.now(), type.id, code, taxType, version, adminActorId));
        return type;
    }

    public void retire(String adminActorId) {
        if (status != EntryTypeStatus.ACTIVE) {
            throw new DomainException("only ACTIVE entry types can be retired (was " + status + ")");
        }
        status = EntryTypeStatus.RETIRED;
        registerEvent(new LineItemEntryTypeRetiredEvent(
            UUID.randomUUID(), Instant.now(), id, adminActorId));
    }

    @Override public UUID getId() { return id; }
    public String getCode() { return code; }
    public TaxTypeCode getTaxType() { return taxType; }
    public ScheduleKind getScheduleKind() { return scheduleKind; }
    public int getVersion() { return version; }
    public List<EntryFieldDefinition> getFields() { return fields; }
    public EntryTypeStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedByActorId() { return createdByActorId; }
    public Long getJpaVersion() { return jpaVersion; }

    public static LineItemEntryType rehydrate(UUID id, String code, TaxTypeCode taxType,
                                              ScheduleKind scheduleKind, int version,
                                              List<EntryFieldDefinition> fields, EntryTypeStatus status,
                                              Instant createdAt, String createdByActorId, Long jpaVersion) {
        LineItemEntryType t = new LineItemEntryType(id, code, taxType, scheduleKind, version, fields,
            status, createdAt, createdByActorId);
        t.jpaVersion = jpaVersion;
        t.pullEvents();
        return t;
    }
}
