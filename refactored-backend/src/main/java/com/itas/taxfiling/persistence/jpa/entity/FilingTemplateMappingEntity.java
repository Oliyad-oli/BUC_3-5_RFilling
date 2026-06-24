package com.itas.taxfiling.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "filing_template_mapping")
@Getter
@Setter
@NoArgsConstructor
public class FilingTemplateMappingEntity {

    @Id
    @Column(name = "tax_type_code", length = 32, nullable = false, updatable = false)
    private String taxTypeCode;

    @Column(name = "filing_template_ref", length = 64, nullable = false)
    private String filingTemplateRef;

    @Column(name = "late_filing_penalty_template_ref", length = 64)
    private String lateFilingPenaltyTemplateRef;

    @Column(name = "created_by", length = 64, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_by", length = 64, nullable = false)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
