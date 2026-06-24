package com.itas.taxfiling.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.Optional;

/**
 * Admin-managed mapping from tax type → ledger template references.
 *
 * <p>Filing-service posts at most two ledger journals when a return is
 * accepted (BUC-FIL-013):
 * <ul>
 *   <li>The principal assessment, which uses {@code filingTemplateRef}.</li>
 *   <li>If the return was filed past its due date, a one-off late-filing
 *       penalty journal that uses {@code lateFilingPenaltyTemplateRef}
 *       (nullable — when not configured for a tax type, no penalty is
 *       posted).</li>
 * </ul>
 *
 * <p>Both refs are owned by the ledger team and surfaced via
 * {@code GET /api/ledger/v1/config/templates}; the back-office screen at
 * {@code /filing/templates} just lets admins associate one ref to a tax
 * type without redeploying.
 */
@Getter
public class FilingTemplateMapping {

    private final String taxTypeCode;
    private String filingTemplateRef;
    private String lateFilingPenaltyTemplateRef;
    private String createdBy;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;

    private FilingTemplateMapping(String taxTypeCode) {
        this.taxTypeCode = taxTypeCode;
    }

    public static FilingTemplateMapping create(
            String taxTypeCode,
            String filingTemplateRef,
            String lateFilingPenaltyTemplateRef,
            String actorId) {
        ensureNonBlank("taxTypeCode", taxTypeCode);
        ensureNonBlank("filingTemplateRef", filingTemplateRef);
        ensureNonBlank("actorId", actorId);
        FilingTemplateMapping m = new FilingTemplateMapping(taxTypeCode.trim());
        m.filingTemplateRef = filingTemplateRef.trim();
        m.lateFilingPenaltyTemplateRef =
            lateFilingPenaltyTemplateRef == null || lateFilingPenaltyTemplateRef.isBlank()
                ? null : lateFilingPenaltyTemplateRef.trim();
        Instant now = Instant.now();
        m.createdBy = actorId;
        m.createdAt = now;
        m.updatedBy = actorId;
        m.updatedAt = now;
        return m;
    }

    public static FilingTemplateMapping rehydrate(
            String taxTypeCode,
            String filingTemplateRef,
            String lateFilingPenaltyTemplateRef,
            String createdBy,
            Instant createdAt,
            String updatedBy,
            Instant updatedAt) {
        FilingTemplateMapping m = new FilingTemplateMapping(taxTypeCode);
        m.filingTemplateRef = filingTemplateRef;
        m.lateFilingPenaltyTemplateRef = lateFilingPenaltyTemplateRef;
        m.createdBy = createdBy;
        m.createdAt = createdAt;
        m.updatedBy = updatedBy;
        m.updatedAt = updatedAt;
        return m;
    }

    public void update(
            String filingTemplateRef,
            String lateFilingPenaltyTemplateRef,
            String actorId) {
        ensureNonBlank("filingTemplateRef", filingTemplateRef);
        ensureNonBlank("actorId", actorId);
        this.filingTemplateRef = filingTemplateRef.trim();
        this.lateFilingPenaltyTemplateRef =
            lateFilingPenaltyTemplateRef == null || lateFilingPenaltyTemplateRef.isBlank()
                ? null : lateFilingPenaltyTemplateRef.trim();
        this.updatedBy = actorId;
        this.updatedAt = Instant.now();
    }

    public Optional<String> lateFilingPenaltyTemplate() {
        return Optional.ofNullable(lateFilingPenaltyTemplateRef);
    }

    private static void ensureNonBlank(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
