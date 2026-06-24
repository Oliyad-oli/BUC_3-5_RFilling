package com.itas.taxfiling.api.advice;

import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.exception.EngineAdapterException;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Translates exceptions to RFC 7807 ProblemDetail responses.
 * No security exceptions — auth is gateway-owned.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        p.setTitle("Validation Failed");
        p.setType(URI.create("urn:itas:filing:validation-error"));
        p.setProperty("timestamp", Instant.now());
        p.setProperty("violations", ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid")));
        return p;
    }

    /**
     * 404 path — caller asked for an aggregate / entity that doesn't exist.
     *
     * <p>Declared with a more-specific {@code @ExceptionHandler} type so it wins
     * over {@link #handleDomain(DomainException)} for {@link ResourceNotFoundException}
     * subclasses. The legacy string-match fallback in {@code handleDomain} is
     * retained for {@code XxxNotFoundException}s that haven't been refactored to
     * extend {@link ResourceNotFoundException} yet — when they all do, that
     * fallback can be removed.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        p.setTitle("Resource Not Found");
        p.setType(URI.create("urn:itas:filing:not-found"));
        p.setProperty("timestamp", Instant.now());
        p.setDetail(ex.getMessage());
        return p;
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex) {
        log.warn("Domain rule violation: {}", ex.getMessage());

        // Fallback string-match for legacy XxxNotFoundException types that haven't yet
        // been refactored to extend ResourceNotFoundException. Once those are migrated
        // this branch can go.
        boolean isLegacyNotFound = ex.getMessage() != null
                && ex.getMessage().toLowerCase().contains("not found");
        HttpStatus status = isLegacyNotFound ? HttpStatus.NOT_FOUND : HttpStatus.UNPROCESSABLE_ENTITY;

        ProblemDetail p = ProblemDetail.forStatus(status);
        p.setTitle(isLegacyNotFound ? "Resource Not Found" : "Business Rule Violation");
        p.setType(URI.create(isLegacyNotFound ? "urn:itas:filing:not-found" : "urn:itas:filing:domain-error"));
        p.setProperty("timestamp", Instant.now());
        p.setDetail(ex.getMessage());
        return p;
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, ObjectOptimisticLockingFailureException.class})
    public ProblemDetail handleOptimisticLock(Exception ex) {
        log.warn("Concurrent modification detected: {}", ex.getMessage());
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        p.setTitle("Concurrent Modification");
        p.setType(URI.create("urn:itas:filing:concurrent-modification"));
        p.setProperty("timestamp", Instant.now());
        p.setDetail("This resource was modified by another request. Please reload and retry.");
        return p;
    }

    @ExceptionHandler(EngineAdapterException.class)
    public ProblemDetail handleEngine(EngineAdapterException ex) {
        log.error("Engine adapter error: engine={} op={}", ex.getEngineName(), ex.getOperation(), ex);
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        p.setTitle("Upstream Engine Unavailable");
        p.setType(URI.create("urn:itas:filing:engine-error"));
        p.setProperty("timestamp", Instant.now());
        p.setDetail("A core engine is temporarily unavailable. Please retry.");
        return p;
    }

    /** Spring Web throws this when a request URL doesn't match any controller. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex) {
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        p.setTitle("Resource Not Found");
        p.setType(URI.create("urn:itas:filing:not-found"));
        p.setDetail("No endpoint for: " + ex.getResourcePath());
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        p.setTitle("Internal Server Error");
        p.setType(URI.create("urn:itas:filing:internal-error"));
        p.setProperty("timestamp", Instant.now());
        return p;
    }
}
