-- One-shot cleanup of legacy short-code rows in calendar_period.
--
-- V6 dropped the ALIASES map in RefreshCalendarProjectionUseCase — only
-- canonical codes from tax_type_catalog get written from now on. This deletes
-- the leftover short-code rows (WHT, CIT, TOT, EXCISE, INCOME_TAX) that were
-- written by the old alias-soup code path.
--
-- Idempotent: re-running deletes nothing because the next CalendarRefreshJob
-- tick won't recreate the legacy codes.

DELETE FROM calendar_period
WHERE tax_type_code NOT IN (SELECT code FROM tax_type_catalog);
