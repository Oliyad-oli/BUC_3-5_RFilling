package com.itas.taxfiling.engineadapter.notification;

import com.itas.taxfiling.application.port.NotificationEnginePort;
import com.itas.taxfiling.engineadapter.shared.BaseEngineAdapter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** [MOCK] notification-engine adapter. Replace by adding the notification-engine client library. */
@Slf4j
@Component
public class NotificationEngineMockAdapter extends BaseEngineAdapter implements NotificationEnginePort {

    public NotificationEngineMockAdapter() { super("notification-engine"); }

    @Override
    @CircuitBreaker(name = "notification-engine", fallbackMethod = "sendFallback")
    @Retry(name = "notification-engine")
    public void send(String tin, String templateCode, Map<String, Object> variables, UUID correlationId) {
        log.info("[MOCK] notification send tin={} template={} correlationId={}", tin, templateCode, correlationId);
    }

    private void sendFallback(String tin, String templateCode, Map<String, Object> variables,
                              UUID correlationId, Exception ex) {
        throw wrapException("send", ex);
    }
}
