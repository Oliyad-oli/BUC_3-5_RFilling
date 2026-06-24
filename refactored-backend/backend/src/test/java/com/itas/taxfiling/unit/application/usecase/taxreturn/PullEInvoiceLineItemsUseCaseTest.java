package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EInvoiceServicePort;
import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.PullEInvoiceLineItemsUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.PullEInvoiceLineItemsUseCase.PullOutcome;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.FilingMethod;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.domain.valueobject.TaxpayerReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullEInvoiceLineItemsUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock LineItemEntryTypeRepositoryPort entryTypes;
    @Mock EInvoiceServicePort eInvoice;
    @Mock EventPublisherPort eventPublisher;

    @InjectMocks PullEInvoiceLineItemsUseCase useCase;

    private TaxReturn taxReturn;
    private Schedule schedule;
    private LineItemEntryType entryType;

    @BeforeEach
    void setUp() {
        taxReturn = TaxReturn.draft(
            new TaxpayerReference("1234567", "party-1"),
            new TaxTypeCode("VAT"),
            new Period(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), PeriodFrequency.MONTHLY),
            FilingMethod.PORTAL,
            new RulePackageVersion("VAT", "1.0.0"));
        schedule = taxReturn.addSchedule(ScheduleKind.SALES, "VAT Sales");
        entryType = LineItemEntryType.register(
            "VAT_SALES_STANDARD", new TaxTypeCode("VAT"), ScheduleKind.SALES, 1,
            List.of(), "admin-1");
    }

    @Test
    void appends_all_lines_when_schedule_is_empty() {
        when(taxReturns.findById(taxReturn.getId())).thenReturn(Optional.of(taxReturn));
        when(entryTypes.findById(entryType.getId())).thenReturn(Optional.of(entryType));
        when(taxReturns.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eInvoice.pullForTaxpayer(eq("1234567"), any(), eq(ScheduleKind.SALES)))
            .thenReturn(List.of(
                line("EI-001", LocalDate.of(2026, 4, 5), "250000.00", "37500.00"),
                line("EI-002", LocalDate.of(2026, 4, 12), "180000.00", "27000.00")));

        PullOutcome outcome = useCase.execute(
            taxReturn.getId(), schedule.getId(), entryType.getId());

        assertThat(outcome.added()).isEqualTo(2);
        assertThat(outcome.skipped()).isEqualTo(0);
        assertThat(taxReturn.getSchedules().get(0).getLineItems()).hasSize(2);
    }

    @Test
    void skips_duplicates_already_on_schedule() {
        // Pre-populate the schedule with EI-001 to simulate a prior pull.
        taxReturn.addLineItem(schedule.getId(), entryType.getId(), entryType.getVersion(),
            Money.of("250000.00", "ETB"), com.itas.taxfiling.domain.valueobject.LineItemSource.E_INVOICE,
            new com.itas.taxfiling.domain.valueobject.EntrySpecificData(
                Map.of("externalInvoiceId", "EI-001")));

        when(taxReturns.findById(taxReturn.getId())).thenReturn(Optional.of(taxReturn));
        when(entryTypes.findById(entryType.getId())).thenReturn(Optional.of(entryType));
        when(taxReturns.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eInvoice.pullForTaxpayer(eq("1234567"), any(), eq(ScheduleKind.SALES)))
            .thenReturn(List.of(
                line("EI-001", LocalDate.of(2026, 4, 5), "250000.00", "37500.00"), // dup
                line("EI-002", LocalDate.of(2026, 4, 12), "180000.00", "27000.00"))); // new

        PullOutcome outcome = useCase.execute(
            taxReturn.getId(), schedule.getId(), entryType.getId());

        assertThat(outcome.added()).isEqualTo(1);
        assertThat(outcome.skipped()).isEqualTo(1);
        assertThat(taxReturn.getSchedules().get(0).getLineItems()).hasSize(2);
    }

    @Test
    void second_call_with_same_payload_is_idempotent() {
        when(taxReturns.findById(taxReturn.getId())).thenReturn(Optional.of(taxReturn));
        when(entryTypes.findById(entryType.getId())).thenReturn(Optional.of(entryType));
        when(taxReturns.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eInvoice.pullForTaxpayer(eq("1234567"), any(), eq(ScheduleKind.SALES)))
            .thenReturn(List.of(
                line("EI-001", LocalDate.of(2026, 4, 5), "250000.00", "37500.00"),
                line("EI-002", LocalDate.of(2026, 4, 12), "180000.00", "27000.00")));

        PullOutcome first = useCase.execute(
            taxReturn.getId(), schedule.getId(), entryType.getId());
        PullOutcome second = useCase.execute(
            taxReturn.getId(), schedule.getId(), entryType.getId());

        assertThat(first.added()).isEqualTo(2);
        assertThat(second.added()).isEqualTo(0);
        assertThat(second.skipped()).isEqualTo(2);
        assertThat(taxReturn.getSchedules().get(0).getLineItems()).hasSize(2);
    }

    @Test
    void passes_schedule_kind_to_e_invoice_port() {
        when(taxReturns.findById(taxReturn.getId())).thenReturn(Optional.of(taxReturn));
        when(entryTypes.findById(entryType.getId())).thenReturn(Optional.of(entryType));
        when(taxReturns.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eInvoice.pullForTaxpayer(eq("1234567"), any(), eq(ScheduleKind.SALES)))
            .thenReturn(List.of());

        PullOutcome outcome = useCase.execute(
            taxReturn.getId(), schedule.getId(), entryType.getId());

        assertThat(outcome.added()).isZero();
        verify(eInvoice).pullForTaxpayer(eq("1234567"), any(), eq(ScheduleKind.SALES));
    }

    @Test
    void rejects_non_sales_purchases_entry_type() {
        LineItemEntryType bad = LineItemEntryType.register(
            "INC_DEDUCTION", new TaxTypeCode("INCOME_TAX"), ScheduleKind.OTHER, 1,
            List.of(), "admin-1");
        when(taxReturns.findById(taxReturn.getId())).thenReturn(Optional.of(taxReturn));
        when(entryTypes.findById(bad.getId())).thenReturn(Optional.of(bad));

        assertThatThrownBy(() -> useCase.execute(
                taxReturn.getId(), schedule.getId(), bad.getId()))
            .isInstanceOf(DomainException.class);
        verify(taxReturns, never()).save(any());
    }

    private EInvoiceServicePort.EInvoiceLine line(String externalId, LocalDate issueDate,
                                                  String amount, String tax) {
        return new EInvoiceServicePort.EInvoiceLine(
            externalId, issueDate,
            Money.of(amount, "ETB"), Money.of(tax, "ETB"),
            "9999999", Map.of("customer_name", "Test Co"));
    }
}
