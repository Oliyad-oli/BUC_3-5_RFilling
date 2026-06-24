package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.application.usecase.taxreturn.BulkUploadLineItemsUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.BulkUploadLineItemsUseCase.BulkUploadResult;
import com.itas.taxfiling.application.usecase.taxreturn.BulkUploadLineItemsUseCase.Format;
import com.itas.taxfiling.application.usecase.taxreturn.GetBulkUploadTemplateUseCase;
import com.itas.taxfiling.domain.exception.DomainException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * BUC-FIL-004 — bulk upload + template download endpoints. Accepts CSV or
 * XLSX (auto-detected by content type or filename extension); returns a
 * per-row outcome JSON.
 */
@RestController
@RequestMapping("/portal/tax-returns/{taxReturnId}/bulk-upload")
@RequiredArgsConstructor
@Tag(name = "Bulk Upload",
     description = "BUC-FIL-004 — bulk upload of line items + downloadable template")
public class BulkUploadController {

    private final BulkUploadLineItemsUseCase uploadUseCase;
    private final GetBulkUploadTemplateUseCase templateUseCase;

    @PostMapping
    @Operation(summary = "Upload a CSV/XLSX of line items (BUC-FIL-004)")
    public BulkUploadResult upload(@PathVariable UUID taxReturnId,
                                   @RequestParam UUID scheduleId,
                                   @RequestParam UUID entryTypeId,
                                   @RequestParam("file") MultipartFile file) throws IOException {
        Format format = detectFormat(file);
        return uploadUseCase.execute(taxReturnId, scheduleId, entryTypeId, format, file.getBytes());
    }

    @GetMapping("/template")
    @Operation(summary = "Download a CSV template for a given entry type")
    public ResponseEntity<byte[]> template(@PathVariable UUID taxReturnId,
                                           @RequestParam UUID entryTypeId) {
        byte[] body = templateUseCase.execute(entryTypeId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment",
            "bulk-upload-template-" + entryTypeId + ".csv");
        return new ResponseEntity<>(body, headers, 200);
    }

    private Format detectFormat(MultipartFile file) {
        String name = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (name != null && name.toLowerCase().endsWith(".xlsx")) return Format.XLSX;
        if (name != null && name.toLowerCase().endsWith(".csv")) return Format.CSV;
        if ("text/csv".equalsIgnoreCase(contentType)) return Format.CSV;
        if (contentType != null && contentType.contains("spreadsheetml")) return Format.XLSX;
        throw new DomainException("unsupported file format; expected .csv or .xlsx");
    }
}
