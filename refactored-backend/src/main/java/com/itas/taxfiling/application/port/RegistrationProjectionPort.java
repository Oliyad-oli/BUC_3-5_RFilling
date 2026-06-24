package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.valueobject.SubledgerGroupReference;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.util.List;
import java.util.Optional;

/**
 * Read-only projection of taxpayer state from registration-service (Rule 10).
 * Filing reads: taxpayer status (active / suspended / deregistered) and the
 * subledger UUIDs that exist for (tin × taxType). Filing never calls
 * party-service or registration-service directly — only consumes the projection.
 */
public interface RegistrationProjectionPort {

    Optional<TaxpayerSnapshot> findByTin(String tin);

    Optional<SubledgerGroupReference> findSubledgers(String tin, TaxTypeCode taxType);

    /** All active taxpayers — used by OpenFilingPeriodJob (BUC-FIL-001). */
    List<TaxpayerSnapshot> listActive();

    /** Webhook-driven projection write (Rule 10). */
    void upsertTaxpayer(String tin, String partyId, String legalName, String status, boolean active);

    /** Webhook-driven subledger projection write (Rule 10). */
    void upsertSubledgers(String tin, TaxTypeCode taxType,
                          java.util.UUID principal, java.util.UUID penalty,
                          java.util.UUID interest, java.util.UUID refund);

    record TaxpayerSnapshot(
        String tin,
        String partyId,
        String legalName,
        String status,
        boolean isActive
    ) {}
}
