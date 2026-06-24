package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.valueobject.AccountCategory;
import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.util.UUID;

/**
 * Ledger-engine integration. TIN-only interface (Rule 3) — no party_id passes
 * through here. The 4 subledgers per (TIN × tax type) are addressed by the
 * AccountCategory parameter; ledger-engine resolves the actual subledger row
 * internally.
 *
 * Methods:
 *   - postAssessment: initial principal post (BUC-FIL-013 → outbox dispatcher).
 *   - postAdjustment: amendment delta to PRINCIPAL referencing the original
 *     entry (BUC-FIL-033, Rule 8).
 *   - postPenalty / postInterest: late-filing penalty + interest postings to
 *     PENALTY / INTEREST. Late-filing penalty posts to PENALTY (same subledger
 *     as late-payment penalty).
 *
 * All payment-side calculations (penalty/interest accrual on overdue principal)
 * live in payment-service. Filing only posts the late-filing penalty when the
 * return itself is late.
 */
public interface LedgerEnginePort {

    LedgerEntryReference postAssessment(String tin, TaxTypeCode taxType, Period period,
                                        Money amount, AccountCategory category);

    LedgerEntryReference postAdjustment(String tin, TaxTypeCode taxType, Period period,
                                        Money delta, AccountCategory category, UUID originalEntryId);

    LedgerEntryReference postPenalty(String tin, TaxTypeCode taxType, Period period, Money amount);

    LedgerEntryReference postInterest(String tin, TaxTypeCode taxType, Period period, Money amount);
}
