package com.itas.taxfiling.engineadapter.casemanagement;

import com.itas.taxfiling.application.port.CaseManagementPort;
import com.itas.taxfiling.engineadapter.shared.BaseEngineAdapter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** [MOCK] case-management adapter. Replace by adding the case-management client library. */
@Slf4j
@Component
public class CaseManagementMockAdapter extends BaseEngineAdapter implements CaseManagementPort {

    public CaseManagementMockAdapter() { super("case-management"); }

    @Override
    @CircuitBreaker(name = "case-management", fallbackMethod = "openFraudCaseFallback")
    @Retry(name = "case-management")
    public UUID openFraudCase(UUID taxReturnId, String tin, String narrative, String officerActorId) {
        UUID caseId = UUID.randomUUID();
        log.info("[MOCK] case-management openFraudCase taxReturnId={} tin={} caseId={}",
            taxReturnId, tin, caseId);
        return caseId;
    }

    private UUID openFraudCaseFallback(UUID taxReturnId, String tin, String narrative,
                                       String officerActorId, Exception ex) {
        throw wrapException("openFraudCase", ex);
    }

    @Override
    @CircuitBreaker(name = "case-management", fallbackMethod = "openCaseFromErrorFallback")
    @Retry(name = "case-management")
    public CaseHandle openCaseFromError(OpenCaseFromErrorRequest request) {
        UUID caseId = UUID.randomUUID();
        String ref = "CASE-" + java.time.LocalDate.now().getYear() + "-" + caseId.toString().substring(0, 6);
        log.info("[MOCK] case-management openCaseFromError sourceBuc={} errorCode={} caseId={} ref={}",
            request.sourceBuc(), request.errorCode(), caseId, ref);
        return new CaseHandle(caseId, ref, "https://cases.example/" + caseId);
    }

    private CaseHandle openCaseFromErrorFallback(OpenCaseFromErrorRequest request, Exception ex) {
        throw wrapException("openCaseFromError", ex);
    }

    @Override
    @CircuitBreaker(name = "case-management", fallbackMethod = "listCategoriesForFallback")
    @Retry(name = "case-management")
    public List<IssueCategory> listCategoriesFor(SourceContext context) {
        log.info("[MOCK] case-management listCategoriesFor service={} buc={}",
            context.sourceService(), context.sourceBuc());
        return List.of(
            new IssueCategory("ENGINE_TIMEOUT", "Engine timeout / unavailable", true),
            new IssueCategory("EINVOICE_MALFORMED", "E-invoice data is malformed", true),
            new IssueCategory("CALCULATION_ERROR", "Tax calculation didn't match expectation", true),
            new IssueCategory("AMENDMENT_BLOCKED", "Cannot open amendment", true),
            new IssueCategory("OTHER", "Other (please describe)", true)
        );
    }

    private List<IssueCategory> listCategoriesForFallback(SourceContext context, Exception ex) {
        throw wrapException("listCategoriesFor", ex);
    }
}
