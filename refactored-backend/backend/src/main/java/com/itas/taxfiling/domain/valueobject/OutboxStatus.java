package com.itas.taxfiling.domain.valueobject;

/** Outbox entry delivery status. */
public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
