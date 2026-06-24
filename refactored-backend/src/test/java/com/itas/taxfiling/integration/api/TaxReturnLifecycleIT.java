package com.itas.taxfiling.integration.api;

import com.itas.taxfiling.application.port.RegistrationProjectionPort;
import com.itas.taxfiling.application.usecase.taxreturn.AcceptCalculationUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.DraftTaxReturnUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.RequestCalculationUseCase;
import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.FilingMethod;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.QuestionnaireAnswers;
import com.itas.taxfiling.domain.valueobject.TaxReturnStatus;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end happy path with a real Postgres (Testcontainers) and the in-memory
 * Spring context. Drives draft → calculate → accept; the outbox dispatcher +
 * validation handlers carry the return through to COMPLETED on background
 * threads.
 *
 * Skipped (not failed) when Docker isn't available — the assumption check
 * runs before any container start so the build stays green on dev machines
 * without a working Docker daemon. CI with Docker runs the test in full.
 */
@SpringBootTest
class TaxReturnLifecycleIT {

    private static final boolean DOCKER_AVAILABLE = checkDocker();
    private static final PostgreSQLContainer<?> POSTGRES = DOCKER_AVAILABLE
        ? new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("filing")
            .withUsername("test")
            .withPassword("test")
        : null;

    @BeforeAll
    static void startContainer() {
        assumeTrue(DOCKER_AVAILABLE,
            "Skipping TaxReturnLifecycleIT — Docker is not available on this host.");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        if (DOCKER_AVAILABLE) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            registry.add("spring.flyway.enabled", () -> "true");
        }
    }

    private static boolean checkDocker() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Autowired RegistrationProjectionPort registration;
    @Autowired DraftTaxReturnUseCase draftUseCase;
    @Autowired RequestCalculationUseCase requestCalcUseCase;
    @Autowired AcceptCalculationUseCase acceptCalcUseCase;
    @Autowired com.itas.taxfiling.application.port.TaxReturnRepositoryPort taxReturns;

    @BeforeEach
    void seed() {
        registration.upsertTaxpayer("9999999", UUID.randomUUID().toString(),
            "IT Test Taxpayer", "ACTIVE", true);
    }

    @Test
    void full_happy_path_drafts_calculates_accepts_and_eventually_completes() {
        TaxReturn draft = draftUseCase.execute("9999999", new TaxTypeCode("VAT"),
            Period.monthly(YearMonth.of(2026, 4)), FilingMethod.PORTAL);
        assertThat(draft.getStatus()).isEqualTo(TaxReturnStatus.DRAFT);

        CalculationIteration iter = requestCalcUseCase.execute(
            draft.getId(), QuestionnaireAnswers.empty());
        assertThat(iter.getOutcome()).isNotNull();

        acceptCalcUseCase.execute(draft.getId(), iter.getId());

        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                TaxReturn t = taxReturns.findById(draft.getId()).orElseThrow();
                assertThat(t.getStatus()).isIn(
                    TaxReturnStatus.COMPLETED, TaxReturnStatus.MANUAL_REVIEW);
            });
    }
}
