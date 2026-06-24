package com.itas.taxfiling.persistence.jpa.entity;

import com.itas.taxfiling.persistence.converter.JsonbConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** JPA entity for LineItemEntryType (admin-config catalog, immutable rows pattern). */
@Entity
@Table(name = "line_item_entry_types",
    indexes = {
        @Index(name = "ix_lit_code_version", columnList = "code, version", unique = true),
        @Index(name = "ix_lit_taxtype_kind_status",
               columnList = "tax_type, schedule_kind, status")
    })
@Getter
@Setter
@NoArgsConstructor
public class LineItemEntryTypeEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 64)
    private String code;

    @Column(name = "tax_type", nullable = false, updatable = false, length = 32)
    private String taxType;

    @Column(name = "schedule_kind", nullable = false, updatable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.ScheduleKind scheduleKind;

    @Column(nullable = false, updatable = false)
    private int version;

    @Column(name = "fields_json", columnDefinition = "jsonb", nullable = false, updatable = false)
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> fields;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.EntryTypeStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by_actor_id", nullable = false, updatable = false, length = 128)
    private String createdByActorId;

    @Version
    private Long revision;
}
