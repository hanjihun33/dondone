# Source Inputs
- 사용자 요청:
  - `WorkproofScreen.kt` 파일 길이 과다에 대한 리팩토링 계획 수립 요청
- 작업 가이드:
  - `AGENTS.md`
  - `apps/dondone-mobile/AGENTS.md`
  - `.agents/skills/execplan-writer/SKILL.md`
- 현재 코드 탐색:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/session/DemoSessionViewModelTest.kt`
- 관련 기존 계획:
  - `docs/execplans/active/2026-03-25-mobile-workproof-current-location.md`

# Goal
`WorkproofScreen.kt`(약 2,479 lines)의 presentation 책임을 섹션별 파일로 분리해 유지보수성을 높이고, 기존 UI/상태/권한/지도/PDF 동작을 변경 없이 유지한다.

# In Scope
- `WorkproofScreen.kt` 내부 Composable/helper를 기능 단위로 파일 분리
- 분리 후에도 기존 `WorkproofScreen(...)` 외부 호출 시그니처를 유지
- 지도/권한/현재위치/UI 상태 처리 흐름 유지
- PDF 생성/미리보기/공유 UI 흐름 유지
- 최소 범위의 import/visibility 정리 및 컴파일 오류 해소
- 리팩토링 회귀를 막기 위한 기존 unit test 재실행

# Out of Scope
- 화면 디자인 변경(색상, 타이포, 간격, UX 흐름)
- `WorkproofUiModel` 계산 로직 변경
- `DemoSessionViewModel` 비즈니스 규칙 변경
- 백엔드 API/DTO/DB 스키마 변경
- 새 라이브러리 도입

# Affected Modules
## Backend
- 없음

## Mobile
- 수정:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- 추가(예상):
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofMapSection.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofPdfSection.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofDetailSection.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofCommonComponents.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreenUtils.kt`
- 영향 참조:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`

## Docs
- `docs/execplans/active/2026-03-25-mobile-workproof-screen-refactor.md`

## Shared
- 없음

# Contract Changes
- 외부 API 계약 변경 없음
- 앱 내부 화면 진입/콜백 계약(`WorkproofScreen` 파라미터) 변경 없음
- 위치 권한/오류 노출 메시지 contract 변경 없음

# Security Notes
- 위치 권한 요청/검사 경로는 기존과 동일하게 유지한다.
- 파일 분리 과정에서 권한 체크가 우회되지 않도록 `onRefreshCurrentLocation` 트리거 조건을 유지한다.
- PDF open/share 로직은 기존 `FileProvider` 경로와 플래그를 그대로 유지한다.

# Maintainability Notes
- `WorkproofScreen.kt`는 화면 orchestration과 상위 상태 연결만 담당하도록 축소한다.
- 지도/권한/Map lifecycle 코드는 한 파일로 응집해 변경 영향 범위를 줄인다.
- PDF 영역은 date-range/preset/result sheet를 한 파일로 묶어 중복 상태 분산을 방지한다.
- 공통 UI 컴포넌트와 포맷 유틸은 분리하되, 도메인 계산 로직은 `WorkproofUiModel.kt`에 남긴다.
- private visibility를 기본으로 두고, 파일 간 공유가 필요한 최소 심볼만 `internal`로 노출한다.

# Implementation Steps
1. `WorkproofScreen.kt` 내부를 섹션별로 inventory(지도/PDF/상세/공통/유틸) 분류한다.
2. 지도 섹션(`WorkproofWorkplaceMapCard`, `KakaoWorkplaceMapView`, marker/lifecycle/helper)을 `WorkproofMapSection.kt`로 이동한다.
3. PDF 섹션(`WorkproofPdf*`, file open/share helper)을 `WorkproofPdfSection.kt`/`WorkproofScreenUtils.kt`로 이동한다.
4. 상세/달력/최근기록/감사 로그 영역을 `WorkproofDetailSection.kt`로 이동한다.
5. 공통 카드/헤더/버튼/row 컴포넌트를 `WorkproofCommonComponents.kt`로 이동한다.
6. `WorkproofScreen.kt`는 top-level screen 조립과 이벤트 wiring만 남기도록 정리한다.
7. import/visibility/resource 참조를 정리하고, 컴파일 오류를 해결한다.
8. 관련 unit test 및 assemble 검증을 수행해 리팩토링 회귀를 확인한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew test --tests com.dondone.mobile.feature.workproof.presentation.WorkproofUiModelTest --tests com.dondone.mobile.app.session.DemoSessionViewModelTest`
- `cd apps/dondone-mobile/android && ./gradlew :app:assembleDebug`

# Review Focus
- `WorkproofScreen` 외부 계약(호출부/파라미터)이 변경되지 않았는지
- 지도 lifecycle(`MapView` resume/pause/finish)와 권한 요청 동작이 동일한지
- 현재 위치 새로고침 이후 카메라 이동/핀 갱신 순서가 유지되는지
- PDF date range/preset/result sheet 상태가 분리 과정에서 깨지지 않았는지
- helper 이동으로 인한 resource/string/file-share 회귀가 없는지

# Worktree Split Decision
- Single lane

`WorkproofScreen.kt` 내부 요소들이 공유 상태와 callback에 밀접하게 결합되어 있고, private helper 이동 시 동시 편집 충돌 가능성이 높다. shared DTO/auth 계약 변경은 없지만, 같은 presentation 파일을 다수 lane이 동시에 분리하면 merge risk가 커서 단일 레인 진행이 안전하다.

# Commit Plan
- `refactor: split workproof screen into feature sections`
- `chore: keep workproof screen contract stable after file split`

# Open Questions
- 파일 분리 깊이를 어디까지 허용할지(5개 파일 수준 vs 3개 파일 수준) 최종 합의 필요
- `WorkproofScreenUtils.kt` 분리 없이 각 섹션 파일 내부 private helper로 유지할지 결정 필요

# Assumptions
- 이번 작업의 성공 기준은 동작 변경 없는 구조 개선이다.
- 리뷰어가 추적하기 쉬운 수준으로 파일 수를 제한한다(과도한 마이크로 분할 지양).
- 새 테스트 추가보다 기존 테스트/빌드 통과를 우선한다.
