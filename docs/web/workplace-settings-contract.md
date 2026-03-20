# Workplace Settings Contract

## 목적
- 고용주가 웹에서 수정하는 사업장 설정이 근로자 출퇴근 판정과 어떻게 연결되는지 고정한다.
- settings 화면은 단순 UI가 아니라 `Workplace`와 `WorkProof` 판정 규칙에 영향을 주는 command임을 명확히 한다.

## 현재 웹 화면 근거
- `apps/dondone-web/src/pages/settings/SettingsPage.tsx`
- `apps/dondone-web/src/pages/settings/components/KakaoWorkplaceMap.tsx`

## 현재 backend foundation 기준
- endpoint
  - `GET /api/employer/workplace-settings`
  - `PUT /api/employer/workplace-settings`
- authz source
  - settings 대상 사업장은 request 파라미터가 아니라 `EmployerAccessScope.defaultWorkplaceId`로 해석한다.
  - settings가 영향을 줄 근로자 범위는 `EmploymentMembership.companyId/workplaceId` 기준으로 해석한다.
- 저장 모델
  - `detailAddress`는 현재 `Workplace.mapLabel` 저장소를 재사용한다.
  - `effectiveFrom`는 `Workplace.settingsEffectiveFrom`에 저장하고, legacy row는 `updatedAt/createAt` fallback으로 읽는다.
  - `updatedBy`는 `Workplace.settingsUpdatedByAccountId`로 남긴다.
- additive 경계
  - 기존 `Workplace.user`, `WorkProof.user`는 worker legacy ownership 용도로 유지하고 settings authz source로 사용하지 않는다.

## 다루는 값
- `workplaceId`
- `address`
- `detailAddress`
- `latitude`
- `longitude`
- `allowedRadiusMeters`
- `effectiveFrom`

## 값의 의미
### address
- 지도 검색 결과 또는 기준 주소
- 사용자 표시와 설정 확인에 사용

### detailAddress
- 출입문, 층수, 메모 같은 추가 설명
- 기본적으로 표시용 필드로 본다.
- 판정 로직에 직접 사용하지 않는다.

### latitude / longitude
- 출퇴근 기준 중심 좌표

### allowedRadiusMeters
- 허용 반경
- check-in/check-out 위치와 중심 좌표의 거리 판정 기준

### effectiveFrom
- 새 설정이 언제부터 판정에 쓰이는지 나타내는 기준 시점
- MVP에서는 저장 시점의 `server time`을 기본 효력 시점으로 본다.

## 기본 계약 원칙
- settings 수정은 사업주 권한이 필요하다.
- 사업주는 임의 `companyId`, `workplaceId`, `effectiveFrom`를 요청으로 넘겨 scope를 넓힐 수 없다.
- 설정 변경은 최소한 `미래 기록`에 대한 기준을 바꾼다.
- 기존 기록에 소급 적용할지 여부는 별도 규칙으로 명시해야 한다.

## MVP 확정 규칙
### 효력 시점
- 새 settings는 저장이 완료된 이후의 새 `check-in`, `check-out`부터 적용한다.
- 같은 날이라도 저장 이전에 이미 완료된 기록은 기존 판정을 유지한다.
- check-in은 이전 설정으로 시작했고 check-out은 새 설정 이후에 발생한 경우:
  - `check-out` 위치 판정은 새 설정 기준을 적용한다.
  - 단, 과거 `check-in` 판정을 다시 계산하지는 않는다.

### 소급 적용
- 기존 완료된 `WorkProof`는 자동 재판정하지 않는다.
- 대시보드, 정정 요청, Wage 집계도 기존 완료 기록에 대해 자동 재계산하지 않는다.
- 필요하면 별도 관리자 재계산 작업으로 처리한다.

### 다중 사업장
- MVP에서는 `GET/PUT /api/employer/workplace-settings`가 기본 사업장 1건을 대상으로 동작해도 허용한다.
- 다중 workplace 지원은 도메인 모델이 안정된 뒤 확장한다.

## 이 규칙을 택한 이유
- 소급 재판정은 Wage, 정정 요청, 대시보드 집계에 연쇄 영향을 준다.
- MVP 단계에서 자동 재계산까지 같이 넣으면 리스크가 커진다.
- 현재 백엔드 구조상 과거 기록 재평가를 안전하게 수행하는 운영 도구가 없다.

## 조회/수정 API 초안
- `GET /api/employer/workplace-settings`
- `PUT /api/employer/workplace-settings`

## 현재 응답/요청 계약
### GET response
- `workplaceId`
- `workplaceName`
- `companyId`
- `companyName`
- `address`
- `detailAddress`
- `latitude`
- `longitude`
- `allowedRadiusMeters`
- `effectiveFrom`
- `updatedAt`
- `updatedByAccountId`
- `activeMembershipCount`

### PUT request
- `address`
- `detailAddress`
- `latitude`
- `longitude`
- `allowedRadiusMeters`

### PUT에서 받지 않는 값
- `companyId`
- `workplaceId`
- `effectiveFrom`
- worker 목록 또는 기존 `WorkProof` 재계산 대상

## 수정 시 검증
- 사업주가 해당 workplace를 관리하는지
- 반경이 허용 범위 안인지
- 좌표가 유효한지
- detailAddress가 지나치게 길지 않은지
- 같은 company 범위 안의 workplace인지

## 응답/저장 시 같이 남길 값
- `updatedAt`
- `updatedByAccountId`
- `effectiveFrom`
- 필요 시 이전 설정 요약

## WorkProof와의 연결 포인트
- 새 check-in 시 사용되는 중심 좌표
- 새 check-out 시 반경 이탈 판정
- `needsReview` 또는 유사 상태 판정 기준
- 미래 근로기록에만 영향을 주는지 여부
- `PUT /api/employer/workplace-settings`는 기존 완료 `WorkProof`를 직접 수정하거나 재계산하지 않는다.
- evidence drift를 막기 위해 `WorkProof` 상세/PDF는 live `Workplace` 대신 record 시점 workplace snapshot을 우선 사용한다.

## 검증해야 할 edge case
- setting 저장과 check-in이 거의 동시에 발생하는 경우
- check-in 이후 workplace 설정이 바뀐 뒤 check-out이 발생하는 경우
- employer가 자신이 관리하지 않는 workplace를 수정하려는 경우

## 열어둘 질문
- 다중 workplace를 한 화면에서 관리할지
- 회사 공통 설정과 workplace 개별 설정을 둘 다 둘지
- 과거 기록 재판정 기능을 관리자 기능으로 둘지
