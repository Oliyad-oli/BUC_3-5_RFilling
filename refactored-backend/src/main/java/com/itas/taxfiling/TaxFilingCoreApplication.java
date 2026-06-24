package com.itas.taxfiling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tax Filing Core Server — BUC-003 Return Filing & BUC-005 Return Processing.
 *
 * <p>Architecture: Strict hexagonal layering — api → application → domain ← persistence, engineadapter.
 * Security is owned by the API Gateway + Keycloak. No spring-boot-starter-security on the classpath.
 * Identity propagates via the {@code X-Actor-Id} header.
 *
 * <p>Runs on port 8081 (override via SERVER_PORT env var).
 */
@SpringBootApplicationo
@EnableAsync
@EnableScheduling
public class TaxFilingCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxFilingCoreApplication.class, args);
    }
}
