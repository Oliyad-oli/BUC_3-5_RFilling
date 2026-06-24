-- Migrate legacy `taxpayer_obligation` rows that use the ambiguous
-- `INCOME_TAX` code to the canonical split (INCOME_TAX_INDIVIDUAL vs
-- INCOME_TAX_BUSINESS). The catalog (V6) only knows about the split.
--
-- Heuristic: TIN prefix.
--   ETH-TIN-ENT-* → entity / business → INCOME_TAX_BUSINESS
--   ETH-TIN-IND-* → individual         → INCOME_TAX_INDIVIDUAL
--   anything else → leave alone (operator fixes manually)
--
-- Idempotent: only flips rows still using the bare `INCOME_TAX` code.

UPDATE taxpayer_obligation
SET tax_type_code = 'INCOME_TAX_BUSINESS'
WHERE tax_type_code = 'INCOME_TAX'
  AND tin LIKE 'ETH-TIN-ENT-%';

UPDATE taxpayer_obligation
SET tax_type_code = 'INCOME_TAX_INDIVIDUAL'
WHERE tax_type_code = 'INCOME_TAX'
  AND tin LIKE 'ETH-TIN-IND-%';

-- Same fix on filing_period rows linked to the migrated obligations.
UPDATE filing_period
SET tax_type_code = 'INCOME_TAX_BUSINESS'
WHERE tax_type_code = 'INCOME_TAX'
  AND tin LIKE 'ETH-TIN-ENT-%';

UPDATE filing_period
SET tax_type_code = 'INCOME_TAX_INDIVIDUAL'
WHERE tax_type_code = 'INCOME_TAX'
  AND tin LIKE 'ETH-TIN-IND-%';
