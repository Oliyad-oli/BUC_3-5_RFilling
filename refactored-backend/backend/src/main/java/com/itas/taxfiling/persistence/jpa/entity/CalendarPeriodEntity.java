package com.itas.taxfiling.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cached projection of the Ministry-published tax calendar (Layer 1). One row
 * per (tax_type, period) — small (a few hundred rows total), shared across all
 * taxpayers. Refreshed by RefreshCalendarProjectionUseCase on a weekly cron and
 * on CalendarPublishedEvent from tax-type-engine.
 */
@Entity
@Table(name = "calendar_period",
    uniqueConstraints = {
        @UniqueConstraint(name = "ux_calendar_period_type_label",
                          columnNames = {"tax_type_code", "period_label"})
    },
    indexes = {
        @Index(name = "ix_calendar_period_type_starts", columnList = "tax_type_code, starts_on"),
        @Index(name = "ix_calendar_period_due",         columnList = "due_on")
    })
@Getter @Setter @NoArgsConstructor
public class CalendarPeriodEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tax_type_code", nullable = false, updatable = false, length = 64)
    private String taxTypeCode;

    @Column(name = "period_label", nullable = false, updatable = false, length = 32)
    private String periodLabel;

    @Column(name = "starts_on", nullable = false, updatable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false, updatable = false)
    private LocalDate endsOn;

    @Column(name = "due_on", nullable = false, updatable = false)
    private LocalDate dueOn;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.PeriodFrequency frequency;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "refreshed_at", nullable = false)
    private Instant refreshedAt;
}
