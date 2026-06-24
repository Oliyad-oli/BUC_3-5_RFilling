package com.itas.taxfiling.infrastructure.config;

import com.itas.taxfiling.application.port.IdempotencyStorePort;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Idempotency Filter
 * 
 * Checks the X-Idempotency-Key header for POST requests
 * and returns cached responses for duplicate requests.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class IdempotencyFilter implements Filter {

    private final IdempotencyStorePort idempotencyStore;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Only apply to POST requests
        if (!"POST".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String idempotencyKey = httpRequest.getHeader("X-Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        // Check if already processed
        var cached = idempotencyStore.getResponse(idempotencyKey);
        if (cached.isPresent()) {
            log.info("Idempotent request detected: key={}", idempotencyKey);
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(200);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(cached.get());
            return;
        }

        chain.doFilter(request, response);
    }
}
