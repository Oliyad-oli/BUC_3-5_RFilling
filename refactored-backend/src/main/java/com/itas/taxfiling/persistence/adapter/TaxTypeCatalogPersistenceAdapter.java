package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.TaxTypeCatalogRepositoryPort;
import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.persistence.jpa.entity.TaxTypeCatalogEntity;
import com.itas.taxfiling.persistence.jpa.repository.TaxTypeCatalogJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaxTypeCatalogPersistenceAdapter implements TaxTypeCatalogRepositoryPort {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TaxTypeCatalogJpaRepository repo;

    @Override
    @Transactional(readOnly = true)
    public List<TaxTypeCatalogEntry> findAllActive() {
        return repo.findByActiveTrueOrderBySortOrder().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaxTypeCatalogEntry> findByCode(TaxTypeCode code) {
        return repo.findByCode(code.value()).map(this::toDomain);
    }

    private TaxTypeCatalogEntry toDomain(TaxTypeCatalogEntity e) {
        List<TaxTypeCatalogEntry.ScheduleSpec> schedules = new ArrayList<>();
        String filingTemplate = null;
        String penaltyTemplate = null;

        if (e.getMetadataJson() != null && !e.getMetadataJson().isBlank()) {
            try {
                JsonNode root = MAPPER.readTree(e.getMetadataJson());
                JsonNode schedulesNode = root.get("schedules");
                if (schedulesNode != null && schedulesNode.isArray()) {
                    for (JsonNode s : schedulesNode) {
                        try {
                            schedules.add(new TaxTypeCatalogEntry.ScheduleSpec(
                                ScheduleKind.valueOf(s.get("kind").asText()),
                                s.has("label") ? s.get("label").asText() : s.get("kind").asText(),
                                s.has("supportsCarryForward") && s.get("supportsCarryForward").asBoolean()));
                        } catch (IllegalArgumentException ignored) {
                            log.warn("Skipping unknown schedule kind in catalog row {}: {}",
                                e.getCode(), s.get("kind"));
                        }
                    }
                }
                JsonNode fRef = root.get("ledgerFilingTemplateRef");
                if (fRef != null && !fRef.isNull()) filingTemplate = fRef.asText();
                JsonNode pRef = root.get("ledgerPenaltyTemplateRef");
                if (pRef != null && !pRef.isNull()) penaltyTemplate = pRef.asText();
            } catch (Exception ex) {
                log.warn("Failed to parse metadata_json for tax type {}: {}", e.getCode(), ex.getMessage());
            }
        }

        return new TaxTypeCatalogEntry(
            e.getCode(),
            e.getLedgerAbbr(),
            e.getName(),
            e.getLegalBasis(),
            PeriodFrequency.valueOf(e.getFrequency()),
            e.getDueOffsetDays(),
            e.getRateKind(),
            e.getStandardRate(),
            e.isLedgerEnabled(),
            e.isActive(),
            e.getSortOrder(),
            schedules,
            filingTemplate,
            penaltyTemplate);
    }
}
