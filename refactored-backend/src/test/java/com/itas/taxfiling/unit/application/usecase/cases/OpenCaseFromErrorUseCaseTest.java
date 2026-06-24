package com.itas.taxfiling.unit.application.usecase.cases;

import com.itas.taxfiling.application.port.CaseManagementPort;
import com.itas.taxfiling.application.port.CaseManagementPort.CaseHandle;
import com.itas.taxfiling.application.port.CaseManagementPort.OpenCaseFromErrorRequest;
import com.itas.taxfiling.application.usecase.cases.OpenCaseFromErrorUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenCaseFromErrorUseCaseTest {

    @Mock CaseManagementPort caseManagement;
    @InjectMocks OpenCaseFromErrorUseCase useCase;

    @Test
    void delegates_to_case_management_port() {
        UUID caseId = UUID.randomUUID();
        when(caseManagement.openCaseFromError(any())).thenReturn(
            new CaseHandle(caseId, "CASE-2026-001234", "https://x"));

        var request = new OpenCaseFromErrorRequest(
            "filing", "BUC-FIL-013", "ENGINE_TIMEOUT", UUID.randomUUID(),
            UUID.randomUUID(), null, "ENGINE_TIMEOUT", "Ledger engine timed out",
            null, Instant.now(), "PORTAL");

        var handle = useCase.execute(request);
        assertThat(handle.caseId()).isEqualTo(caseId);
    }
}
