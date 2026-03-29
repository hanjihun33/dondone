ALTER TABLE work_contracts
    ADD COLUMN IF NOT EXISTS payday_day INTEGER;

UPDATE work_contracts
SET payday_day = 31
WHERE payday_day IS NULL;

ALTER TABLE work_contracts
    ALTER COLUMN payday_day SET NOT NULL;
