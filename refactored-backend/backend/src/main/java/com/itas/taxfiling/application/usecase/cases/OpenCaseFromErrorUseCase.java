package com.itas.taxfiling.application.usecase.cases;

import com.itas.taxfiling.application.port.CaseManagementPort;
import com.itas.taxfiling.application.port.CaseManagementPort.CaseHandle;
import com.itas.taxfiling.application.port.CaseManagementPort.OpenCaseFromErrorRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule 13 — opens a support case after an engine call fails. Two flows:
 *   - Flow A (pre-auth): officer-on-behalf-of-taxpayer where session can't be
 *     established. Identity is provided in the request.
 *   - Flow B (post-auth): standard portal/back-office flow. partyId is set
 *     from the authenticated session.
 */
@Service
@RequiredArgsConstructor
public class OpenCaseFromErrorUseCase {

    private final CaseManagementPort caseManagement;

    @Transactional
    public CaseHandle execute(OpenCaseFromErrorRequest request) {
        return caseManagement.openCaseFromError(request);
    }
}
