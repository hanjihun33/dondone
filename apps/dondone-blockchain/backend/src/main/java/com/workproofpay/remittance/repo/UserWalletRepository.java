package com.workproofpay.remittance.repo;

import com.workproofpay.remittance.domain.UserWalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWalletRepository extends JpaRepository<UserWalletEntity, String> {
}
