package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.persistence.jpa.entity.FilingTemplateMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilingTemplateMappingJpaRepository
    extends JpaRepository<FilingTemplateMappingEntity, String> {}
