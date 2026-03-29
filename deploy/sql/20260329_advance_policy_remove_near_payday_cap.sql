UPDATE advance_policies
SET reduced_cap_days_before_payday = 0,
    near_payday_max_cap_display_krw_amount = max_cap_display_krw_amount,
    updated_at = NOW()
WHERE reduced_cap_days_before_payday <> 0
   OR near_payday_max_cap_display_krw_amount <> max_cap_display_krw_amount;
