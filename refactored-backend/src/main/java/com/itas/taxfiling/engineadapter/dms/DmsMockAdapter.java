package com.itas.taxfiling.engineadapter.dms;

import com.itas.taxfiling.application.port.DmsPort;
import com.itas.taxfiling.domain.valueobject.CertificateReference;
import com.itas.taxfiling.engineadapter.shared.BaseEngineAdapter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/** [MOCK] dms adapter. Replace by adding the dms client library. */
@Slf4j
@Component
public class DmsMockAdapter extends BaseEngineAdapter implements DmsPort {

    public DmsMockAdapter() { super("dms"); }

    @Override
    @CircuitBreaker(name = "dms", fallbackMethod = "storeCertificateFallback")
    @Retry(name = "dms")
    public CertificateReference storeCertificate(UUID taxReturnId, String certificateNumber, byte[] payload) {
        log.info("[MOCK] dms storeCertificate taxReturnId={} cert={} bytes={}",
            taxReturnId, certificateNumber, payload.length);
        return new CertificateReference(UUID.randomUUID(), certificateNumber, Instant.now());
    }

    private CertificateReference storeCertificateFallback(UUID taxReturnId, String certificateNumber,
                                                          byte[] payload, Exception ex) {
        throw wrapException("storeCertificate", ex);
    }
}
