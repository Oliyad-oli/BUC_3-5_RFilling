package com.itas.taxfiling.application.port;

import java.util.Map;
import java.util.UUID;

/**
 * Notification-engine integration. Filing emits notifications for: certificate
 * issued, amendment requested by officer, fraud confirmed (audit copy), etc.
 *
 * All filing reminders + due-date notifications live in scheduled-tasks +
 * notification-engine — filing does NOT manage reminder state (Phase 2 deferral).
 */
public interface NotificationEnginePort {
    void send(String tin, String templateCode, Map<String, Object> variables, UUID correlationId);
}
