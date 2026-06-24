package com.itas.taxfiling.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Pull e-invoices into a schedule (BUC-FIL-005).
 *
 * <p>The pull is always scoped to the tax return's filing period — no
 * separate from/to inputs. Dedup is enforced by the use case: any e-invoice
 * whose {@code externalInvoiceId} already lives on a line of the target
 * schedule is silently skipped and reported back in {@code linesSkipped}.
 */
public record PullEInvoiceRequest(@NotNull UUID entryTypeId) {}
