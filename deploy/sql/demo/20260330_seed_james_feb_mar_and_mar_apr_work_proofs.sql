DO $$
DECLARE
    v_user_email TEXT;
    v_user_id BIGINT;

    v_workplace_id BIGINT;
    v_contract_id BIGINT;
    v_workplace_name TEXT;
    v_workplace_address TEXT;
    v_workplace_map_label TEXT;
    v_workplace_latitude DOUBLE PRECISION;
    v_workplace_longitude DOUBLE PRECISION;
BEGIN
    FOREACH v_user_email IN ARRAY ARRAY[
        'james4@gmail.com',
        'james5@gmail.com',
        'james6@gmail.com',
        'james7@gmail.com',
        'james8@gmail.com',
        'james9@gmail.com'
    ]
    LOOP
        v_user_id := NULL;
        v_workplace_id := NULL;
        v_contract_id := NULL;

        SELECT u.id
          INTO v_user_id
          FROM users u
         WHERE lower(u.email) = lower(v_user_email)
         LIMIT 1;

        IF v_user_id IS NULL THEN
            RAISE NOTICE 'Skipping james work proof seed: user not found for email=%', v_user_email;
            CONTINUE;
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
            RAISE NOTICE 'Skipping james work proof seed: active employment membership not found for user_id=% email=%', v_user_id, v_user_email;
            CONTINUE;
        END IF;

        SELECT wc.id
          INTO v_contract_id
          FROM work_contracts wc
         WHERE wc.workplace_id = v_workplace_id
           AND wc.effective_to IS NULL
         ORDER BY wc.effective_from DESC, wc.id DESC
         LIMIT 1;

        IF v_contract_id IS NULL THEN
            RAISE NOTICE 'Skipping james work proof seed: active work contract not found for workplace_id=% email=%', v_workplace_id, v_user_email;
            CONTINUE;
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
            item.memo,
            item.edit_reason,
            item.attachment_count,
            NULL,
            'REFLECTED',
            item.clock_out_at,
            item.clock_out_at
        FROM (
            VALUES
                -- 2026-02 ~ 2026-03 cycle: 17 reflected records, <= 3 modified in 2026-02
                (DATE '2026-02-02', TIMESTAMP '2026-02-02 09:00:00', TIMESTAMP '2026-02-02 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-03', TIMESTAMP '2026-02-03 09:00:00', TIMESTAMP '2026-02-03 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-04', TIMESTAMP '2026-02-04 09:00:00', TIMESTAMP '2026-02-04 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-05', TIMESTAMP '2026-02-05 09:00:00', TIMESTAMP '2026-02-05 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-06', TIMESTAMP '2026-02-06 09:00:00', TIMESTAMP '2026-02-06 17:00:00', 'james 2026-02~03 cycle reflected work proof', '증빙 보완으로 출근 기록을 수정했어요.', 1),
                (DATE '2026-02-09', TIMESTAMP '2026-02-09 09:00:00', TIMESTAMP '2026-02-09 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-10', TIMESTAMP '2026-02-10 09:00:00', TIMESTAMP '2026-02-10 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-11', TIMESTAMP '2026-02-11 09:00:00', TIMESTAMP '2026-02-11 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-12', TIMESTAMP '2026-02-12 09:00:00', TIMESTAMP '2026-02-12 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-13', TIMESTAMP '2026-02-13 09:00:00', TIMESTAMP '2026-02-13 17:00:00', 'james 2026-02~03 cycle reflected work proof', '증빙 보완으로 퇴근 기록을 수정했어요.', 1),
                (DATE '2026-02-16', TIMESTAMP '2026-02-16 09:00:00', TIMESTAMP '2026-02-16 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-17', TIMESTAMP '2026-02-17 09:00:00', TIMESTAMP '2026-02-17 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-18', TIMESTAMP '2026-02-18 09:00:00', TIMESTAMP '2026-02-18 17:00:00', 'james 2026-02~03 cycle reflected work proof', '근무 시간 확인을 위해 수정 요청이 반영됐어요.', 1),
                (DATE '2026-02-19', TIMESTAMP '2026-02-19 09:00:00', TIMESTAMP '2026-02-19 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-20', TIMESTAMP '2026-02-20 09:00:00', TIMESTAMP '2026-02-20 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-23', TIMESTAMP '2026-02-23 09:00:00', TIMESTAMP '2026-02-23 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),
                (DATE '2026-02-24', TIMESTAMP '2026-02-24 09:00:00', TIMESTAMP '2026-02-24 17:00:00', 'james 2026-02~03 cycle reflected work proof', NULL, 0),

                -- 2026-03 ~ 2026-04 cycle: 17 reflected records, <= 3 modified in 2026-03, only dates before 2026-03-29
                (DATE '2026-03-02', TIMESTAMP '2026-03-02 09:00:00', TIMESTAMP '2026-03-02 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-03', TIMESTAMP '2026-03-03 09:00:00', TIMESTAMP '2026-03-03 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-04', TIMESTAMP '2026-03-04 09:00:00', TIMESTAMP '2026-03-04 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-05', TIMESTAMP '2026-03-05 09:00:00', TIMESTAMP '2026-03-05 17:00:00', 'james 2026-03~04 cycle reflected work proof', '출근 기록을 증빙 기준으로 수정했어요.', 1),
                (DATE '2026-03-06', TIMESTAMP '2026-03-06 09:00:00', TIMESTAMP '2026-03-06 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-09', TIMESTAMP '2026-03-09 09:00:00', TIMESTAMP '2026-03-09 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-10', TIMESTAMP '2026-03-10 09:00:00', TIMESTAMP '2026-03-10 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-11', TIMESTAMP '2026-03-11 09:00:00', TIMESTAMP '2026-03-11 17:00:00', 'james 2026-03~04 cycle reflected work proof', '근무 시간 확인을 위해 수정 요청이 반영됐어요.', 1),
                (DATE '2026-03-12', TIMESTAMP '2026-03-12 09:00:00', TIMESTAMP '2026-03-12 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-13', TIMESTAMP '2026-03-13 09:00:00', TIMESTAMP '2026-03-13 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-16', TIMESTAMP '2026-03-16 09:00:00', TIMESTAMP '2026-03-16 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-17', TIMESTAMP '2026-03-17 09:00:00', TIMESTAMP '2026-03-17 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-18', TIMESTAMP '2026-03-18 09:00:00', TIMESTAMP '2026-03-18 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-19', TIMESTAMP '2026-03-19 09:00:00', TIMESTAMP '2026-03-19 17:00:00', 'james 2026-03~04 cycle reflected work proof', '퇴근 기록을 증빙 기준으로 수정했어요.', 1),
                (DATE '2026-03-20', TIMESTAMP '2026-03-20 09:00:00', TIMESTAMP '2026-03-20 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-23', TIMESTAMP '2026-03-23 09:00:00', TIMESTAMP '2026-03-23 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0),
                (DATE '2026-03-24', TIMESTAMP '2026-03-24 09:00:00', TIMESTAMP '2026-03-24 17:00:00', 'james 2026-03~04 cycle reflected work proof', NULL, 0)
        ) AS item(work_date, clock_in_at, clock_out_at, memo, edit_reason, attachment_count)
        WHERE NOT EXISTS (
            SELECT 1
              FROM work_proofs wp
             WHERE wp.user_id = v_user_id
               AND wp.work_date = item.work_date
        );

        RAISE NOTICE 'Inserted james reflected work proofs for email=% cycles 2026-02~03 (17 days) and 2026-03~04 (17 days before 2026-03-29).', v_user_email;
    END LOOP;
END $$;
