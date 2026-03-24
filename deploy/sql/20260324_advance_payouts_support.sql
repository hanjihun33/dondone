-- Add advance_payouts to persist treasury-to-wallet payout state for advances.
-- The table is intentionally independent from job execution so payout lifecycle
-- can be replayed, retried and audited safely.

CREATE TABLE IF NOT EXISTS advance_payouts (
    advance_payout_id VARCHAR(64) PRIMARY KEY,
    advance_request_id BIGINT NOT NULL REFERENCES advance_requests (id),
    user_id BIGINT NOT NULL REFERENCES users (id),
    wallet_address VARCHAR(42) NOT NULL,
    amount_atomic BIGINT NOT NULL,
    asset_symbol VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL,
    tx_hash VARCHAR(66),
    signed_transaction TEXT,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_advance_payouts_advance_request
    ON advance_payouts (advance_request_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_advance_payouts_user_idempotency
    ON advance_payouts (user_id, idempotency_key);

CREATE UNIQUE INDEX IF NOT EXISTS uk_advance_payouts_tx_hash
    ON advance_payouts (tx_hash)
    WHERE tx_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_advance_payouts_user_created_at
    ON advance_payouts (user_id, created_at DESC, advance_payout_id DESC);

CREATE INDEX IF NOT EXISTS idx_advance_payouts_status_updated_at
    ON advance_payouts (status, updated_at DESC, advance_payout_id DESC);

CREATE INDEX IF NOT EXISTS idx_advance_payouts_user_status
    ON advance_payouts (user_id, status);
