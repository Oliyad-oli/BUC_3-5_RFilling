package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.BulkUploadLineItemsUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.BulkUploadLineItemsUseCase.BulkUploadResult;
import com.itas.taxfiling.application.usecase.taxreturn.BulkUploadLineItemsUseCase.Format;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntryFieldType;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.unit.test_support.TaxReturnTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkUploadLineItemsUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock LineItemEntryTypeRepositoryPort entryTypes;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks BulkUploadLineItemsUseCase useCase;

    @Test
    void parses_csv_and_adds_line_items() {
        TaxReturn t = TaxReturnTestBuilder.newDraft();
        Schedule s = t.addSchedule(ScheduleKind.SALES, "Sales");
        t.pullEvents();
        LineItemEntryType type = LineItemEntryType.register(
            "VAT_SALES", new TaxTypeCode("VAT"), ScheduleKind.SALES, 1,
            List.of(new EntryFieldDefinition("invoiceNumber", "Invoice",
                EntryFieldType.STRING, false, List.of(), null)),
            "admin");

        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        when(entryTypes.findById(type.getId())).thenReturn(Optional.of(type));
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        String csv = "amount,currency,source,invoiceNumber\n"
            + "100.00,ETB,BULK_UPLOAD,INV-1\n"
            + "250.50,ETB,BULK_UPLOAD,INV-2\n";

        BulkUploadResult result = useCase.execute(t.getId(), s.getId(), type.getId(),
            Format.CSV, csv.getBytes(StandardCharsets.UTF_8));

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isZero();
        assertThat(t.getSchedules().get(0).getLineItems()).hasSize(2);
    }

    @Test
    void accepts_template_with_human_labels_and_table_order() {
        // Mirrors the template output: non-numeric fields first, then the
        // universal amount column ("Amount before VAT (ETB)" for SALES),
        // then numeric fields. Headers are labels, not raw keys.
        TaxReturn t = TaxReturnTestBuilder.newDraft();
        Schedule s = t.addSchedule(ScheduleKind.SALES, "Sales");
        t.pullEvents();
        LineItemEntryType type = LineItemEntryType.register(
            "VAT_SALES_INVOICE", new TaxTypeCode("VAT"), ScheduleKind.SALES, 1,
            List.of(
                new EntryFieldDefinition("customer_name", "Customer name",
                    EntryFieldType.STRING, true, List.of(), null),
                new EntryFieldDefinition("invoice_number", "Invoice #",
                    EntryFieldType.STRING, true, List.of(), null),
                new EntryFieldDefinition("vat_amount", "VAT (ETB)",
                    EntryFieldType.DECIMAL, true, List.of(), null),
                new EntryFieldDefinition("total_amount", "Total incl. VAT (ETB)",
                    EntryFieldType.DECIMAL, true, List.of(), null)
            ),
            "admin");

        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        when(entryTypes.findById(type.getId())).thenReturn(Optional.of(type));
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        String csv = "Customer name,Invoice #,Amount before VAT (ETB),VAT (ETB),Total incl. VAT (ETB)\n"
            + "Hilton,INV-1,100000.00,15000.00,115000.00\n"
            + "Ethio Bank,INV-2,50000.00,7500.00,57500.00\n";

        BulkUploadResult result = useCase.execute(t.getId(), s.getId(), type.getId(),
            Format.CSV, csv.getBytes(StandardCharsets.UTF_8));

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isZero();
        // Confirm the label-to-key translation populated entryData correctly.
        var firstItem = t.getSchedules().get(0).getLineItems().get(0);
        assertThat(firstItem.getAmount().amount()).isEqualByComparingTo("100000.00");
        assertThat(firstItem.getEntryData().get("customer_name")).isEqualTo("Hilton");
        assertThat(firstItem.getEntryData().get("invoice_number")).isEqualTo("INV-1");
        assertThat(firstItem.getEntryData().get("vat_amount")).isEqualTo("15000.00");
        assertThat(firstItem.getEntryData().get("total_amount")).isEqualTo("115000.00");
    }

    @Test
    void per_row_errors_dont_abort_the_batch() {
        TaxReturn t = TaxReturnTestBuilder.newDraft();
        Schedule s = t.addSchedule(ScheduleKind.SALES, "Sales");
        t.pullEvents();
        LineItemEntryType type = LineItemEntryType.register(
            "VAT_SALES", new TaxTypeCode("VAT"), ScheduleKind.SALES, 1,
            List.of(new EntryFieldDefinition("invoiceNumber", "Invoice",
                EntryFieldType.STRING, false, List.of(), null)),
            "admin");

        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        when(entryTypes.findById(type.getId())).thenReturn(Optional.of(type));
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        String csv = "amount,currency,source\n"
            + "100.00,ETB,BULK_UPLOAD\n"
            + "not-a-number,ETB,BULK_UPLOAD\n"
            + "200.00,ETB,BULK_UPLOAD\n";

        BulkUploadResult result = useCase.execute(t.getId(), s.getId(), type.getId(),
            Format.CSV, csv.getBytes(StandardCharsets.UTF_8));

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.rows()).extracting(BulkUploadLineItemsUseCase.RowOutcome::success)
            .containsExactly(true, false, true);
    }
}
