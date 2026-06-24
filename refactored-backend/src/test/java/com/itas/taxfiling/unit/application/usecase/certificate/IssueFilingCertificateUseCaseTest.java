package com.itas.taxfiling.unit.application.usecase.certificate;

import com.itas.taxfiling.application.port.DmsPort;
import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.FilingCertificateRepositoryPort;
import com.itas.taxfiling.application.port.NotificationEnginePort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.certificate.IssueFilingCertificateUseCase;
import com.itas.taxfiling.domain.model.FilingCertificate;
import com.itas.taxfiling.domain.valueobject.CertificateReference;
import com.itas.taxfiling.unit.test_support.TaxReturnTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueFilingCertificateUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock FilingCertificateRepositoryPort certificates;
    @Mock DmsPort dms;
    @Mock NotificationEnginePort notifications;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks IssueFilingCertificateUseCase useCase;

    @Test
    void issues_certificate_calls_dms_and_notifies() {
        var t = TaxReturnTestBuilder.accepted();
        when(certificates.findByTaxReturnId(t.getId())).thenReturn(Optional.empty());
        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        when(dms.storeCertificate(any(), anyString(), any())).thenReturn(
            new CertificateReference(UUID.randomUUID(), "FIL-1", Instant.now()));
        when(certificates.save(any(FilingCertificate.class))).thenAnswer(inv -> inv.getArgument(0));

        FilingCertificate cert = useCase.execute(t.getId());

        assertThat(cert).isNotNull();
        verify(notifications).send(any(), anyString(), any(), any());
    }

    @Test
    void idempotent_when_certificate_already_exists() {
        var t = TaxReturnTestBuilder.accepted();
        FilingCertificate existing = FilingCertificate.issue(t.getId(),
            new CertificateReference(UUID.randomUUID(), "FIL-EXISTING", Instant.now()));
        when(certificates.findByTaxReturnId(t.getId())).thenReturn(Optional.of(existing));

        FilingCertificate result = useCase.execute(t.getId());

        assertThat(result).isSameAs(existing);
        verify(dms, never()).storeCertificate(any(), anyString(), any());
        verify(notifications, never()).send(any(), anyString(), any(), any());
    }
}
