package com.workproofpay.backend.wage.repo;

import com.workproofpay.backend.wage.model.WageDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

public interface WageDepositRepository extends JpaRepository<WageDeposit, Long> {

    List<WageDeposit> findByUserIdAndYearMonthOrderByDepositDateDescCreatedAtDesc(Long userId, String yearMonth);

    Optional<WageDeposit> findFirstByUserIdAndYearMonthOrderByDepositDateDescCreatedAtDesc(Long userId, String yearMonth);

    Optional<WageDeposit> findFirstByUserIdAndYearMonthAndDepositDateLessThanEqualOrderByDepositDateDescCreatedAtDesc(
            Long userId,
            String yearMonth,
            LocalDate asOf
    );
}
