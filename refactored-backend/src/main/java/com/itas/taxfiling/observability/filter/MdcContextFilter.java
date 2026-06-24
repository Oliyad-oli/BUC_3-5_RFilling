package com.itas.taxfiling.observability.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Propagates X-Correlation-Id and X-Actor-Id into SLF4J MDC for every request.
 * Generates a UUID correlation ID if the gateway did not provide one.
 */
@Component
@Order(1)
public class MdcContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String correlationId = request.getHeader("X-Correlation-Id");
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            String actorId = request.getHeader("X-Actor-Id");
            if (actorId == null || actorId.isBlank()) {
                actorId = "anonymous";
            }
            MDC.put("correlationId", correlationId);
            MDC.put("actorId", actorId);
            response.setHeader("X-Correlation-Id", correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("actorId");
        }
    }
}
