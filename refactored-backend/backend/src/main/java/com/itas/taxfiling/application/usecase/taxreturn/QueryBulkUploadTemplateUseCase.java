package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntryFieldType;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BUC-FIL-004 — generates a CSV template for the given entry type.
 *
 * <p>Headers use the human-readable {@link EntryFieldDefinition#label()} so
 * a taxpayer can open the file in Excel and fill in rows without learning
 * the internal JSON keys. Column order mirrors the wizard table:
 * non-numeric entry-type fields → universal amount column → numeric
 * entry-type fields. The upload parser ({@link BulkUploadLineItemsUseCase})
 * translates labels back to keys.
 */
@Service
@RequiredArgsConstructor
public class QueryBulkUploadTemplateUseCase {

    private final LineItemEntryTypeRepositoryPort entryTypes;

    @Transactional(readOnly = true)
    public byte[] execute(UUID entryTypeId) {
        LineItemEntryType type = entryTypes.findById(entryTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("entry type not found: " + entryTypeId));

        List<String> beforeAmount = new ArrayList<>();
        List<String> afterAmount = new ArrayList<>();
        for (EntryFieldDefinition f : type.getFields()) {
            if (isNumeric(f.type())) afterAmount.add(f.label());
            else beforeAmount.add(f.label());
        }

        List<String> headers = new ArrayList<>(beforeAmount.size() + 1 + afterAmount.size());
        headers.addAll(beforeAmount);
        headers.add(amountLabel(type.getScheduleKind()));
        headers.addAll(afterAmount);

        try (StringWriter writer = new StringWriter();
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            printer.printRecord(headers);
            printer.flush();
            return writer.toString().getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("failed to write CSV template: " + e.getMessage(), e);
        }
    }

    /**
     * The user-facing label of the universal-spine amount column. Invoice-style
     * schedules (SALES / PURCHASES) collect the net (pre-VAT) figure here and
     * the wizard / template name it accordingly; everything else gets a plain
     * "Amount (ETB)" header. Must stay in sync with the upload parser.
     */
    static String amountLabel(ScheduleKind kind) {
        return (kind == ScheduleKind.SALES || kind == ScheduleKind.PURCHASES)
            ? "Amount before VAT (ETB)"
            : "Amount (ETB)";
    }

    private static boolean isNumeric(EntryFieldType type) {
        return type == EntryFieldType.INTEGER || type == EntryFieldType.DECIMAL;
    }
}
