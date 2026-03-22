-- Align work_proofs.financial_status check constraint with current application enum.

ALTER TABLE work_proofs
    DROP CONSTRAINT IF EXISTS work_proofs_financial_status_check;

ALTER TABLE work_proofs
    ADD CONSTRAINT work_proofs_financial_status_check
    CHECK (
        financial_status IN (
            'PENDING',
            'NEEDS_REVIEW',
            'REFLECTED'
        )
    );
