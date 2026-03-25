package com.workproofpay.backend.workproof.service;

import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.Workplace;

import java.time.LocalDate;

public interface WorkContractProvisioner {

    WorkContract ensureActiveContract(Workplace workplace, LocalDate effectiveFrom);
}
