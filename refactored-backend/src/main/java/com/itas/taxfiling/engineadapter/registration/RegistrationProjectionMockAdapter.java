package com.itas.taxfiling.engineadapter.registration;

import com.itas.taxfiling.application.port.RegistrationProjectionPort;
import com.itas.taxfiling.domain.valueobject.SubledgerGroupReference;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.engineadapter.shared.BaseEngineAdapter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * [MOCK] registration-service projection adapter. Production reads a local
 * read-model populated by registration-service projection events; mock returns
 * deterministic values so the rest of the flow can be exercised.
 */
@Slf4j
@Component
public class RegistrationProjectionMockAdapter extends BaseEngineAdapter implements RegistrationProjectionPort {

    public RegistrationProjectionMockAdapter() { super("registration-service"); }

    @Override
    @CircuitBreaker(name = "registration-service", fallbackMethod = "findByTinFallback")
    @Retry(name = "registration-service")
    public Optional<TaxpayerSnapshot> findByTin(String tin) {
        log.info("[MOCK] registration projection findByTin tin={}", tin);
        return Optional.of(new TaxpayerSnapshot(tin, UUID.randomUUID().toString(),
            "MOCK Taxpayer " + tin, "ACTIVE", true));
    }

    private Optional<TaxpayerSnapshot> findByTinFallback(String tin, Exception ex) {
        throw wrapException("findByTin", ex);
    }

    @Override
    @CircuitBreaker(name = "registration-service", fallbackMethod = "findSubledgersFallback")
    @Retry(name = "registration-service")
    public Optional<SubledgerGroupReference> findSubledgers(String tin, TaxTypeCode taxType) {
        log.info("[MOCK] registration projection findSubledgers tin={} taxType={}", tin, taxType);
        return Optional.of(new SubledgerGroupReference(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
    }

    private Optional<SubledgerGroupReference> findSubledgersFallback(String tin, TaxTypeCode taxType, Exception ex) {
        throw wrapException("findSubledgers", ex);
    }

    @Override
    @CircuitBreaker(name = "registration-service", fallbackMethod = "listActiveFallback")
    @Retry(name = "registration-service")
    public java.util.List<TaxpayerSnapshot> listActive() {
        log.info("[MOCK] registration projection listActive");
        return java.util.List.of();
    }

    private java.util.List<TaxpayerSnapshot> listActiveFallback(Exception ex) {
        throw wrapException("listActive", ex);
    }

    @Override
    public void upsertTaxpayer(String tin, String partyId, String legalName, String status, boolean active) {
        log.info("[MOCK] registration upsertTaxpayer tin={} status={}", tin, status);
    }

    @Override
    public void upsertSubledgers(String tin, TaxTypeCode taxType,
                                 UUID principal, UUID penalty, UUID interest, UUID refund) {
        log.info("[MOCK] registration upsertSubledgers tin={} taxType={}", tin, taxType);
    }
}
