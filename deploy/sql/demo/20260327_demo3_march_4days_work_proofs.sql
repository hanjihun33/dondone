DO $$
DECLARE
    v_user_id BIGINT := 98;
    v_user_email TEXT := 'demo3@test.com';
    v_user_name TEXT := 'jaja';

    v_workplace_id BIGINT;
    v_contract_id BIGINT;
    v_workplace_name TEXT;
    v_workplace_address TEXT;
    v_workplace_map_label TEXT;
    v_workplace_latitude DOUBLE PRECISION;
    v_workplace_longitude DOUBLE PRECISION;
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM users u
        WHERE u.id = v_user_id
          AND lower(u.email) = lower(v_user_email)
          AND u.name = v_user_name
    ) THEN
        RAISE NOTICE 'Skipping demo3 reflected work proof seed: target user mismatch (id=%, email=%, name=%)', v_user_id, v_user_email, v_user_name;
        RETURN;
    END IF;

    SELECT em.workplace_id
      INTO v_workplace_id
      FROM employment_memberships em
     WHERE em.worker_account_id = v_user_id
       AND em.status = 'ACTIVE'
       AND em.effective_to IS NULL
     ORDER BY em.effective_from DESC, em.id DESC
     LIMIT 1;

    IF v_workplace_id IS NULL THEN
        RAISE NOTICE 'Skipping demo3 reflected work proof seed: active employment membership not found for user_id=%', v_user_id;
        RETURN;
    END IF;

    SELECT wc.id
      INTO v_contract_id
      FROM work_contracts wc
     WHERE wc.workplace_id = v_workplace_id
       AND wc.effective_to IS NULL
     ORDER BY wc.effective_from DESC, wc.id DESC
     LIMIT 1;

    IF v_contract_id IS NULL THEN
        RAISE NOTICE 'Skipping demo3 reflected work proof seed: active work contract not found for workplace_id=%', v_workplace_id;
        RETURN;
    END IF;

    SELECT w.name,
           w.address,
           w.map_label,
           w.latitude,
           w.longitude
      INTO v_workplace_name,
           v_workplace_address,
           v_workplace_map_label,
           v_workplace_latitude,
           v_workplace_longitude
      FROM workplaces w
     WHERE w.id = v_workplace_id;

    INSERT INTO work_proofs (
        user_id,
        workplace_id,
        workplace_name_snapshot,
        workplace_address_snapshot,
        workplace_map_label_snapshot,
        workplace_latitude_snapshot,
        workplace_longitude_snapshot,
        contract_id,
        work_date,
        clock_in_at,
        clock_out_at,
        recognized_clock_in_at,
        recognized_clock_out_at,
        device_clock_in_at,
        device_clock_out_at,
        server_clock_in_at,
        server_clock_out_at,
        clock_in_latitude,
        clock_in_longitude,
        clock_out_latitude,
        clock_out_longitude,
        clock_in_location_label,
        clock_out_location_label,
        clock_out_outside_allowed_radius,
        memo,
        edit_reason,
        attachment_count,
        attachment_metadata_json,
        financial_status,
        created_at,
        updated_at
    )
    SELECT
        v_user_id,
        v_workplace_id,
        v_workplace_name,
        v_workplace_address,
        v_workplace_map_label,
        v_workplace_latitude,
        v_workplace_longitude,
        v_contract_id,
        item.work_date,
        item.clock_in_at,
        item.clock_out_at,
        item.clock_in_at,
        item.clock_out_at,
        item.clock_in_at,
        item.clock_out_at,
        item.clock_in_at,
        item.clock_out_at,
        v_workplace_latitude,
        v_workplace_longitude,
        v_workplace_latitude,
        v_workplace_longitude,
        COALESCE(v_workplace_map_label, v_workplace_name),
        COALESCE(v_workplace_map_label, v_workplace_name),
        FALSE,
        'demo3 3월 4일 reflected 근무 기록',
        NULL,
        0,
        NULL,
        'REFLECTED',
        item.clock_out_at,
        item.clock_out_at
    FROM (
        VALUES
            (DATE '2026-03-17', TIMESTAMP '2026-03-17 09:00:00', TIMESTAMP '2026-03-17 18:00:00'),
            (DATE '2026-03-18', TIMESTAMP '2026-03-18 09:00:00', TIMESTAMP '2026-03-18 18:00:00'),
            (DATE '2026-03-19', TIMESTAMP '2026-03-19 09:00:00', TIMESTAMP '2026-03-19 18:00:00'),
            (DATE '2026-03-20', TIMESTAMP '2026-03-20 09:00:00', TIMESTAMP '2026-03-20 18:00:00')
    ) AS item(work_date, clock_in_at, clock_out_at)
    WHERE NOT EXISTS (
        SELECT 1
        FROM work_proofs wp
        WHERE wp.user_id = v_user_id
          AND wp.work_date = item.work_date
    );
END $$;
