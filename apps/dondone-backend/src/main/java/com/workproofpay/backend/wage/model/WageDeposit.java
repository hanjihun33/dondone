package com.workproofpay.backend.wage.model;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "wage_deposits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WageDeposit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "deposit_date", nullable = false)
    private LocalDate depositDate;

    @Column(name = "actual_deposit_amount", nullable = false)
    private Long actualDepositAmount;

    @Column(name = "deductions_known", nullable = false)
    private boolean deductionsKnown;

    @Column(length = 500)
    private String note;

    private WageDeposit(User user, String yearMonth, LocalDate depositDate, Long actualDepositAmount, boolean deductionsKnown, String note) {
        this.user = user;
        this.yearMonth = yearMonth;
        this.depositDate = depositDate;
        this.actualDepositAmount = actualDepositAmount;
        this.deductionsKnown = deductionsKnown;
        this.note = note;
    }

    public static WageDeposit record(User user,
                                     String yearMonth,
                                     LocalDate depositDate,
                                     Long actualDepositAmount,
                                     boolean deductionsKnown,
                                     String note) {
        return new WageDeposit(user, yearMonth, depositDate, actualDepositAmount, deductionsKnown, note);
    }
}
