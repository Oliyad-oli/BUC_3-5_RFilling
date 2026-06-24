package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntrySpecificData;
import com.itas.taxfiling.domain.valueobject.EntryTypeStatus;
import com.itas.taxfiling.domain.valueobject.LineItemSource;
import com.itas.taxfiling.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * BUC-FIL-004 — bulk upload of line items via CSV or XLSX. Reuses the
 * AddLineItem path per row but in a single transaction. Returns a per-row
 * outcome so the UI can show which rows succeeded vs failed.
 *
 * Required columns (case-sensitive header): amount, currency, source — plus
 * any entry-type-specific fields. Unknown columns are passed through into
 * EntrySpecificData; the per-line validation cascade (Levels 1-2) runs in
 * AddLineItemUseCase.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUploadLineItemsUseCase {

    private static final List<String> RESERVED_COLUMNS = List.of("amount", "currency", "source");

    private final TaxReturnRepositoryPort taxReturns;
    private final LineItemEntryTypeRepositoryPort entryTypes;
    private final EventPublisherPort eventPublisher;

    public enum Format { CSV, XLSX }

    public record BulkUploadResult(int successCount, int failureCount, List<RowOutcome> rows) {}
    public record RowOutcome(int rowNumber, boolean success, String message) {}

    @Transactional
    public BulkUploadResult execute(UUID taxReturnId, UUID scheduleId, UUID entryTypeId,
                                    Format format, byte[] payload) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        LineItemEntryType type = entryTypes.findById(entryTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("entry type not found: " + entryTypeId));
        if (type.getStatus() == EntryTypeStatus.RETIRED) {
            throw new DomainException("entry type retired: " + type.getCode());
        }

        List<Map<String, String>> rows = format == Format.CSV ? parseCsv(payload) : parseXlsx(payload);

        // Templates ship with human-readable headers ("Customer name", "VAT
        // (ETB)", "Amount before VAT (ETB)"). Translate those back to the
        // entry-type field keys + the canonical "amount" column so the rest
        // of this method works against canonical names. Files that already
        // use raw keys (legacy CSVs, system-generated dumps) pass through
        // unchanged because the lookup falls back to the raw header.
        Map<String, String> labelToKey = buildLabelToKeyMap(type);
        rows = rows.stream().map(r -> translateHeaders(r, labelToKey)).toList();

        // DATE fields: normalize anything spreadsheet-y to ISO yyyy-MM-dd so
        // the Level-1 validator (and the wizard's <input type="date">) can
        // read it back. Excel hands us "5/15/2026", "15-05-2026", or even
        // numeric serial days from XLSX cells; the regex check downstream
        // only accepts ISO.
        Set<String> dateKeys = dateFieldKeys(type);
        if (!dateKeys.isEmpty()) {
            rows = rows.stream().map(r -> normalizeDates(r, dateKeys)).toList();
        }

        List<RowOutcome> outcomes = new ArrayList<>();
        int success = 0;
        int failure = 0;
        int rowNum = 1;
        for (Map<String, String> row : rows) {
            try {
                Money amount = new Money(new BigDecimal(row.getOrDefault("amount", "0")),
                    row.getOrDefault("currency", "ETB"));
                LineItemSource source = LineItemSource.valueOf(
                    row.getOrDefault("source", LineItemSource.BULK_UPLOAD.name()));
                Map<String, Object> entryData = new LinkedHashMap<>();
                row.forEach((k, v) -> {
                    if (!RESERVED_COLUMNS.contains(k)) entryData.put(k, v);
                });
                t.addLineItem(scheduleId, type.getId(), type.getVersion(),
                    amount, source, new EntrySpecificData(entryData));
                outcomes.add(new RowOutcome(rowNum, true, null));
                success++;
            } catch (Exception ex) {
                outcomes.add(new RowOutcome(rowNum, false, ex.getMessage()));
                failure++;
            }
            rowNum++;
        }

        if (success > 0) {
            TaxReturn saved = taxReturns.save(t);
            saved.pullEvents().forEach(eventPublisher::publish);
        }
        log.info("Bulk upload taxReturnId={} success={} failure={}", taxReturnId, success, failure);
        return new BulkUploadResult(success, failure, outcomes);
    }

    /**
     * Build a label → canonical-key map for this entry type. Includes the
     * universal-spine amount column ("Amount before VAT (ETB)" / "Amount
     * (ETB)") so it routes to the reserved {@code amount} field. Source of
     * truth for the labels is {@link QueryBulkUploadTemplateUseCase} — they
     * must match exactly.
     */
    private Map<String, String> buildLabelToKeyMap(LineItemEntryType type) {
        Map<String, String> map = new HashMap<>();
        for (EntryFieldDefinition f : type.getFields()) {
            map.put(f.label(), f.key());
        }
        map.put(QueryBulkUploadTemplateUseCase.amountLabel(type.getScheduleKind()), "amount");
        return map;
    }

    /** Keys of every DATE-typed field on the entry type. */
    private Set<String> dateFieldKeys(LineItemEntryType type) {
        Set<String> keys = new HashSet<>();
        for (EntryFieldDefinition f : type.getFields()) {
            if (f.type() == com.itas.taxfiling.domain.valueobject.EntryFieldType.DATE) {
                keys.add(f.key());
            }
        }
        return keys;
    }

    /** Tolerant date formats Excel / locales commonly produce. ISO first so the
     *  hot path is fastest. */
    private static final List<DateTimeFormatter> DATE_INPUT_FORMATS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,                    // 2026-05-15
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("dd MMM yyyy"),
        DateTimeFormatter.ofPattern("d MMM yyyy"),
        DateTimeFormatter.ofPattern("d-MMM-yyyy")
    );

    private Map<String, String> normalizeDates(Map<String, String> row, Set<String> dateKeys) {
        Map<String, String> out = new LinkedHashMap<>(row);
        for (String key : dateKeys) {
            String raw = row.get(key);
            if (raw == null || raw.isBlank()) continue;
            String iso = toIsoDate(raw.trim());
            if (iso != null) out.put(key, iso);
        }
        return out;
    }

    /**
     * Best-effort coercion of a date string into ISO {@code yyyy-MM-dd}.
     * Returns {@code null} when nothing matches; caller leaves the raw value
     * in place so the validator can surface a clean per-row error.
     *
     * Handles:
     *   - ISO and the locale formats in {@link #DATE_INPUT_FORMATS}
     *   - Excel numeric serials (e.g. "46127" → days since 1899-12-30)
     */
    private String toIsoDate(String raw) {
        for (DateTimeFormatter fmt : DATE_INPUT_FORMATS) {
            try {
                return LocalDate.parse(raw, fmt).toString();
            } catch (DateTimeParseException ignore) {
                // try next format
            }
        }
        // Excel serial date — integer days since 1899-12-30. Tolerate
        // fractional values (time-of-day) by truncating to whole days.
        try {
            double serial = Double.parseDouble(raw);
            return DateUtil.getJavaDate(serial).toInstant()
                .atZone(java.time.ZoneOffset.UTC).toLocalDate().toString();
        } catch (IllegalArgumentException ignore) {
            // NumberFormatException is an IllegalArgumentException — covers
            // both "not a number" and "out-of-range Excel serial" cases.
        }
        return null;
    }

    private Map<String, String> translateHeaders(Map<String, String> row,
                                                 Map<String, String> labelToKey) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : row.entrySet()) {
            String header = e.getKey();
            // Trim surrounding whitespace; Excel / hand-edited CSVs often add it.
            String trimmed = header == null ? null : header.trim();
            String mapped = labelToKey.getOrDefault(trimmed, trimmed);
            if (mapped == null || mapped.isEmpty()) continue;
            out.put(mapped, e.getValue());
        }
        return out;
    }

    private List<Map<String, String>> parseCsv(byte[] bytes) {
        List<Map<String, String>> rows = new ArrayList<>();
        try (InputStream in = new ByteArrayInputStream(bytes);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                 .setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            for (CSVRecord rec : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                parser.getHeaderMap().keySet().forEach(h -> row.put(h, rec.get(h)));
                rows.add(row);
            }
        } catch (IOException e) {
            throw new DomainException("failed to parse CSV: " + e.getMessage());
        }
        return rows;
    }

    private List<Map<String, String>> parseXlsx(byte[] bytes) {
        List<Map<String, String>> rows = new ArrayList<>();
        try (InputStream in = new ByteArrayInputStream(bytes);
             Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) return rows;
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < header.getLastCellNum(); c++) {
                Cell cell = header.getCell(c);
                headers.add(cell == null ? "" : cell.getStringCellValue());
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row dataRow = sheet.getRow(r);
                if (dataRow == null) continue;
                Map<String, String> row = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = dataRow.getCell(c);
                    row.put(headers.get(c), cellAsString(cell));
                }
                rows.add(row);
            }
        } catch (IOException e) {
            throw new DomainException("failed to parse XLSX: " + e.getMessage());
        }
        return rows;
    }

    private String cellAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                // POI: a cell formatted as a date is still NUMERIC under the
                // hood — sniff via the cell style. Emit ISO directly so date
                // fields get the right value without further parsing.
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
