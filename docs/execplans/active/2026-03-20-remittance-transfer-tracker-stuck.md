# Source Inputs
- `AGENTS.md`
- `.agents/skills/execplan-writer/SKILL.md`
- `.agents/skills/implement-checklist/SKILL.md`
- PRD: `docs/DonDone_PRD_v1.5.md` 7E Remittance
- API contract: `docs/DonDone_P0_API_Contract_v0.md` 7.4, 7.5
- Backend investigation:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/api/RemittanceController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/TransferService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/model/Transfer.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/RemittanceJobWorker.java`
- Mobile investigation:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`

# Goal
송금 후 tracker 화면이 `네트워크에 전송했어요. 확인을 기다리는 중이에요.` 상태에서 멈추는 원인을 제거하고, 비동기 remittance 상태가 모바일 UI에 안정적으로 반영되게 한다.

# In Scope
- 모바일 remittance 상세 폴링 종료 조건 점검 및 수정
- 모바일 remittance 상태 매핑과 tracker 종료 상태 반영 보정
- 필요한 범위의 단위/빌드 검증

# Out of Scope
- 백엔드 remittance 상태 enum/DB 스키마 변경
- remittance worker 재시도 정책 변경
- 새 API 추가 또는 응답 DTO shape 변경

# Affected Modules
## Backend
- 없음. 읽기 전용 원인 확인만 수행

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`

## Docs
- `docs/execplans/active/2026-03-20-remittance-transfer-tracker-stuck.md`

## Shared
- 없음

# Contract Changes
- 없음. 기존 backend `REQUESTED -> SIGNED -> BROADCASTED -> CONFIRMED / FAILED / TIMED_OUT` 응답을 그대로 사용한다.

# Security Notes
- 인증/인가 경로 변경 없음
- 토큰 처리 변경 없음
- 공개 endpoint 추가 없음

# Maintainability Notes
- remittance UI 상태는 backend 상세 상태를 그대로 읽되, UI 내부 enum으로 축약하는 위치를 `DemoSessionViewModel`에 유지한다.
- tracker 멈춤 해결을 위해 폴링 시간만 늘리는 식의 임시처방보다, 상태 동기화 종료 조건과 화면 복귀 시 재동기화가 서로 모순되지 않게 맞춘다.

# Implementation Steps
1. backend remittance 상태 전이와 mobile 상태 매핑의 실제 불일치 지점을 확인한다.
2. mobile 폴링이 terminal 상태를 놓치거나 너무 빨리 끝나는 조건을 수정한다.
3. tracker 화면이 `SUBMITTED`에 고정되는 경로를 막고, backend terminal 상태를 다시 반영하도록 보정한다.
4. 필요한 경우 remittance UI 문구를 상태 의미에 맞게 유지한다.
5. 모바일 단위 테스트 또는 assemble 검증을 수행한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew :app:testDebugUnitTest`
- 필요 시 `cd apps/dondone-mobile/android && ./gradlew :app:assembleDebug`

# Review Focus
- backend 상태 문자열과 mobile 매핑이 완전히 일치하는지
- terminal 상태 수신 후 polling/job/UI 상태가 서로 충돌하지 않는지
- tracker 화면이 실패/확정 상태에서 닫기 또는 확인으로 정상 전환되는지

# Worktree Split Decision
- Single lane

모바일 ViewModel과 tracker UI가 같은 상태 모델을 공유하고, 이번 수정은 shared DTO나 auth 계약 변경 없이 한 흐름 안에서 닫힌다. 병렬 분리는 이득보다 충돌 가능성이 크다.

# Commit Plan
- `docs: add remittance tracker stuck execplan`
- `fix: keep remittance tracker synced with terminal backend status`

# Open Questions
- 없음

# Assumptions
- backend는 현재 상세 API에서 terminal 상태를 정상 반환하고 있으며, 주된 문제는 모바일 polling/state sync 쪽이다.
- P0 범위에서는 tracker가 백그라운드 장기 추적까지 보장할 필요는 없지만, 현재 세션 안에서는 terminal 상태를 놓치지 않아야 한다.
