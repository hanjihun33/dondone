-- Follow-up cleanup for legacy KRW columns that are no longer written by the
-- dUSDC-based advance request flow.
--
-- The current application persists only:
-- - *_amount_atomic
-- - *_reference_krw
-- - asset_symbol / asset_decimals / reference_exchange_rate
--
-- Legacy KRW-only columns may still exist in operational databases. Their
-- NOT NULL constraints must be relaxed so SUBMITTED requests can be inserted
-- without populating obsolete columns.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'advance_requests'
          AND column_name = 'requested_amount'
    ) THEN
        ALTER TABLE advance_requests
            ALTER COLUMN requested_amount DROP NOT NULL;
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

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'advance_requests'
          AND column_name = 'fee_amount'
    ) THEN
        ALTER TABLE advance_requests
            ALTER COLUMN fee_amount DROP NOT NULL;
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
        ALTER TABLE advance_requests
            ALTER COLUMN snapshot_available_amount DROP NOT NULL;
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
        ALTER TABLE advance_requests
            ALTER COLUMN snapshot_max_cap DROP NOT NULL;
    END IF;
END $$;
