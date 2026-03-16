package com.workproofpay.backend.wage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * verification 시점에 계산된 원인 설명을 그대로 보존한다.
 * 후속 계산 규칙이 바뀌어도 Documents/Claim는 당시 설명을 그대로 재사용할 수 있다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WageVerificationPossibleCauseSnapshot {

    @Column(name = "cause_code", nullable = false, length = 100)
    private String code;

    @Column(name = "cause_title", nullable = false, length = 200)
    private String title;

    @Column(name = "cause_detail", nullable = false, length = 500)
    private String detail;

    private WageVerificationPossibleCauseSnapshot(String code, String title, String detail) {
        this.code = code;
        this.title = title;
        this.detail = detail;
    }

    public static WageVerificationPossibleCauseSnapshot of(String code, String title, String detail) {
        return new WageVerificationPossibleCauseSnapshot(code, title, detail);
    }
}
