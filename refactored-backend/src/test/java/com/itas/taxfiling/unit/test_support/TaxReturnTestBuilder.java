package com.itas.taxfiling.unit.test_support;

import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.AccountCategory;
import com.itas.taxfiling.domain.valueobject.CalculationOutcome;
import com.itas.taxfiling.domain.valueobject.FilingMethod;
import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.QuestionnaireAnswers;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.domain.valueobject.TaxpayerReference;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Test factory for TaxReturn fixtures. Keeps test code terse. */
public final class TaxReturnTestBuilder {

    private TaxReturnTestBuilder() {}

    public static TaxReturn newDraft() {
        return TaxReturn.draft(
            new TaxpayerReference("1234567", "party-1"),
            new TaxTypeCode("VAT"),
            Period.monthly(YearMonth.of(2026, 4)),
            FilingMethod.PORTAL,
            new RulePackageVersion("VAT", "1.0.0"));
    }

    /** Returns an ACCEPTED tax return (calc done + accepted; not yet ledger-posted). */
    public static TaxReturn accepted() {
        TaxReturn t = newDraft();
        CalculationIteration iter = t.requestCalculation(QuestionnaireAnswers.empty());
        Money net = Money.of("100.00", "ETB");
        t.completeCalculation(iter.getId(),
            new CalculationOutcome(net, Money.zero("ETB"), net, List.of(),
                new RulePackageVersion("VAT", "1.0.0")));
        t.acceptCalculation(iter.getId());
        return t;
    }

    /** Returns a return that is POSTED_TO_LEDGER. */
    public static TaxReturn posted() {
        TaxReturn t = accepted();
        t.recordLedgerPosted(new LedgerEntryReference(
            UUID.randomUUID(), AccountCategory.PRINCIPAL, Instant.now()));
        return t;
    }
}
