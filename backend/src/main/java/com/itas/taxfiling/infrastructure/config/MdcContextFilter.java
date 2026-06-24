package com.itas.taxfiling.infrastructure.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC Context Filter
 * 
 * Adds correlation ID and request metadata to MDC for structured logging.
 */
@Component
@Order(1)
public class MdcContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String correlationId = httpRequest.getHeader("X-Correlation-Id");
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put("correlationId", correlationId);
            MDC.put("requestMethod", httpRequest.getMethod());
            MDC.put("requestUri", httpRequest.getRequestURI());

            String actorId = httpRequest.getHeader("X-Actor-Id");
            if (actorId != null) {
                MDC.put("actorId", actorId);
            }

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
