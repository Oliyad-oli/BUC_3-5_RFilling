package com.itas.taxfiling.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Amendment Entity
 * 
 * Represents an amendment cycle for a tax return
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Amendment {
    private String id;
    private String reason;
    private Instant requestedAt;
    private Instant acceptedAt;
    
    public Amendment(String id, String reason) {
        this.id = id;
        this.reason = reason;
        this.requestedAt = Instant.now();
    }
    
    public void accept() {
        this.acceptedAt = Instant.now();
    }
    
    public boolean isAccepted() {
        return acceptedAt != null;
    }
}
