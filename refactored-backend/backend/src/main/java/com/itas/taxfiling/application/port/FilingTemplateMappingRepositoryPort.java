package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.FilingTemplateMapping;

import java.util.List;
import java.util.Optional;

public interface FilingTemplateMappingRepositoryPort {

    FilingTemplateMapping save(FilingTemplateMapping mapping);

    Optional<FilingTemplateMapping> findByTaxTypeCode(String taxTypeCode);

    List<FilingTemplateMapping> findAll();

    void deleteByTaxTypeCode(String taxTypeCode);
}
