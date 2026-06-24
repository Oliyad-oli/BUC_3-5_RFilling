package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntryFieldType;
import com.itas.taxfiling.domain.valueobject.EntryTypeStatus;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.persistence.jpa.entity.LineItemEntryTypeEntity;
import com.itas.taxfiling.persistence.jpa.repository.LineItemEntryTypeJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LineItemEntryTypePersistenceAdapter implements LineItemEntryTypeRepositoryPort {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LineItemEntryTypeJpaRepository repository;

    @Override
    @Transactional
    public LineItemEntryType save(LineItemEntryType type) {
        // Load the managed entity if it already exists in the session, then
        // apply the domain state to it. Constructing a fresh detached entity
        // with the same id (the previous behaviour) raises a NonUniqueObject /
        // DuplicateKey exception — Hibernate will not merge two instances with
        // the same id silently. Returning the input instance also keeps any
        // registered domain events intact for the use case to publish.
        LineItemEntryTypeEntity entity = repository.findById(type.getId())
            .orElseGet(LineItemEntryTypeEntity::new);
        applyTo(entity, type);
        repository.save(entity);
        return type;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LineItemEntryType> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LineItemEntryType> findByCodeAndVersion(String code, int version) {
        return repository.findByCodeAndVersion(code, version).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LineItemEntryType> findByTaxTypeAndKindAndStatus(
        TaxTypeCode taxType, ScheduleKind kind, EntryTypeStatus status) {
        return repository.findByTaxTypeAndScheduleKindAndStatus(taxType.value(), kind, status)
            .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int nextVersionForCode(String code) {
        return repository.nextVersionForCode(code);
    }

    private void applyTo(LineItemEntryTypeEntity e, LineItemEntryType t) {
        e.setId(t.getId());
        e.setCode(t.getCode());
        e.setTaxType(t.getTaxType().value());
        e.setScheduleKind(t.getScheduleKind());
        e.setVersion(t.getVersion());
        e.setFields(Map.of("fields", t.getFields().stream().map(this::fieldToMap).toList()));
        e.setStatus(t.getStatus());
        e.setCreatedAt(t.getCreatedAt());
        e.setCreatedByActorId(t.getCreatedByActorId());
    }

    @SuppressWarnings("unchecked")
    private LineItemEntryType toDomain(LineItemEntryTypeEntity e) {
        Object raw = e.getFields().get("fields");
        List<Map<String, Object>> fieldMaps = raw == null
            ? List.of()
            : MAPPER.convertValue(raw, new TypeReference<List<Map<String, Object>>>() {});
        List<EntryFieldDefinition> fields = fieldMaps.stream().map(this::fieldFromMap).toList();
        return LineItemEntryType.rehydrate(
            e.getId(), e.getCode(), new TaxTypeCode(e.getTaxType()), e.getScheduleKind(),
            e.getVersion(), fields, e.getStatus(), e.getCreatedAt(),
            e.getCreatedByActorId(), e.getRevision());
    }

    private Map<String, Object> fieldToMap(EntryFieldDefinition f) {
        return Map.of(
            "key", f.key(),
            "label", f.label(),
            "type", f.type().name(),
            "required", f.required(),
            "allowedValues", f.allowedValues(),
            "validationRegex", f.validationRegex() == null ? "" : f.validationRegex());
    }

    @SuppressWarnings("unchecked")
    private EntryFieldDefinition fieldFromMap(Map<String, Object> m) {
        String regex = (String) m.getOrDefault("validationRegex", "");
        return new EntryFieldDefinition(
            (String) m.get("key"),
            (String) m.get("label"),
            EntryFieldType.valueOf((String) m.get("type")),
            Boolean.TRUE.equals(m.get("required")),
            (List<String>) m.getOrDefault("allowedValues", List.of()),
            regex.isBlank() ? null : regex);
    }
}
