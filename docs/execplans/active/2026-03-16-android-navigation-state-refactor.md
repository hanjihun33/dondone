## Source Inputs
- 사용자 제보: `기록 보기` 이후 하단 홈 버튼이 기대대로 동작하지 않는 Android 모바일 버그
- 모바일 코드 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- 모바일 코드 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- 모바일 코드 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- 테스트 상태 확인: `./gradlew test` 실행 결과 `TransferUiModelTest` 1건 실패

## Goal
Android 모바일에서 루트 탭 이동 규칙을 일관되게 정리하고, WorkProof 상세/수정 상태가 루트 탭 전환 후 잘못 복원되는 문제를 줄이며, 현재 깨진 `TransferUiModelTest`까지 함께 복구한다.

## In Scope
- Android 루트 탭 이동 API 정리
- Home/Wage 등 내부 CTA가 루트 탭 이동 시 같은 규칙을 사용하도록 정리
- WorkProof 상세/수정 관련 임시 UI 상태의 소유권 정리
- WorkProof 뒤로가기 동작 정리
- Android 단위 테스트 추가/수정
- 깨진 `TransferUiModelTest` 원인 수정 및 회귀 검증

## Out of Scope
- mockup 웹 수정
- backend/API/DTO 변경
- 신규 화면 추가
- 디자인 전면 개편

## Affected Modules
### Backend
- 없음

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- 관련 테스트 파일

### Docs
- 본 실행 계획 문서

### Shared
- 없음

## Contract Changes
- 외부 API/DTO/DB 스키마 변경 없음
- Android 내부 UI 상태 전환 규칙만 변경

## Security Notes
- 인증/인가/토큰/노출 경로 영향 없음
- 테스트넷/데모 범위 유지

## Maintainability Notes
- 루트 탭 이동 규칙은 한 곳에서 소유하도록 정리해 중복 `navController.navigate(...)` 분기를 줄인다.
- WorkProof 임시 상태는 화면 내부 세부 상태와 루트 탭 상태를 섞지 않도록 분리한다.
- 테스트는 UI 세부 구현보다 상태 전환 규칙에 초점을 맞춰 회귀에 강하게 만든다.

## Implementation Steps
1. Android 루트 탭 이동 헬퍼를 정리하고, Home/Wage 등 루트 탭으로 가는 CTA가 동일한 이동 경로를 사용하도록 맞춘다.
2. WorkProof 상세/수정 상태를 검토해 루트 탭 이탈 시 리셋하거나 복원 범위를 제한한다.
3. WorkProof 시스템 뒤로가기와 화면 내부 뒤로가기 동작을 일관되게 맞춘다.
4. `TransferUiModelTest` 실패 원인을 확인하고 해당 프로덕션/테스트 코드를 최소 수정한다.
5. 변경된 상태 전환 규칙을 검증하는 테스트를 추가/수정한다.

## Test Plan
- `TransferUiModelTest` 수정 및 회귀 검증
- WorkProof 상태 전환용 단위 테스트 추가
- 필요 시 루트 탭 이동 규칙을 검증하는 작은 순수 함수/리듀서 테스트 추가
- 검증 명령:
  - `cd apps/dondone-mobile/android && ./gradlew test`
  - 실패 범위를 줄일 필요가 있으면 `./gradlew testDebugUnitTest --tests '*Workproof*' --tests '*TransferUiModelTest*'`

## Review Focus
- 내부 CTA와 하단 탭이 동일한 루트 탭 이동 규칙을 쓰는지
- WorkProof 상세/수정 상태가 루트 탭 전환 후 부자연스럽게 남지 않는지
- 시스템 뒤로가기와 화면 내부 뒤로가기 의미가 충돌하지 않는지
- `TransferUiModelTest` 수정이 표현 포맷/리밋 계산 회귀를 만들지 않는지

## Worktree Split Decision
- Single lane

루트 탭 이동 규칙, WorkProof 상태, 관련 테스트가 서로 강하게 묶여 있어서 병렬 작업 시 충돌 위험이 높다. 공통 네비게이션 계약이 움직이는 동안은 한 번에 정리하는 편이 안전하다.

## Commit Plan
- `mobile: unify root tab navigation state handling`
- `test: cover workproof navigation state regressions`

## Open Questions
- 루트 탭 재진입 시 WorkProof 월 오프셋까지 항상 초기화할지 여부
- WorkProof 수정 시트가 열린 상태에서 다른 루트 탭으로 이동하면 즉시 닫는 것이 제품 의도와 맞는지 여부

## Assumptions
- 루트 탭 이동은 하단 탭과 내부 CTA 모두 같은 규칙을 써야 한다.
- WorkProof 상세/수정 상태는 루트 탭 전환 시 유지하지 않는 것이 현재 UX 기대에 더 가깝다.
- 외부 계약 변경 없이 Android 내부 상태 관리 리팩토링으로 해결 가능하다.
