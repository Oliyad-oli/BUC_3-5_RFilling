package com.itas.taxfiling.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Mandatory architecture rules for filing-service.
 * Every rule failure indicates a layering or boundary leak — fix the source, not the test.
 */
@AnalyzeClasses(
    packages = "com.itas.taxfiling",
    importOptions = {ImportOption.DoNotIncludeTests.class}
)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule layered_dependencies =
        layeredArchitecture()
            .consideringAllDependencies()
            .layer("Api").definedBy("com.itas.taxfiling.api..")
            .layer("Application").definedBy("com.itas.taxfiling.application..")
            .layer("Domain").definedBy("com.itas.taxfiling.domain..")
            .layer("Persistence").definedBy("com.itas.taxfiling.persistence..")
            .layer("EngineAdapter").definedBy("com.itas.taxfiling.engineadapter..")
            .layer("Observability").definedBy("com.itas.taxfiling.observability..")
            .layer("Config").definedBy("com.itas.taxfiling.config..")
            .whereLayer("Api").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Api", "Persistence", "EngineAdapter", "Observability", "Config")
            .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Config")
            .whereLayer("EngineAdapter").mayOnlyBeAccessedByLayers("Config");

    @ArchTest
    static final ArchRule domain_isolation =
        noClasses()
            .that().resideInAPackage("com.itas.taxfiling.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "jakarta.persistence..",
                "org.springframework..",
                "com.itas.taxfiling.persistence..",
                "com.itas.taxfiling.api..",
                "com.itas.taxfiling.engineadapter..");

    @ArchTest
    static final ArchRule no_engine_adapter_calls_from_api =
        noClasses()
            .that().resideInAPackage("com.itas.taxfiling.api..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.itas.taxfiling.engineadapter..");

    @ArchTest
    static final ArchRule persistence_does_not_depend_on_application_classes =
        noClasses()
            .that().resideInAPackage("com.itas.taxfiling.persistence..")
            .and().resideOutsideOfPackage("com.itas.taxfiling.persistence.adapter..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.itas.taxfiling.application..");

    @ArchTest
    static final ArchRule no_spring_security_anywhere =
        noClasses()
            .that().resideInAPackage("com.itas.taxfiling..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework.security..",
                "org.springframework.boot.autoconfigure.security..");
}
