package com.workproofpay.backend.auth.model;

import com.workproofpay.backend.auth.support.EmailNormalizer;
import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import com.workproofpay.backend.shared.util.CompanyCodeUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "company_code", length = 50)
    private String companyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    private User(
            String email,
            String passwordHash,
            String name,
            String phoneNumber,
            String companyCode,
            UserRole role
    ) {
        this.email = EmailNormalizer.normalize(email);
        this.passwordHash = passwordHash;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.companyCode = companyCode;
        this.role = role;
    }

    public static User register(String email, String encodedPassword, String name) {
        return register(email, encodedPassword, name, null, CompanyCodeUtils.DEFAULT_COMPANY_CODE);
    }

    public static User register(String email, String encodedPassword, String name, String phoneNumber) {
        return register(email, encodedPassword, name, phoneNumber, CompanyCodeUtils.DEFAULT_COMPANY_CODE);
    }

    public static User register(
            String email,
            String encodedPassword,
            String name,
            String phoneNumber,
            String companyCode
    ) {
        return new User(
                email,
                encodedPassword,
                name,
                phoneNumber,
                CompanyCodeUtils.normalizeNullableOrThrow(companyCode),
                UserRole.USER
        );
    }

    public static User registerEmployer(String email, String encodedPassword, String name) {
        return new User(email, encodedPassword, name, null, null, UserRole.EMPLOYER);
    }

    public static User registerAdmin(String email, String encodedPassword, String name) {
        return new User(email, encodedPassword, name, null, null, UserRole.ADMIN);
    }

    public void updateProfile(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public void updateCompanyCode(String companyCode) {
        this.companyCode = CompanyCodeUtils.normalizeOrThrow(companyCode);
    }
}
