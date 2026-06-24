package com.itas.taxfiling.application.usecase.templates;

import com.itas.taxfiling.application.port.FilingTemplateMappingRepositoryPort;
import com.itas.taxfiling.domain.model.FilingTemplateMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * BUC-FIL-CONFIG-LDG-01 — admin maintains the mapping from tax type to ledger
 * template references used by filing-service when posting to ledger-engine.
 *
 * Five tiny use cases (one per HTTP verb / list / get) co-located so the
 * package stays compact.
 */
@Service
@RequiredArgsConstructor
public class FilingTemplateMappingUseCases {

    private final FilingTemplateMappingRepositoryPort repo;

    @Transactional(readOnly = true)
    public List<FilingTemplateMapping> list() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public FilingTemplateMapping get(String taxTypeCode) {
        return repo.findByTaxTypeCode(taxTypeCode)
            .orElseThrow(() -> new MappingNotFoundException(
                "filing template mapping not found for tax type: " + taxTypeCode));
    }

    @Transactional
    public FilingTemplateMapping create(
            String taxTypeCode,
            String filingTemplateRef,
            String lateFilingPenaltyTemplateRef,
            String actorId) {
        repo.findByTaxTypeCode(taxTypeCode).ifPresent(existing -> {
            throw new MappingAlreadyExistsException(
                "filing template mapping already exists for tax type: " + taxTypeCode);
        });
        FilingTemplateMapping mapping = FilingTemplateMapping.create(
            taxTypeCode, filingTemplateRef, lateFilingPenaltyTemplateRef, actorId);
        return repo.save(mapping);
    }

    @Transactional
    public FilingTemplateMapping update(
            String taxTypeCode,
            String filingTemplateRef,
            String lateFilingPenaltyTemplateRef,
            String actorId) {
        FilingTemplateMapping mapping = get(taxTypeCode);
        mapping.update(filingTemplateRef, lateFilingPenaltyTemplateRef, actorId);
        return repo.save(mapping);
    }

    @Transactional
    public void delete(String taxTypeCode) {
        if (repo.findByTaxTypeCode(taxTypeCode).isEmpty()) {
            throw new MappingNotFoundException(
                "filing template mapping not found for tax type: " + taxTypeCode);
        }
        repo.deleteByTaxTypeCode(taxTypeCode);
    }

    public static class MappingNotFoundException extends RuntimeException {
        public MappingNotFoundException(String msg) { super(msg); }
    }

    public static class MappingAlreadyExistsException extends RuntimeException {
        public MappingAlreadyExistsException(String msg) { super(msg); }
    }
}
