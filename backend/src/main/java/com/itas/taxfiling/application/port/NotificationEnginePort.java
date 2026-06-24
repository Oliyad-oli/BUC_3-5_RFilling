package com.itas.taxfiling.application.port;

import java.util.Map;

public interface NotificationEnginePort {
    void sendNotification(String tin, String notificationType, Map<String, Object> payload);
}
