package com.workproofpay.backend.workproof.model;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
public class Workplace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_id")
    private Long companyId;

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

    @Column(name = "settings_effective_from")
    private LocalDateTime settingsEffectiveFrom;

    @Column(name = "settings_updated_by_account_id")
    private Long settingsUpdatedByAccountId;

    private Workplace(User user,
                      Long companyId,
                      String name,
                      String address,
                      String mapLabel,
                      Double latitude,
                      Double longitude,
                      Integer allowedRadiusMeters,
                      LocalDateTime settingsEffectiveFrom,
                      Long settingsUpdatedByAccountId) {
        this.user = user;
        this.companyId = companyId;
        this.name = name;
        this.address = address;
        this.mapLabel = mapLabel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadiusMeters = allowedRadiusMeters;
        this.settingsEffectiveFrom = settingsEffectiveFrom;
        this.settingsUpdatedByAccountId = settingsUpdatedByAccountId;
    }

    public static Workplace create(User user,
                                   String name,
                                   String address,
                                   String mapLabel,
                                   Double latitude,
                                   Double longitude,
                                   Integer allowedRadiusMeters) {
        return new Workplace(user, null, name, address, mapLabel, latitude, longitude, allowedRadiusMeters, null, null);
    }

    public static Workplace create(User user,
                                   Long companyId,
                                   String name,
                                   String address,
                                   String mapLabel,
                                   Double latitude,
                                   Double longitude,
                                   Integer allowedRadiusMeters) {
        return new Workplace(user, companyId, name, address, mapLabel, latitude, longitude, allowedRadiusMeters, null, null);
    }

    // 기존 row에는 반경 값이 없을 수 있어서 lane 1 기본 반경을 fallback으로 쓴다.
    public int resolveAllowedRadiusMeters(int defaultAllowedRadiusMeters) {
        return allowedRadiusMeters == null ? defaultAllowedRadiusMeters : allowedRadiusMeters;
    }

    public void updateEmployerSettings(String address,
                                       String detailAddress,
                                       Double latitude,
                                       Double longitude,
                                       Integer allowedRadiusMeters,
                                       LocalDateTime effectiveFrom,
                                       Long updatedByAccountId) {
        this.address = address.trim();
        this.mapLabel = normalizeOptional(detailAddress);
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadiusMeters = allowedRadiusMeters;
        this.settingsEffectiveFrom = effectiveFrom;
        this.settingsUpdatedByAccountId = updatedByAccountId;
    }

    public String resolveDetailAddress() {
        return mapLabel;
    }

    public LocalDateTime resolveSettingsEffectiveFrom() {
        if (settingsEffectiveFrom != null) {
            return settingsEffectiveFrom;
        }
        if (getUpdatedAt() != null) {
            return getUpdatedAt();
        }
        return getCreatedAt();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
