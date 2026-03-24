package com.workproofpay.backend.employer.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.workproofpay.backend.employer.model.AttendanceOvertimeRoundingUnit;
import com.workproofpay.backend.employer.service.EmployerAccessScope;
import com.workproofpay.backend.workproof.model.Workplace;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.time.LocalDateTime;

public record EmployerWorkplaceSettingsResponse(
        @Schema(description = "Workplace ID", example = "5")
        Long workplaceId,
        @Schema(description = "Workplace name", example = "Seoul Hub")
        String workplaceName,
        @Schema(description = "Company ID", example = "1")
        Long companyId,
        @Schema(description = "Company name", example = "Acme Logistics")
        String companyName,
        @Schema(description = "Workplace address", example = "서울특별시 강남구 테헤란로 212")
        String address,
        @Schema(description = "Display-only detail address memo", example = "1층 정문 앞")
        String detailAddress,
        @Schema(description = "Latitude", example = "37.501274")
        Double latitude,
        @Schema(description = "Longitude", example = "127.039585")
        Double longitude,
        @Schema(description = "Allowed radius in meters", example = "300")
        Integer allowedRadiusMeters,
        @Schema(description = "Company-wide scheduled clock-in time", example = "09:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime scheduledClockInTime,
        @Schema(description = "Company-wide scheduled clock-out time", example = "18:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime scheduledClockOutTime,
        @Schema(description = "Company-wide overtime rounding unit", example = "FIFTEEN_MINUTES")
        AttendanceOvertimeRoundingUnit overtimeRoundingUnit,
        @Schema(description = "Server-side effective timestamp for the latest settings update")
        LocalDateTime effectiveFrom,
        @Schema(description = "Last updated timestamp")
        LocalDateTime updatedAt,
        @Schema(description = "Account ID that last updated the settings", example = "9")
        Long updatedByAccountId,
        @Schema(description = "Active memberships currently scoped to this workplace", example = "12")
        long activeMembershipCount
) {
    private static final int DEFAULT_ALLOWED_RADIUS_METERS = 1_000;

    public static EmployerWorkplaceSettingsResponse from(EmployerAccessScope scope,
                                                         Workplace workplace,
                                                         long activeMembershipCount) {
        return new EmployerWorkplaceSettingsResponse(
                workplace.getId(),
                workplace.getName(),
                scope.companyId(),
                scope.companyName(),
                workplace.getAddress(),
                workplace.resolveDetailAddress(),
                workplace.getLatitude(),
                workplace.getLongitude(),
                workplace.resolveAllowedRadiusMeters(DEFAULT_ALLOWED_RADIUS_METERS),
                scope.scheduledClockInTime(),
                scope.scheduledClockOutTime(),
                scope.overtimeRoundingUnit(),
                workplace.resolveSettingsEffectiveFrom(),
                workplace.getUpdatedAt(),
                workplace.getSettingsUpdatedByAccountId(),
                activeMembershipCount
        );
    }
}
