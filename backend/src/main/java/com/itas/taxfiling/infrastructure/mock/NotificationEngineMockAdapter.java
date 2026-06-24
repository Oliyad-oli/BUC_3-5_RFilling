package com.itas.taxfiling.infrastructure.mock;

import com.itas.taxfiling.application.port.NotificationEnginePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mock Notification Engine Adapter
 * 
 * Simulates sending notifications (email, SMS, etc.)
 */
@Slf4j
@Component
public class NotificationEngineMockAdapter implements NotificationEnginePort {

    @Override
    public void sendNotification(String tin, String notificationType, Map<String, Object> payload) {
        log.info("[MOCK-NOTIFICATION] Sending notification: tin={}, type={}, payload={}",
                tin, notificationType, payload);
    }
}
