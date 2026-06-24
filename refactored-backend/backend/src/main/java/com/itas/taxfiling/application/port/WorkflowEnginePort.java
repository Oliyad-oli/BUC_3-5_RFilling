package com.itas.taxfiling.application.port;

import java.util.Map;
import java.util.UUID;

/**
 * Workflow-engine integration. Filing triggers a workflow when an officer
 * review item is queued (BUC-FIL-050) so the review SLA can be tracked.
 */
public interface WorkflowEnginePort {

    UUID startWorkflow(String workflowCode, Map<String, Object> variables);

    void signal(UUID workflowInstanceId, String signal, Map<String, Object> payload);
}
