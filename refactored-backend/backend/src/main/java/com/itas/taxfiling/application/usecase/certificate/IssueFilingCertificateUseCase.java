package com.itas.taxfiling.application.usecase.certificate;

import com.itas.taxfiling.application.port.DmsPort;
import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.FilingCertificateRepositoryPort;
import com.itas.taxfiling.application.port.NotificationEnginePort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.FilingCertificate;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.CertificateReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Generates a FilingCertificate after a return reaches COMPLETED (BUC-FIL-040).
 * Pushes the binary to dms, stores the dms reference locally, and notifies the
 * taxpayer that the certificate is available.
 *
 * Idempotent: if a certificate already exists for the return (e.g. handler
 * fired twice on retry), returns the existing one.
 */
@Service
@RequiredArgsConstructor
public class IssueFilingCertificateUseCase {

    private static final DateTimeFormatter NUMBER_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TaxReturnRepositoryPort taxReturns;
    private final FilingCertificateRepositoryPort certificates;
    private final DmsPort dms;
    private final NotificationEnginePort notifications;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public FilingCertificate execute(UUID taxReturnId) {
        Optional<FilingCertificate> existing = certificates.findByTaxReturnId(taxReturnId);
        if (existing.isPresent()) return existing.get();

        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));

        String certNumber = "FIL-" + t.getTaxType().value() + "-" + t.getPeriod().label()
            + "-" + NUMBER_FMT.format(t.getCreatedAt().atZone(java.time.ZoneOffset.UTC));
        byte[] payload = renderCertificatePayload(t).getBytes(StandardCharsets.UTF_8);
        CertificateReference ref = dms.storeCertificate(taxReturnId, certNumber, payload);

        FilingCertificate fc = FilingCertificate.issue(taxReturnId, ref);
        FilingCertificate saved = certificates.save(fc);
        saved.pullEvents().forEach(eventPublisher::publish);

        notifications.send(t.getTaxpayer().tin(), "filing.certificate-issued",
            Map.of("certificateNumber", certNumber, "taxType", t.getTaxType().value(),
                   "period", t.getPeriod().label()),
            UUID.randomUUID());
        return saved;
    }

    private String renderCertificatePayload(TaxReturn t) {
        // Plain-text payload — production would render PDF via a template engine.
        return ("FILING CERTIFICATE\n"
            + "TIN: " + t.getTaxpayer().tin() + "\n"
            + "Tax type: " + t.getTaxType().value() + "\n"
            + "Period: " + t.getPeriod().label() + "\n"
            + "Status: " + t.getStatus() + "\n");
    }
}
