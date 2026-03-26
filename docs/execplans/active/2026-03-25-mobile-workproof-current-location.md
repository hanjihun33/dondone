# Source Inputs
- 작업 가이드:
  - `AGENTS.md`
  - `apps/dondone-mobile/AGENTS.md`
- 기존 구현 탐색:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/workproof/BackendWorkproofRepository.kt`
  - `apps/dondone-mobile/android/app/src/main/AndroidManifest.xml`
- 기존 계획 참고:
  - `docs/execplans/active/2026-03-17-workproof-gps-geofence.md`

# Goal
모바일 WorkProof 화면에서 `현재 내 위치` 핀과 출퇴근 요청 좌표가 실제 단말 GPS를 사용하도록 수정한다. 위치 권한과 위치 조회 실패를 명시적으로 처리해, 실연동 사용자가 데모 좌표를 서버에 보내지 않도록 막는다.

# 2026-03-25 Debug Update
- 실기기 로그 확인 결과:
  - `getCurrentLocation(gps)`가 타임아웃되는 케이스가 빈번함
  - `lastKnown` 후보가 `30초 / 80m` 기준에서 탈락해 `ERROR`로 떨어짐
  - 원격 Workproof 동기화 직후 초기 current 좌표가 데모 시드(구미)로 남아 혼선을 유발함
- 이번 추가 수정 범위:
  - provider 순차 재시도(`GPS -> NETWORK -> PASSIVE`)
  - `lastKnown` 허용 기준 현실화(실기기 기준 10분/100m)
  - 원격 Workproof 첫 동기화(`IDLE`) 시 current 좌표를 workplace로 정렬

# In Scope
- Android 위치 권한 선언 추가
- 현재 위치 조회 provider 추가
- WorkProof 화면 진입 시 권한 확인 및 위치 갱신 트리거 추가
- 지도 현재 위치 핀/카메라가 최신 좌표를 반영하도록 수정
- 실연동 출근/퇴근 요청 직전에 최신 위치를 재조회하도록 보강
- 위치 상태에 맞춘 UI 메시지/버튼 활성화 보정
- 관련 unit test 보강

# Out of Scope
- backend API 계약 변경
- geofence 정책 변경
- 백그라운드 위치 추적
- 실시간 위치 스트리밍
- 다른 탭의 위치 의존 기능 확장

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/AndroidManifest.xml`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/WorkproofActionUiState.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/location/CurrentLocationProvider.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/session/DemoSessionViewModelTest.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModelTest.kt`

## Docs
- `docs/execplans/active/2026-03-25-mobile-workproof-current-location.md`

## Shared
- 기존 `latitude` / `longitude` backend 계약 유지

# Contract Changes
- backend request/response 필드 변경 없음
- 모바일 내부 상태 계약에 현재 위치 조회 상태를 추가한다

# Security Notes
- 위치 권한은 WorkProof 화면에서만 요청한다.
- 위치 권한이 없거나 좌표 조회에 실패하면 실연동 출퇴근 요청을 막아 데모 좌표 전송을 방지한다.
- 새 외부 전송 데이터는 기존 `latitude`, `longitude` 필드 범위를 넘지 않는다.

# Maintainability Notes
- 위치 조회 로직은 Compose 화면에 직접 넣지 말고 별도 provider로 분리한다.
- ViewModel은 provider 결과를 앱 상태로 반영하고, 화면은 상태를 소비하는 구조를 유지한다.
- 지도 갱신을 위해 broad refactor를 하지 않고 현재 좌표 변경 시 안전하게 다시 그리는 수준으로 제한한다.

# Implementation Steps
1. 위치 권한 선언과 현재 위치 provider를 추가한다.
2. ViewModel에 현재 위치 상태/갱신 메서드를 추가하고 WorkProof state에 좌표를 반영한다.
3. WorkProof 화면에서 권한 요청과 위치 새로고침 트리거를 연결한다.
4. 지도 현재 위치 핀과 카메라가 갱신 좌표를 반영하도록 수정한다.
5. 실연동 출근/퇴근 요청 직전에 위치를 다시 확인하고 실패 시 요청을 중단한다.
6. WorkProof UI model test와 ViewModel test를 보강한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew test --tests com.dondone.mobile.feature.workproof.presentation.WorkproofUiModelTest --tests com.dondone.mobile.app.session.DemoSessionViewModelTest`

# Review Focus
- 권한 거부/조회 실패 시 실연동 출퇴근이 차단되는지
- 지도 핀과 카메라가 최신 current 좌표를 반영하는지
- remote workplace sync가 current 좌표를 덮어쓰지 않는지
- demo fallback 동작이 과도하게 깨지지 않는지

# Worktree Split Decision
- Single lane

위치 provider, ViewModel 상태, Compose 화면, 테스트가 하나의 흐름으로 묶여 있어 병렬 분리 이점이 작고 충돌 위험이 높다.

# Commit Plan
- `feat: WorkProof 현재 위치 실연동 연결`
- `test: WorkProof 현재 위치 상태 검증 보강`

# Open Questions
- 없음

# Assumptions
- WorkProof 외 화면에서는 이번 위치 권한을 사용하지 않는다.
- authenticated + remote content 상태를 실연동 모드로 보고, 이때만 위치 미확보 시 출퇴근을 차단한다.
- 위치 조회는 foreground 단건 조회로 충분하며 지속 추적은 필요 없다.
