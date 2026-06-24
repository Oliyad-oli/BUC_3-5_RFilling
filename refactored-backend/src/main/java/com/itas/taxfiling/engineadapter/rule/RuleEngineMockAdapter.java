package com.itas.taxfiling.engineadapter.rule;

import com.itas.taxfiling.application.port.RuleEnginePort;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.CalculationOutcome;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.QuestionnaireAnswers;
import com.itas.taxfiling.domain.valueobject.RuleOutcome;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.engineadapter.shared.BaseEngineAdapter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** [MOCK] rule-engine adapter. Replace by adding the rule-engine client library. */
@Slf4j
@Component
public class RuleEngineMockAdapter extends BaseEngineAdapter implements RuleEnginePort {

    public RuleEngineMockAdapter() { super("rule-engine"); }

    @Override
    @CircuitBreaker(name = "rule-engine", fallbackMethod = "calculateFallback")
    @Retry(name = "rule-engine")
    public CalculationOutcome calculate(TaxReturn taxReturn, QuestionnaireAnswers answers,
                                        RulePackageVersion rulePackage) {
        // [MOCK] Implements Ethiopian tax math per Proclamation 1395/2025
        // (effective 1 July 2025) for the periodic-return scenarios the
        // taxpayer portal supports today: VAT, PAYE, domestic WHT, Excise,
        // and Annual CIT + MAT.
        String taxType = taxReturn.getTaxType().value();
        BigDecimal output = BigDecimal.ZERO;
        BigDecimal input  = BigDecimal.ZERO;

        for (var sch : taxReturn.getSchedules()) {
            for (var li : sch.getLineItems()) {
                var data = li.getEntryData() == null ? java.util.Map.<String,Object>of()
                    : li.getEntryData().values();

                // Canonical codes only — tax_type_catalog is the source of truth.
                // Old short aliases (WHT/CIT/EXCISE) are retired; if you see one
                // in payload, something upstream didn't migrate.
                switch (taxType) {
                    case "VAT": {
                        // Prefer explicit vatAmount (e-invoice path carries it).
                        // For manual entries, derive at the standard rate from
                        // the line's base amount: 15% (Proclamation 1395/2025).
                        BigDecimal vat = toBigDecimal(data.get("vatAmount"));
                        if (vat.signum() == 0 && li.getAmount() != null) {
                            vat = li.getAmount().amount().multiply(new BigDecimal("0.15"));
                        }
                        var kind = sch.getKind();
                        if (kind == com.itas.taxfiling.domain.valueobject.ScheduleKind.SALES) {
                            output = output.add(vat);
                        } else if (kind == com.itas.taxfiling.domain.valueobject.ScheduleKind.PURCHASES) {
                            // Includes both local-purchase VAT and customs-paid
                            // import VAT — both are input-creditable.
                            input = input.add(vat);
                        }
                        break;
                    }
                    case "PAYE": {
                        // Schedule A — employer remits monthly PAYE on payroll.
                        // Bracket math per Proclamation 1395/2025 (0/15/20/25/30/35,
                        // tax-free threshold 2,000 birr).
                        BigDecimal grossSalary = toBigDecimal(data.get("grossSalary"));
                        if (grossSalary.signum() > 0) {
                            output = output.add(payeFromGross(grossSalary));
                        }
                        break;
                    }
                    case "WITHHOLDING_TAX": {
                        // Domestic supplier WHT — 3% with TIN, 30% final-tax
                        // without; service/goods thresholds apply per 1395/2025.
                        BigDecimal grossPaid = toBigDecimal(data.get("grossPaid"));
                        if (grossPaid.signum() > 0) {
                            String paymentType = String.valueOf(data.getOrDefault("paymentType", "DOMESTIC_GOODS"));
                            boolean hasTin = !"false".equalsIgnoreCase(String.valueOf(data.getOrDefault("hasTin", "true")));
                            output = output.add(whtFromPayment(grossPaid, paymentType, hasTin));
                        }
                        break;
                    }
                    case "EXCISE_TAX": {
                        BigDecimal base = toBigDecimal(data.get("netAmount"));
                        BigDecimal rate = toBigDecimal(data.get("exciseRate"));
                        // exciseRate stored as a fraction (0.60 for 60%).
                        output = output.add(base.multiply(rate));
                        break;
                    }
                    case "INCOME_TAX_BUSINESS": {
                        // Annual CIT — single CIT_ANNUAL_SUMMARY entry carries
                        // turnover + expenses + prepayments. Higher of CIT vs MAT.
                        BigDecimal revenue        = toBigDecimal(data.get("grossRevenue"));
                        BigDecimal expenses       = toBigDecimal(data.get("deductibleExpenses"));
                        BigDecimal advanceCustoms = toBigDecimal(data.get("advancePaidAtCustoms"));
                        BigDecimal whtCredits     = toBigDecimal(data.get("whtCreditsReceived"));
                        BigDecimal quarterlies    = toBigDecimal(data.get("quarterlyInstalments"));

                        BigDecimal profit = revenue.subtract(expenses).max(BigDecimal.ZERO);
                        BigDecimal cit    = profit.multiply(new BigDecimal("0.30"));
                        BigDecimal mat    = revenue.multiply(new BigDecimal("0.025"));
                        BigDecimal liability = cit.max(mat);

                        output  = output.add(liability);
                        input   = input.add(advanceCustoms.add(whtCredits).add(quarterlies));
                        break;
                    }
                }
            }
        }
        Money gross   = new Money(output, "ETB");
        Money credits = new Money(input,  "ETB");
        Money net     = gross.subtract(credits);
        log.info("[MOCK] rule-engine calculate taxReturn={} taxType={} output={} credits={} net={}",
            taxReturn.getId(), taxType, output, input, net.amount());
        return new CalculationOutcome(gross, credits, net, List.of(), rulePackage);
    }

    private static BigDecimal toBigDecimal(Object raw) {
        if (raw == null) return BigDecimal.ZERO;
        if (raw instanceof BigDecimal bd) return bd;
        if (raw instanceof Number n)      return new BigDecimal(n.toString());
        try { return new BigDecimal(raw.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    // ─── Ethiopian PAYE bracket schedule (Proclamation 1395/2025) ──────────
    // Cumulative-deduction form: tax = gross × marginalRate − cumulativeDeduction.
    //   0 –  2,000 → 0%      (tax-free threshold raised from 600 in 1395/2025)
    //   2,001 – 4,000 → 15%  (lowest bracket bumped from 10% in 1395/2025)
    //   4,001 – 7,000 → 20%
    //   7,001 – 10,000 → 25%
    //  10,001 – 14,000 → 30%
    //  14,001+ → 35%         (top bracket entry-point shifted from 10,900)
    // Cumulative deductions are derived so the progressive tax is continuous
    // across boundaries.
    private static final BigDecimal[][] PAYE_BRACKETS = {
        { new BigDecimal("2000"),  new BigDecimal("0.00"), new BigDecimal("0")    },
        { new BigDecimal("4000"),  new BigDecimal("0.15"), new BigDecimal("300")  },
        { new BigDecimal("7000"),  new BigDecimal("0.20"), new BigDecimal("500")  },
        { new BigDecimal("10000"), new BigDecimal("0.25"), new BigDecimal("850")  },
        { new BigDecimal("14000"), new BigDecimal("0.30"), new BigDecimal("1350") },
        { null,                    new BigDecimal("0.35"), new BigDecimal("2050") }
    };

    private static BigDecimal payeFromGross(BigDecimal gross) {
        for (BigDecimal[] b : PAYE_BRACKETS) {
            BigDecimal upper = b[0];
            if (upper == null || gross.compareTo(upper) <= 0) {
                BigDecimal tax = gross.multiply(b[1]).subtract(b[2]);
                return tax.signum() < 0 ? BigDecimal.ZERO : tax;
            }
        }
        return BigDecimal.ZERO;
    }

    // ─── Domestic WHT (Proclamation 1395/2025) ─────────────────────────────
    // 3% with TIN; 30% final tax for no-TIN suppliers; threshold rules apply
    // (services > 3,000; goods > 10,000). Special types use their own rate.
    private static BigDecimal whtFromPayment(BigDecimal gross, String paymentType, boolean hasTin) {
        switch (paymentType) {
            case "DIVIDEND":
                return gross.multiply(new BigDecimal("0.15"));
            case "ROYALTY":
                return gross.multiply(new BigDecimal("0.05"));
            case "INTEREST":
                return gross.multiply(new BigDecimal("0.10"));
            case "DOMESTIC_SERVICE":
                if (!hasTin) return gross.multiply(new BigDecimal("0.30"));
                if (gross.compareTo(new BigDecimal("3000")) <= 0) return BigDecimal.ZERO;
                return gross.multiply(new BigDecimal("0.03"));
            case "DOMESTIC_GOODS":
            default:
                if (!hasTin) return gross.multiply(new BigDecimal("0.30"));
                if (gross.compareTo(new BigDecimal("10000")) <= 0) return BigDecimal.ZERO;
                return gross.multiply(new BigDecimal("0.03"));
        }
    }

    private CalculationOutcome calculateFallback(TaxReturn taxReturn, QuestionnaireAnswers answers,
                                                 RulePackageVersion rulePackage, Exception ex) {
        throw wrapException("calculate", ex);
    }

    @Override
    @CircuitBreaker(name = "rule-engine", fallbackMethod = "postLedgerCheckFallback")
    @Retry(name = "rule-engine")
    public RuleOutcome postLedgerCheck(TaxReturn taxReturn, RulePackageVersion rulePackage) {
        log.info("[MOCK] rule-engine postLedgerCheck taxReturn={} package={}", taxReturn.getId(), rulePackage);
        return new RuleOutcome(true, List.of());
    }

    private RuleOutcome postLedgerCheckFallback(TaxReturn taxReturn, RulePackageVersion rulePackage, Exception ex) {
        throw wrapException("postLedgerCheck", ex);
    }
}
