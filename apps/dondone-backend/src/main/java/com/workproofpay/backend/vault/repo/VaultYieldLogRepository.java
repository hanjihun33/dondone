package com.workproofpay.backend.vault.repo;

import com.workproofpay.backend.vault.model.VaultYieldLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaultYieldLogRepository extends JpaRepository<VaultYieldLog, Long> {
}
