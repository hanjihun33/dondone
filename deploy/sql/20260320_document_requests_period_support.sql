-- Generalize document_generation_requests so period-based documents such as
-- WORKPROOF_STATEMENT can coexist with month-based documents.

ALTER TABLE document_generation_requests
    ADD COLUMN IF NOT EXISTS start_date DATE;

ALTER TABLE document_generation_requests
    ADD COLUMN IF NOT EXISTS end_date DATE;

ALTER TABLE document_generation_requests
    ADD COLUMN IF NOT EXISTS file_name VARCHAR(255);

ALTER TABLE document_generation_requests
    ADD COLUMN IF NOT EXISTS generated_at TIMESTAMP;

ALTER TABLE document_generation_requests
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(100);

ALTER TABLE document_generation_requests
    ALTER COLUMN year_month DROP NOT NULL;
