package com.itas.taxfiling.domain.exception;

/**
 * Thrown when an engine adapter (ledger, risk, rule, etc.) returns an error or
 * the circuit breaker trips. Maps to HTTP 502 Bad Gateway.
 */
public class EngineAdapterException extends DomainException {

    public EngineAdapterException(String engine, String detail) {
        super("Engine adapter [" + engine + "] failed: " + detail);
    }

    public EngineAdapterException(String engine, String detail, Throwable cause) {
        super("Engine adapter [" + engine + "] failed: " + detail, cause);
    }
}
