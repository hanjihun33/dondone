UPDATE advance_policies
SET payday_day = 31,
    updated_at = NOW()
WHERE payday_day <> 31;
