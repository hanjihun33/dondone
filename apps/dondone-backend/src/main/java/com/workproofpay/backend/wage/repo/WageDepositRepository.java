package com.workproofpay.backend.wage.repo;

import com.workproofpay.backend.wage.model.WageDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WageDepositRepository extends JpaRepository<WageDeposit, Long> {

    List<WageDeposit> findByUserIdAndYearMonthOrderByDepositDateDescCreatedAtDesc(Long userId, String yearMonth);
}
