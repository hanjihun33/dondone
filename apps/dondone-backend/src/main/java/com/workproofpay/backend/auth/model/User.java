package com.workproofpay.backend.auth.model;

import com.workproofpay.backend.auth.support.EmailNormalizer;
import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import jakarta.persistence.*;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    private User(String email, String passwordHash, String name, String phoneNumber, UserRole role) {
        this.email = EmailNormalizer.normalize(email);
        this.passwordHash = passwordHash;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }


    public static User register(String email, String encodedPassword, String name) {
        return register(email, encodedPassword, name, null);
    }

    public static User register(String email, String encodedPassword, String name, String phoneNumber) {
        return new User(email, encodedPassword, name, phoneNumber, UserRole.USER);
    }

    public void updateProfile(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public static User registerEmployer(String email, String encodedPassword, String name) {
        return new User(email, encodedPassword, name, null, UserRole.EMPLOYER);
    }   

}
