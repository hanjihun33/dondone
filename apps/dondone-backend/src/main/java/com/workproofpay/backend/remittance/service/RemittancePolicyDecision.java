package com.workproofpay.backend.remittance.service;

import com.workproofpay.backend.remittance.adapter.ChainBalanceSnapshot;
import com.workproofpay.backend.remittance.model.Recipient;
import com.workproofpay.backend.remittance.model.RemittancePolicyCode;
import com.workproofpay.backend.remittance.model.UserWallet;

record RemittancePolicyDecision(
        boolean allowed,
        RemittancePolicyCode policyCode,
        Recipient recipient,
        UserWallet wallet,
        ChainBalanceSnapshot balanceSnapshot,
        boolean recentRecipientConfirmationRequired
) {
}
