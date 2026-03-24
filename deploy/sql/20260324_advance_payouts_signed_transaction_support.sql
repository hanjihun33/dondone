ALTER TABLE advance_payouts
    ADD COLUMN IF NOT EXISTS signed_transaction TEXT;
