package com.itas.taxfiling.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's {@code @Scheduled} task support for background jobs such as
 * the idempotency record TTL cleanup.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
