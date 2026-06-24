package com.itas.taxfiling.engineadapter.workflow;

import com.itas.taxfiling.application.port.WorkflowEnginePort;
import com.itas.taxfiling.engineadapter.shared.BaseEngineAdapter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** [MOCK] workflow-engine adapter. Replace by adding the workflow-engine client library. */
@Slf4j
@Component
public class WorkflowEngineMockAdapter extends BaseEngineAdapter implements WorkflowEnginePort {

    public WorkflowEngineMockAdapter() { super("workflow-engine"); }

    @Override
    @CircuitBreaker(name = "workflow-engine", fallbackMethod = "startWorkflowFallback")
    @Retry(name = "workflow-engine")
    public UUID startWorkflow(String workflowCode, Map<String, Object> variables) {
        UUID instanceId = UUID.randomUUID();
        log.info("[MOCK] workflow start code={} instanceId={}", workflowCode, instanceId);
        return instanceId;
    }

    private UUID startWorkflowFallback(String workflowCode, Map<String, Object> variables, Exception ex) {
        throw wrapException("startWorkflow", ex);
    }

    @Override
    @CircuitBreaker(name = "workflow-engine", fallbackMethod = "signalFallback")
    @Retry(name = "workflow-engine")
    public void signal(UUID workflowInstanceId, String signal, Map<String, Object> payload) {
        log.info("[MOCK] workflow signal instanceId={} signal={}", workflowInstanceId, signal);
    }

    private void signalFallback(UUID workflowInstanceId, String signal, Map<String, Object> payload, Exception ex) {
        throw wrapException("signal", ex);
    }
}
