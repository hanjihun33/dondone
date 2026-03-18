package com.workproofpay.remittance.repo;

import com.workproofpay.remittance.domain.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {
}
