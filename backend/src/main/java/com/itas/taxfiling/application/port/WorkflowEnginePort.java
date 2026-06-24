package com.itas.taxfiling.application.port;

import java.util.Map;

public interface WorkflowEnginePort {
    void startWorkflow(String processKey, String businessKey, Map<String, Object> variables);
    void signalTask(String taskId, Map<String, Object> variables);
}
