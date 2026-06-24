-- One-shot cleanup of calendar_period rows whose frequency doesn't match the
-- catalog. Before V6 the period generator defaulted unknown codes to MONTHLY,
-- which produced monthly rows for annual tax types (INCOME_TAX_BUSINESS) and
-- quarterly types (TURNOVER_TAX). After V6 the generator reads frequency from
-- tax_type_catalog, but the old rows persist. This deletes them so the next
-- CalendarRefreshJob tick produces a consistent set.
--
-- Idempotent — re-running deletes nothing once the calendar is consistent.

DELETE FROM calendar_period cp
USING tax_type_catalog ttc
WHERE cp.tax_type_code = ttc.code
  AND cp.frequency    <> ttc.frequency;
