package com.itas.taxfiling.engineadapter.einvoice;

import com.itas.taxfiling.application.port.EInvoiceServicePort;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.engineadapter.shared.BaseEngineAdapter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * [MOCK] e-invoice-service adapter. Replace by adding the e-invoice client library.
 *
 * <p>Each call returns three freshly-generated Ethiopian B2B invoices —
 * unique externalInvoiceId derived from the counterparty's own numbering
 * convention so dedup never collides on replay, issue dates picked
 * randomly within the period, amounts varied across the realistic
 * small / medium / large bands, VAT at 15%, totals reconciled to net + VAT.
 *
 * <p>SALES schedules pull invoices the taxpayer issued — counterparty role
 * is "customer"; PURCHASES schedules pull invoices the taxpayer received —
 * counterparty role is "supplier". The mock spreads entry-type-shaped
 * keys directly into the line item's entryData so the use case needs no
 * per-key translation.
 */
@Slf4j
@Component
public class EInvoiceServiceMockAdapter extends BaseEngineAdapter implements EInvoiceServicePort {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.15");

    /**
     * Realistic Ethiopian counterparties — each carries its own TIN and the
     * invoice-numbering convention it actually uses on hard-copy invoices.
     * Mix of industries + scale bands so generated invoices cover the
     * small / medium / large amount ranges.
     */
    private record Counterparty(
        String name,
        String tin,
        String invoicePrefix,
        AmountBand band
    ) {}

    private enum AmountBand {
        SMALL(2_500L, 35_000L),       // office supplies, stationery, small services
        MEDIUM(35_000L, 450_000L),    // IT services, professional fees, catering
        LARGE(450_000L, 8_500_000L);  // construction, equipment, enterprise contracts

        final long min;
        final long max;
        AmountBand(long min, long max) { this.min = min; this.max = max; }
    }

    private static final List<Counterparty> CUSTOMERS = List.of(
        // Banks
        new Counterparty("Commercial Bank of Ethiopia", "0011000123", "CBE/INV", AmountBand.LARGE),
        new Counterparty("Awash Bank S.C.",             "0011223344", "AB-",     AmountBand.MEDIUM),
        new Counterparty("Dashen Bank S.C.",            "0023456789", "DSB/FS/", AmountBand.MEDIUM),
        new Counterparty("Bank of Abyssinia",           "0011334455", "BOA-",    AmountBand.LARGE),
        // Telecom
        new Counterparty("Ethio Telecom",               "0019988776", "ET/INV/", AmountBand.LARGE),
        new Counterparty("Safaricom Ethiopia",          "0024455667", "SFM-",    AmountBand.MEDIUM),
        // Hotels
        new Counterparty("Sheraton Addis",              "0017766554", "SHR-",    AmountBand.MEDIUM),
        new Counterparty("Hilton Addis Ababa",          "0014477889", "HIL-",    AmountBand.MEDIUM),
        new Counterparty("Hyatt Regency Addis",         "0028899001", "HYT-",    AmountBand.MEDIUM),
        new Counterparty("Skylight Hotel",              "0025566778", "SKY-",    AmountBand.SMALL),
        // Airlines
        new Counterparty("Ethiopian Airlines",          "0011112233", "ET-FS-",  AmountBand.LARGE),
        // Insurance
        new Counterparty("Awash Insurance",             "0013344556", "AIC-",    AmountBand.MEDIUM),
        new Counterparty("Nile Insurance",              "0026677889", "NIC-",    AmountBand.SMALL),
        // Manufacturing / Beverage
        new Counterparty("BGI Ethiopia",                "0015544332", "BGI-",    AmountBand.LARGE),
        new Counterparty("Heineken Ethiopia",           "0029900112", "HNK/",    AmountBand.LARGE),
        new Counterparty("MOHA Soft Drinks Industry",   "0018877665", "MOHA-",   AmountBand.MEDIUM),
        // Government
        new Counterparty("Ministry of Finance",         "0010001000", "MOF/PO/", AmountBand.LARGE),
        new Counterparty("Addis Ababa City Admin.",     "0010002000", "AACA-",   AmountBand.MEDIUM)
    );

    private static final List<Counterparty> SUPPLIERS = List.of(
        // Office supplies / stationery
        new Counterparty("Africom Office Supplies",     "0034455667", "AOS-",    AmountBand.SMALL),
        new Counterparty("Habtamu Stationery PLC",      "0034566778", "HST-",    AmountBand.SMALL),
        new Counterparty("Walia Equipment Imports",     "0034677889", "WEI-",    AmountBand.MEDIUM),
        // IT / Software
        new Counterparty("Sunshine IT Solutions",       "0035788990", "SIT/INV", AmountBand.MEDIUM),
        new Counterparty("Custor Computing PLC",        "0035899001", "CC-",     AmountBand.MEDIUM),
        new Counterparty("Information Network Sec. Ag.","0035900102", "INSA-",   AmountBand.LARGE),
        // Logistics / freight
        new Counterparty("Bole Logistics PLC",          "0036011223", "BLS-",    AmountBand.MEDIUM),
        new Counterparty("Trans Ethiopia Freight",      "0036122334", "TEF/",    AmountBand.MEDIUM),
        new Counterparty("Mesfin Industrial Eng.",      "0036233445", "MIE-",    AmountBand.LARGE),
        // Construction
        new Counterparty("Sunshine Construction PLC",   "0037344556", "SNC-",    AmountBand.LARGE),
        new Counterparty("MIDROC Ethiopia Construction","0037455667", "MID/",    AmountBand.LARGE),
        new Counterparty("Yotek Construction",          "0037566778", "YTK-",    AmountBand.LARGE),
        // Energy / fuel
        new Counterparty("Ethiopian Electric Power",    "0038677889", "EEP/INV", AmountBand.LARGE),
        new Counterparty("NOC Ethiopia",                "0038788990", "NOC-",    AmountBand.MEDIUM),
        // Imports / trading
        new Counterparty("Nile Trading PLC",            "0039899001", "NTR-",    AmountBand.MEDIUM),
        new Counterparty("Blue Nile Imports",           "0039900112", "BNI-",    AmountBand.MEDIUM),
        new Counterparty("Highland Coffee Traders",     "0039011223", "HCT-",    AmountBand.SMALL),
        // Food & beverage
        new Counterparty("Heineken Ethiopia",           "0029900112", "HNK/",    AmountBand.MEDIUM)
    );

    public EInvoiceServiceMockAdapter() { super("e-invoice-service"); }

    @Override
    @CircuitBreaker(name = "e-invoice-service", fallbackMethod = "pullForTaxpayerFallback")
    @Retry(name = "e-invoice-service")
    public List<EInvoiceLine> pullForTaxpayer(String tin, Period period, ScheduleKind kind) {
        log.info("[MOCK] e-invoice pullForTaxpayer tin={} period={} kind={}",
            tin, period.label(), kind);
        List<EInvoiceLine> out = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            out.add(generateInvoice(period, kind));
        }
        return out;
    }

    private EInvoiceLine generateInvoice(Period period, ScheduleKind kind) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        boolean sales = kind == ScheduleKind.SALES;

        List<Counterparty> pool = sales ? CUSTOMERS : SUPPLIERS;
        Counterparty cp = pool.get(rnd.nextInt(pool.size()));

        // Per-counterparty invoice number, e.g. "CBE/INV-2026/05/0006142" —
        // unique enough to never collide across pulls. The trailing fragment
        // pulls 4 + 4 digits from a UUID to give us 10^8 distinct numbers
        // per counterparty per period.
        String uuidFrag = UUID.randomUUID().toString().replace("-", "");
        String sequence = uuidFrag.substring(0, 4).toUpperCase()
            + uuidFrag.substring(4, 8).toUpperCase();
        String periodSeg = period.label().replace("-", "/");
        String invoiceNumber = cp.invoicePrefix() + periodSeg + "/" + sequence;

        LocalDate issueDate = randomDateInPeriod(period, rnd);

        BigDecimal net = BigDecimal.valueOf(
                cp.band().min + (long) (rnd.nextDouble() * (cp.band().max - cp.band().min)))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal vat = net.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = net.add(vat).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put(sales ? "customer_name" : "supplier_name", cp.name());
        extra.put(sales ? "customer_tin" : "supplier_tin", cp.tin());
        extra.put("invoice_number", invoiceNumber);
        extra.put("invoice_date", issueDate.toString());
        extra.put("vat_amount", vat.toPlainString());
        extra.put("total_amount", total.toPlainString());

        return new EInvoiceLine(invoiceNumber, issueDate,
            Money.of(net.toPlainString(), "ETB"),
            Money.of(vat.toPlainString(), "ETB"),
            cp.tin(), extra);
    }

    private LocalDate randomDateInPeriod(Period period, ThreadLocalRandom rnd) {
        long span = ChronoUnit.DAYS.between(period.start(), period.end());
        long offset = span <= 0 ? 0 : rnd.nextLong(span + 1);
        return period.start().plusDays(offset);
    }

    @SuppressWarnings("unused")
    private List<EInvoiceLine> pullForTaxpayerFallback(String tin, Period period,
                                                      ScheduleKind kind, Exception ex) {
        throw wrapException("pullForTaxpayer", ex);
    }
}
