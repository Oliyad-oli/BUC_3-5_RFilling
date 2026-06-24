package com.itas.taxfiling.infrastructure.mock;

import com.itas.taxfiling.application.port.WorkflowEnginePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mock Workflow Engine Adapter
 * 
 * Simulates workflow routing. In production, this would call Camunda or similar.
 */
@Slf4j
@Component
public class WorkflowEngineMockAdapter implements WorkflowEnginePort {

    @Override
    public void startWorkflow(String processKey, String businessKey, Map<String, Object> variables) {
        log.info("[MOCK-WORKFLOW] Starting workflow: process={}, key={}, vars={}",
                processKey, businessKey, variables);
    }

    @Override
    public void signalTask(String taskId, Map<String, Object> variables) {
        log.info("[MOCK-WORKFLOW] Signalling task: taskId={}, vars={}", taskId, variables);
    }
}
