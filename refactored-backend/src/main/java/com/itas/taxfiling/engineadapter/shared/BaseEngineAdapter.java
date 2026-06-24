package com.itas.taxfiling.engineadapter.shared;

import com.itas.taxfiling.domain.exception.EngineAdapterException;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for all engine adapters — provides common error-wrapping.
 */
@Slf4j
public abstract class BaseEngineAdapter {
    protected final String engineName;

    protected BaseEngineAdapter(String engineName) {
        this.engineName = engineName;
    }

    protected EngineAdapterException wrapException(String operation, Exception ex) {
        log.error("Engine call failed engine={} op={} error={}", engineName, operation, ex.getMessage());
        return new EngineAdapterException(engineName, operation, ex);
    }
}
