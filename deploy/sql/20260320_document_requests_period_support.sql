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

ALTER TABLE document_generation_requests
    ALTER COLUMN wage_verification_id DROP NOT NULL;

ALTER TABLE document_generation_requests
    DROP CONSTRAINT IF EXISTS document_generation_requests_document_type_check;

ALTER TABLE document_generation_requests
    ADD CONSTRAINT document_generation_requests_document_type_check
    CHECK (
        document_type IN (
            'PROOF_PACK',
            'CLAIM_KIT',
            'TRANSFER_RECEIPT',
            'WORKPROOF_STATEMENT'
        )
    );
