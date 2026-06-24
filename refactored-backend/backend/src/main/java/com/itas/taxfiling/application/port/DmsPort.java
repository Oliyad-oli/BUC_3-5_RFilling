package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.valueobject.CertificateReference;

import java.util.UUID;

/**
 * Document-management-service integration. Used by FilingCertificate generation
 * (BUC-FIL-040) — filing produces the certificate payload, dms stores the
 * binary and returns a reference.
 */
public interface DmsPort {

    CertificateReference storeCertificate(UUID taxReturnId, String certificateNumber, byte[] payload);
}
