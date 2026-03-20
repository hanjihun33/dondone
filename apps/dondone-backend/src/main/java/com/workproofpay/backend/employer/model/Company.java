package com.workproofpay.backend.employer.model;

import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "companies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "company_code", nullable = false, unique = true, length = 50)
    private String companyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyStatus status;

    private Company(String name, String companyCode, CompanyStatus status) {
        this.name = name;
        this.companyCode = companyCode;
        this.status = status;
    }

    public static Company create(String name, String companyCode) {
        return new Company(name, companyCode, CompanyStatus.ACTIVE);
    }
}
