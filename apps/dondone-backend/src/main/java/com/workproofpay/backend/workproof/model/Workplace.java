package com.workproofpay.backend.workproof.model;

import com.workproofpay.backend.auth.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workplaces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/**
 * WorkProof lane 1에서 출퇴근과 계약의 기준축이 되는 사용자 소속 근무지다.
 */
public class Workplace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "map_label", length = 100)
    private String mapLabel;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "allowed_radius_meters")
    private Integer allowedRadiusMeters;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Workplace(User user,
                      String name,
                      String address,
                      String mapLabel,
                      Double latitude,
                      Double longitude,
                      Integer allowedRadiusMeters) {
        this.user = user;
        this.name = name;
        this.address = address;
        this.mapLabel = mapLabel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadiusMeters = allowedRadiusMeters;
    }

    public static Workplace create(User user,
                                   String name,
                                   String address,
                                   String mapLabel,
                                   Double latitude,
                                   Double longitude,
                                   Integer allowedRadiusMeters) {
        return new Workplace(user, name, address, mapLabel, latitude, longitude, allowedRadiusMeters);
    }

    // 기존 row에는 반경 값이 없을 수 있어서 lane 1 기본 반경을 fallback으로 쓴다.
    public int resolveAllowedRadiusMeters(int defaultAllowedRadiusMeters) {
        return allowedRadiusMeters == null ? defaultAllowedRadiusMeters : allowedRadiusMeters;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
