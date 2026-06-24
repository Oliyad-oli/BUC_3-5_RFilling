package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.QueryBulkUploadTemplateUseCase;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntryFieldType;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryBulkUploadTemplateUseCaseTest {

    @Mock LineItemEntryTypeRepositoryPort entryTypes;
    @InjectMocks QueryBulkUploadTemplateUseCase useCase;

    @Test
    void sales_template_emits_labels_in_table_order_with_amount_before_vat() {
        LineItemEntryType type = LineItemEntryType.register(
            "VAT_SALES_INVOICE", new TaxTypeCode("VAT"), ScheduleKind.SALES, 1,
            List.of(
                new EntryFieldDefinition("customer_name", "Customer name",
                    EntryFieldType.STRING, true, List.of(), null),
                new EntryFieldDefinition("invoice_number", "Invoice #",
                    EntryFieldType.STRING, true, List.of(), null),
                new EntryFieldDefinition("invoice_date", "Invoice date",
                    EntryFieldType.DATE, true, List.of(), null),
                new EntryFieldDefinition("vat_amount", "VAT (ETB)",
                    EntryFieldType.DECIMAL, true, List.of(), null),
                new EntryFieldDefinition("total_amount", "Total incl. VAT (ETB)",
                    EntryFieldType.DECIMAL, true, List.of(), null)
            ),
            "admin");
        when(entryTypes.findById(type.getId())).thenReturn(Optional.of(type));

        String csv = new String(useCase.execute(type.getId()), StandardCharsets.UTF_8).trim();

        // Order: non-numeric labels → amount column → numeric labels.
        assertThat(csv).isEqualTo(
            "Customer name,Invoice #,Invoice date,Amount before VAT (ETB),VAT (ETB),Total incl. VAT (ETB)");
    }

    @Test
    void non_invoice_schedule_uses_plain_amount_label() {
        LineItemEntryType type = LineItemEntryType.register(
            "WHT_PAYROLL", new TaxTypeCode("WHT"), ScheduleKind.WITHHOLDING, 1,
            List.of(
                new EntryFieldDefinition("employee_name", "Employee name",
                    EntryFieldType.STRING, true, List.of(), null),
                new EntryFieldDefinition("gross_salary", "Gross salary (ETB)",
                    EntryFieldType.DECIMAL, true, List.of(), null)
            ),
            "admin");
        when(entryTypes.findById(type.getId())).thenReturn(Optional.of(type));

        String csv = new String(useCase.execute(type.getId()), StandardCharsets.UTF_8).trim();

        assertThat(csv).isEqualTo(
            "Employee name,Amount (ETB),Gross salary (ETB)");
    }
}
