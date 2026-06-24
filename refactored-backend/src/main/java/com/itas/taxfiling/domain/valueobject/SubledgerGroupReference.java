package com.itas.taxfiling.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * The 4 subledger UUIDs that exist per (TIN × tax type). Created at registration time
 * by the ledger-engine and projected into filing-service via the registration-service webhook.
 * Filing never asks ledger to create subledgers — it only posts into them by category.
 *
 * Required by BUC-EFR-003/005: ledger posting on return completion uses the
 * principalSubledgerId to record the tax liability entry.
 */
public record SubledgerGroupReference(
    UUID principalSubledgerId,
    UUID penaltySubledgerId,
    UUID interestSubledgerId,
    UUID refundSubledgerId
) {
    public SubledgerGroupReference {
        Objects.requireNonNull(principalSubledgerId, "principalSubledgerId");
        Objects.requireNonNull(penaltySubledgerId, "penaltySubledgerId");
        Objects.requireNonNull(interestSubledgerId, "interestSubledgerId");
        Objects.requireNonNull(refundSubledgerId, "refundSubledgerId");
    }

    public UUID forCategory(AccountCategory category) {
        return switch (category) {
            case PRINCIPAL -> principalSubledgerId;
            case PENALTY   -> penaltySubledgerId;
            case INTEREST  -> interestSubledgerId;
            case REFUND    -> refundSubledgerId;
        };
    }
}
