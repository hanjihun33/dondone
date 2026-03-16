# Findings
- 현재 기준 blocker 급 신규 finding은 없음.
- 이번 리뷰 패스에서 확인된 두 이슈는 바로 수정했다.
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/advance/service/AdvanceService.java`
    - 기존 `hasOutstandingAdvance` 조회가 `userId + yearMonth` 기준이라, 같은 사용자의 다른 근무지 승인 건까지 현재 근무지 eligibility를 막을 수 있었다.
    - `userId + workplaceId + yearMonth + status` 기준으로 범위를 좁혔고, 해당 회귀를 `AdvanceIntegrationTest`에 추가했다.
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/auth/BackendAuthRepository.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/advance/BackendAdvanceRepository.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
    - API base URL 하드코딩과 세션 만료 정리 로직이 중복돼 있어 변경 안전성이 낮았다.
    - 공통 `BackendApiSupport`와 `clearAuthenticatedState/expireSession` 헬퍼로 정리했다.

# Open Questions
- `GET /api/advance/requests?month=YYYY-MM`의 month 기준을 `requestedAt`, 최신 근무기록 month, 데모 시점 중 무엇으로 고정할지 계약에서 더 명확히 할 필요가 있다.
- Android의 `3개월 조회 + detail merge`는 UX 보정이다. 장기적으로는 백엔드가 생성 직후 포함해야 할 month 기준을 더 명확히 내려주는 편이 낫다.

# Testing Gaps
- 모바일은 로컬 `local.properties` SDK 경로에 의존한다. CI/공용 환경에서 같은 경로 문제가 재발하지 않도록 별도 환경 정리가 필요하다.
- Compose UI 상호작용 자체는 빌드/단위 테스트로만 확인했고, 신청 상세 바텀시트의 실제 터치 흐름은 수동 검증이 남아 있다.

# Residual Risks
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/advance/BackendAdvanceRepository.kt`
  - 현재 목록 조회는 전월/당월/익월 3개월을 합치는 휴리스틱이다. 월 기준 계약이 바뀌면 이 부분은 다시 맞춰야 한다.
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remote/BackendApiSupport.kt`
  - base URL은 중앙화했지만 여전히 demo/emulator 고정값이다. 배포 빌드나 환경별 전환이 필요해지면 별도 구성 분리가 필요하다.

# Change Summary
- 백엔드 outstanding advance 범위를 근무지 단위로 수정하고 회귀 테스트를 추가했다.
- 모바일 네트워크 설정과 인증 만료 처리 중복을 정리해 유지보수성을 높였다.
- 백엔드 `test integrationTest`, Android `compileDebugKotlin`, `testDebugUnitTest`, `assembleDebug`를 다시 확인했다.
