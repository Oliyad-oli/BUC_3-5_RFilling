package com.itas.taxfiling.observability.filter;

import com.itas.taxfiling.application.port.IdempotencyStorePort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Intercepts unsafe POST requests carrying {@code Idempotency-Key} and replays
 * stored responses. 409 when same key is reused with a different body.
 *
 * <h3>Safety limits</h3>
 * <ul>
 *   <li>Body size capped at {@code itas.idempotency.max-body-bytes} (default 64 KiB).
 *       Larger POSTs pass through untouched — clients cannot use idempotency keys
 *       on uploads, but the service also cannot be OOM'd by buffering a 10 MB request.</li>
 *   <li>Only JSON / problem+json / x-www-form-urlencoded / text are eligible.
 *       Multipart and octet-stream skip the filter outright.</li>
 *   <li>Content-Length is checked <i>before</i> reading the body, not after — a request
 *       with {@code Content-Length: 100000000} aborts at the header check, never
 *       allocating the byte buffer. An earlier revision of this filter slurped first
 *       and checked after, which still allowed the OOM path; that has been fixed.</li>
 * </ul>
 *
 * <p>This filter is intentionally identical in shape to the payment service's filter
 * — they solve the same problem against the same store contract. When the diff is
 * just package names, that's the cue to extract a shared
 * {@code itas-observability-commons} module.
 */
@Component
@Order(2)
@Slf4j
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String HEADER = "Idempotency-Key";
    private static final long TTL_HOURS = 24L;

    private final IdempotencyStorePort store;
    private final long maxBodyBytes;

    public IdempotencyFilter(IdempotencyStorePort store,
                             @Value("${itas.idempotency.max-body-bytes:65536}") long maxBodyBytes) {
        this.store = store;
        this.maxBodyBytes = maxBodyBytes;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse resp,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, resp);
            return;
        }
        String key = req.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            chain.doFilter(req, resp);
            return;
        }
        if (!isEligibleContentType(req.getContentType())) {
            // Multipart uploads, octet-stream, etc. — skip idempotency (cannot safely buffer).
            chain.doFilter(req, resp);
            return;
        }
        long declared = req.getContentLengthLong();
        if (declared > maxBodyBytes) {
            log.debug("Idempotency-Key ignored: body size {} > limit {}", declared, maxBodyBytes);
            chain.doFilter(req, resp);
            return;
        }
        if (declared < 0) {
            log.debug("Idempotency-Key ignored: unknown body size");
            chain.doFilter(req, resp);
            return;
        }

        String endpoint = req.getRequestURI();
        byte[] requestBytes = req.getInputStream().readAllBytes();
        if (requestBytes.length > maxBodyBytes) {
            // Belt-and-braces: declared-length was small but actual body exceeded the cap.
            log.debug("Idempotency-Key ignored: body {} exceeded limit {} post-read",
                    requestBytes.length, maxBodyBytes);
            chain.doFilter(req, resp);
            return;
        }
        String requestBody = new String(requestBytes, StandardCharsets.UTF_8);
        String requestHash = sha256(requestBody);

        CachedBodyHttpServletRequest wrappedReq = new CachedBodyHttpServletRequest(req, requestBytes);
        ContentCachingResponseWrapper wrappedResp = new ContentCachingResponseWrapper(resp);

        try {
            Optional<IdempotencyStorePort.StoredResponse> existing = store.find(key, endpoint);
            if (existing.isPresent()) {
                IdempotencyStorePort.StoredResponse stored = existing.get();
                if (!stored.requestHash().equals(requestHash)) {
                    resp.setStatus(HttpStatus.CONFLICT.value());
                    resp.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    resp.getWriter()
                            .write("{\"title\":\"Idempotency conflict\",\"detail\":\"Same key with different body\"}");
                    return;
                }
                resp.setStatus(stored.statusCode());
                resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
                resp.getWriter().write(stored.responseBody());
                return;
            }
        } catch (Exception e) {
            log.warn("Idempotency disabled for request: store lookup failed (key={}, endpoint={})", key, endpoint, e);
            chain.doFilter(wrappedReq, resp);
            return;
        }

        try {
            chain.doFilter(wrappedReq, wrappedResp);

            int status = wrappedResp.getStatus();
            if (status >= 200 && status < 300) {
                String responseBody = new String(wrappedResp.getContentAsByteArray(), StandardCharsets.UTF_8);
                try {
                    store.store(
                            key, endpoint, requestHash, status, responseBody,
                            Instant.now().plus(TTL_HOURS, ChronoUnit.HOURS));
                } catch (Exception e) {
                    log.warn("Idempotency store failed (key={}, endpoint={}, status={})", key, endpoint, status, e);
                }
            }
        } finally {
            wrappedResp.copyBodyToResponse();
        }
    }

    private static boolean isEligibleContentType(String contentType) {
        if (contentType == null) return true; // treat unknown as JSON-ish
        String n = contentType.toLowerCase();
        return n.startsWith("application/json")
                || n.startsWith("application/problem+json")
                || n.startsWith("application/x-www-form-urlencoded")
                || n.startsWith("text/");
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
