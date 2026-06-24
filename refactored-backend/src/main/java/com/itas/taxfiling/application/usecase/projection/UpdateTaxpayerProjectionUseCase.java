package com.itas.taxfiling.application.usecase.projection;

import com.itas.taxfiling.application.port.RegistrationProjectionPort;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Internal — applies registration-service projection events into the local
 * read-models taxpayer_projection + subledger_projection (Rule 10). Webhook
 * controller calls this; the port write goes through the
 * RegistrationProjectionDbAdapter, which owns the transactional boundary.
 */
@Service
@RequiredArgsConstructor
public class UpdateTaxpayerProjectionUseCase {

    private final RegistrationProjectionPort registration;

    public void upsertTaxpayer(String tin, String partyId, String legalName,
                               String status, boolean active) {
        registration.upsertTaxpayer(tin, partyId, legalName, status, active);
    }

    public void upsertSubledgers(String tin, String taxType,
                                 UUID principal, UUID penalty, UUID interest, UUID refund) {
        registration.upsertSubledgers(tin, new TaxTypeCode(taxType),
            principal, penalty, interest, refund);
    }
}
