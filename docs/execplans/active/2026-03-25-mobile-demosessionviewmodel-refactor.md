# Source Inputs
- 사용자 요청:
  - `DemoSessionViewModel` 파일 길이 과다에 대한 리팩토링(서브 에이전트 활용) 요청
- 작업 가이드:
  - `AGENTS.md`
  - `apps/dondone-mobile/AGENTS.md`
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
- 코드 탐색:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
  - `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/session/DemoSessionViewModelTest.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- 관련 기존 계획:
  - `docs/execplans/active/2026-03-25-mobile-workproof-current-location.md`
  - `docs/execplans/active/2026-03-25-mobile-workproof-screen-refactor.md`

# Goal
`DemoSessionViewModel.kt`(약 2,738 lines)의 기능별 책임을 분리해 유지보수성을 높이고, 기존 UI/도메인 동작·원격 연동·권한/상태 전이 결과를 변경 없이 유지한다.

# In Scope
- `DemoSessionViewModel` 내부 메서드를 기능 축으로 분리(예: auth/workproof/wage/remittance/vault/common sync)
- ViewModel 본체를 오케스트레이션 중심으로 축소
- 공통 remote load/apply 패턴 및 메시지 처리의 중복 제거(동작 동일성 유지)
- 분리된 구조에 맞춰 테스트 영향 범위 보완(최소 필요 범위)
- 네비게이션/화면 호출부의 외부 계약(`StateFlow` 노출, public API 함수 시그니처) 유지

# Out of Scope
- 화면 UI/UX 변경
- 백엔드 API/DTO/DB 스키마 변경
- 인증/권한 정책 변경
- Workproof 위치 정책/지오펜스 규칙 변경
- 신규 라이브러리 도입

# Affected Modules
## Backend
- 없음

## Mobile
- 수정(핵심):
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- 추가(예상):
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionWorkproofHandlers.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionRemittanceHandlers.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionVaultHandlers.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionWageHandlers.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionSyncExtensions.kt`
- 테스트(예상):
  - `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/session/DemoSessionViewModelTest.kt`

## Docs
- `docs/execplans/active/2026-03-25-mobile-demosessionviewmodel-refactor.md`

## Shared
- 없음

# Contract Changes
- backend/mobile API 계약 변경 없음
- `DonDoneNavGraph`에서 사용하는 `DemoSessionViewModel` public 함수/StateFlow 계약 변경 없음
- 앱 내부 상태 구조(`*UiState`)의 필드 계약 변경 없음

# Security Notes
- 인증 만료 분기(`clearAuthenticatedState`, unauthorized 예외 처리) 동작을 유지한다.
- 위치 권한/위치 조회 실패 분기와 출퇴근 차단 조건은 유지한다.
- 민감 데이터(토큰/파일 경로) 로그 노출이 늘어나지 않도록 신규 로그 추가를 제한한다.

# Maintainability Notes
- ViewModel의 책임을 “상태 저장소 + 오케스트레이션”으로 제한하고, 도메인별 처리 로직은 파일 단위로 분리한다.
- 공통 원격 로딩/예외 매핑 패턴은 helper로 통합하되, 각 도메인 정책(예: remittance polling, vault pending 처리)은 분리 유지한다.
- `DemoSessionReducer`와 ViewModel 사이 경계를 흐리지 않고, 순수 상태 전이는 reducer/extension으로 유지한다.
- 거대 파일 분해 과정에서 가독성을 위해 private extension/handler 네이밍을 도메인 중심으로 통일한다.

# Implementation Steps
1. `DemoSessionViewModel` 메서드를 도메인별(auth/workproof/wage/remittance/vault/common sync)로 인벤토리화한다.
2. 상태/의존성 접근이 필요한 private 로직을 extension/handler 파일로 추출할 수 있는 최소 단위 API를 설계한다.
3. Workproof 관련 흐름(현재 위치, 출퇴근, PDF, 원격 동기화)을 먼저 분리해 컴파일 가능한 중간 상태를 만든다.
4. Remittance/Vault/Wage 흐름을 순차 분리하고 공통 패턴(helper) 중복을 정리한다.
5. ViewModel 본체에는 public entrypoint + 공통 상태 필드 + 핵심 lifecycle(init/restore)만 남긴다.
6. 기존 단위 테스트를 갱신/보강하고, 도메인별 회귀(인증 만료, 원격 실패, 위치 실패, pending polling)를 확인한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew test --tests com.dondone.mobile.app.session.DemoSessionViewModelTest`
- 가능하면 도메인별 추가 대상:
  - `DemoSessionViewModelTest` 내 workproof/remittance/vault/wage 관련 회귀 케이스
- 환경 제약으로 gradle 실행 불가 시 원인(CRLF/런타임)을 명시 보고

# Review Focus
- public API 및 `StateFlow` 노출 계약이 변경되지 않았는지
- unauthorized/expireSession/clearAuthenticatedState 분기가 누락되지 않았는지
- 위치/출퇴근/문서 생성/송금/예치의 성공·실패 메시지/상태 전이가 동일한지
- polling cancel/start 조건과 requestId 보호 로직이 유지되는지
- 리팩토링 중 도메인 간 순환 의존이 생기지 않았는지

# Worktree Split Decision
- Single lane

`DemoSessionViewModel`은 공통 상태(`_uiState`, `_authUiState`, remote state)와 도메인별 흐름이 강하게 결합돼 있어 병렬 수정 시 충돌 가능성이 높다. shared auth/session 처리와 공통 state mutation이 함께 움직이므로 단일 레인에서 연속적으로 분해하는 편이 안전하다.

# Commit Plan
- `refactor: split demo session viewmodel by domain handlers`
- `test: preserve demo session state transition coverage`

# Open Questions
- 파일 분리 단위를 “도메인별 handler + sync extension”으로 고정할지, 추가로 `auth` 전용 파일까지 분리할지 최종 결정 필요
- 공통 helper 추출 범위를 어디까지 허용할지(가독성 우선 vs 추상화 최소) 결정 필요

# Assumptions
- 이번 작업은 동작 변경 없는 구조 개선을 목표로 한다.
- `DonDoneNavGraph` 및 화면 계층에서 호출하는 ViewModel public API는 유지한다.
- 테스트 러너 환경 이슈가 있더라도, 코드 레벨 회귀 근거를 우선 확보하고 blocker를 명시한다.

# Progress Update (2026-03-25)
- 완료:
  - `DemoSessionMappers.kt` 추가: ViewModel 내부 순수 변환/헬퍼 확장 함수 분리
  - `DemoSessionWorkproofHandlers.kt` 추가: 위치 갱신/출퇴근 submit 흐름 위임
  - `DemoSessionAdvanceHandlers.kt` 추가: advance 선택/상세조회/신청/새로고침 흐름 위임
  - `DemoSessionViewModel.kt` public API는 유지한 채 위임 구조로 축소
- 지표:
  - `DemoSessionViewModel.kt` 2,738 -> 2,190 lines
- 검증:
  - `cd apps/dondone-mobile/android && cmd.exe /c gradlew.bat :app:compileDebugKotlin` 성공
- 다음 단계:
  - Remittance/Vault/Wage 흐름 추가 분리
  - 단위 테스트 영향 범위 보강 및 회귀 확인
