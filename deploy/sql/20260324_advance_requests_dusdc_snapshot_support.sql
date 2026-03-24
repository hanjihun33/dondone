-- Normalize advance_requests to the dUSDC-based amount model and
-- persist KRW display snapshots explicitly for operational deployments.
--
-- Goals:
-- 1. Create advance_requests when it does not exist.
-- 2. Backfill the current dUSDC atomic fields and display snapshot fields
--    from the legacy KRW-only columns when the table already exists.
-- 3. Keep the migration idempotent and safe to rerun.
--
-- This draft intentionally does not drop legacy KRW-only columns yet.

CREATE TABLE IF NOT EXISTS advance_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    workplace_id BIGINT NOT NULL REFERENCES workplaces (id),
    contract_id BIGINT NOT NULL REFERENCES work_contracts (id),
    year_month VARCHAR(7) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    asset_symbol VARCHAR(20) NOT NULL,
    asset_decimals INTEGER NOT NULL,
    reference_exchange_rate NUMERIC(12, 2) NOT NULL,
    requested_amount_atomic BIGINT NOT NULL,
    requested_reference_krw BIGINT NOT NULL,
    approved_amount_atomic BIGINT,
    approved_reference_krw BIGINT,
    fee_amount_atomic BIGINT NOT NULL,
    fee_reference_krw BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    repayment_due_date DATE NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    reviewed_by_account_id BIGINT,
    reviewed_at TIMESTAMP,
    snapshot_available_amount_atomic BIGINT NOT NULL,
    snapshot_available_reference_krw BIGINT NOT NULL,
    snapshot_max_cap_amount_atomic BIGINT NOT NULL,
    snapshot_max_cap_reference_krw BIGINT NOT NULL,
    snapshot_policy_rate NUMERIC(5, 2) NOT NULL,
    snapshot_reflected_work_days INTEGER NOT NULL,
    snapshot_reflected_work_minutes BIGINT NOT NULL,
    snapshot_needs_review_record_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS asset_symbol VARCHAR(20);

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS asset_decimals INTEGER;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS reference_exchange_rate NUMERIC(12, 2);

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS requested_amount_atomic BIGINT;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS requested_reference_krw BIGINT;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS approved_amount_atomic BIGINT;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS approved_reference_krw BIGINT;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS fee_amount_atomic BIGINT;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS fee_reference_krw BIGINT;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS snapshot_available_amount_atomic BIGINT;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS snapshot_available_reference_krw BIGINT;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS snapshot_max_cap_amount_atomic BIGINT;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS snapshot_max_cap_reference_krw BIGINT;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS snapshot_policy_rate NUMERIC(5, 2);

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS snapshot_reflected_work_days INTEGER;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS snapshot_reflected_work_minutes BIGINT;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS snapshot_needs_review_record_count INTEGER;

ALTER TABLE advance_requests
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

UPDATE advance_requests
SET asset_symbol = COALESCE(asset_symbol, 'dUSDC'),
    asset_decimals = COALESCE(asset_decimals, 6),
    reference_exchange_rate = COALESCE(reference_exchange_rate, 1450.00)
WHERE asset_symbol IS NULL
   OR asset_decimals IS NULL
   OR reference_exchange_rate IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'advance_requests'
          AND column_name = 'requested_amount'
    ) THEN
        EXECUTE $legacy$
            UPDATE advance_requests
            SET requested_amount_atomic = COALESCE(
                    requested_amount_atomic,
                    CASE
                        WHEN requested_amount IS NULL OR requested_amount <= 0 THEN 0
                        ELSE FLOOR((requested_amount::numeric * 1000000) / 1450)::bigint
                    END
                ),
                requested_reference_krw = COALESCE(requested_reference_krw, requested_amount)
            WHERE requested_amount_atomic IS NULL
               OR requested_reference_krw IS NULL
        $legacy$;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'advance_requests'
          AND column_name = 'approved_amount'
    ) THEN
        EXECUTE $legacy$
            UPDATE advance_requests
            SET approved_amount_atomic = CASE
                    WHEN approved_amount_atomic IS NOT NULL THEN approved_amount_atomic
                    WHEN approved_amount IS NULL OR approved_amount <= 0 THEN NULL
                    ELSE FLOOR((approved_amount::numeric * 1000000) / 1450)::bigint
                END,
                approved_reference_krw = CASE
                    WHEN approved_reference_krw IS NOT NULL THEN approved_reference_krw
                    WHEN approved_amount IS NULL OR approved_amount <= 0 THEN NULL
                    ELSE approved_amount
                END
            WHERE approved_amount_atomic IS NULL
               OR approved_reference_krw IS NULL
        $legacy$;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'advance_requests'
          AND column_name = 'fee_amount'
    ) THEN
        EXECUTE $legacy$
            UPDATE advance_requests
            SET fee_amount_atomic = COALESCE(
                    fee_amount_atomic,
                    CASE
                        WHEN fee_amount IS NULL OR fee_amount <= 0 THEN 0
                        ELSE FLOOR((fee_amount::numeric * 1000000) / 1450)::bigint
                    END
                ),
                fee_reference_krw = COALESCE(fee_reference_krw, fee_amount)
            WHERE fee_amount_atomic IS NULL
               OR fee_reference_krw IS NULL
        $legacy$;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'advance_requests'
          AND column_name = 'snapshot_available_amount'
    ) THEN
        EXECUTE $legacy$
            UPDATE advance_requests
            SET snapshot_available_amount_atomic = COALESCE(
                    snapshot_available_amount_atomic,
                    CASE
                        WHEN snapshot_available_amount IS NULL OR snapshot_available_amount <= 0 THEN 0
                        ELSE FLOOR((snapshot_available_amount::numeric * 1000000) / 1450)::bigint
                    END
                ),
                snapshot_available_reference_krw = COALESCE(snapshot_available_reference_krw, snapshot_available_amount)
            WHERE snapshot_available_amount_atomic IS NULL
               OR snapshot_available_reference_krw IS NULL
        $legacy$;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'advance_requests'
          AND column_name = 'snapshot_max_cap'
    ) THEN
        EXECUTE $legacy$
            UPDATE advance_requests
            SET snapshot_max_cap_amount_atomic = COALESCE(
                    snapshot_max_cap_amount_atomic,
                    CASE
                        WHEN snapshot_max_cap IS NULL OR snapshot_max_cap <= 0 THEN 0
                        ELSE FLOOR((snapshot_max_cap::numeric * 1000000) / 1450)::bigint
                    END
                ),
                snapshot_max_cap_reference_krw = COALESCE(snapshot_max_cap_reference_krw, snapshot_max_cap)
            WHERE snapshot_max_cap_amount_atomic IS NULL
               OR snapshot_max_cap_reference_krw IS NULL
        $legacy$;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'advance_requests'
          AND column_name = 'approved_amount'
    ) THEN
        ALTER TABLE advance_requests
            ALTER COLUMN approved_amount DROP NOT NULL;
    END IF;
END $$;

UPDATE advance_requests
SET fee_amount_atomic = COALESCE(fee_amount_atomic, 0),
    fee_reference_krw = COALESCE(fee_reference_krw, 0),
    snapshot_available_amount_atomic = COALESCE(snapshot_available_amount_atomic, 0),
    snapshot_available_reference_krw = COALESCE(snapshot_available_reference_krw, 0),
    snapshot_max_cap_amount_atomic = COALESCE(snapshot_max_cap_amount_atomic, 0),
    snapshot_max_cap_reference_krw = COALESCE(snapshot_max_cap_reference_krw, 0),
    snapshot_policy_rate = COALESCE(snapshot_policy_rate, 0),
    snapshot_reflected_work_days = COALESCE(snapshot_reflected_work_days, 0),
    snapshot_reflected_work_minutes = COALESCE(snapshot_reflected_work_minutes, 0),
    snapshot_needs_review_record_count = COALESCE(snapshot_needs_review_record_count, 0),
    created_at = COALESCE(created_at, requested_at, NOW())
WHERE fee_amount_atomic IS NULL
   OR fee_reference_krw IS NULL
   OR snapshot_available_amount_atomic IS NULL
   OR snapshot_available_reference_krw IS NULL
   OR snapshot_max_cap_amount_atomic IS NULL
   OR snapshot_max_cap_reference_krw IS NULL
   OR snapshot_policy_rate IS NULL
   OR snapshot_reflected_work_days IS NULL
   OR snapshot_reflected_work_minutes IS NULL
   OR snapshot_needs_review_record_count IS NULL
   OR created_at IS NULL;

ALTER TABLE advance_requests
    ALTER COLUMN asset_symbol SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN asset_decimals SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN reference_exchange_rate SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN requested_amount_atomic SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN requested_reference_krw SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN fee_amount_atomic SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN fee_reference_krw SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN snapshot_available_amount_atomic SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN snapshot_available_reference_krw SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN snapshot_max_cap_amount_atomic SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN snapshot_max_cap_reference_krw SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN snapshot_policy_rate SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN snapshot_reflected_work_days SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN snapshot_reflected_work_minutes SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN snapshot_needs_review_record_count SET NOT NULL;

ALTER TABLE advance_requests
    ALTER COLUMN created_at SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_advance_requests_user_idempotency'
    ) THEN
        ALTER TABLE advance_requests
            ADD CONSTRAINT uk_advance_requests_user_idempotency
            UNIQUE (user_id, idempotency_key);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_advance_requests_user_year_month_requested_at
    ON advance_requests (user_id, year_month, requested_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_advance_requests_user_workplace_cycle_status
    ON advance_requests (user_id, workplace_id, year_month, status);

CREATE INDEX IF NOT EXISTS idx_advance_requests_requested_at
    ON advance_requests (requested_at DESC, created_at DESC, id DESC);
