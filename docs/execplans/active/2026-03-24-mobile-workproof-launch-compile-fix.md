# 2026-03-24 Mobile Workproof Launch Compile Fix

## Source Inputs
- 사용자 제공 `:app:assembleDebug` 실패 로그 (`DonDoneNavGraph.kt`, `DemoSessionViewModel.kt`, `WorkproofScreen.kt`의 `WorkproofLaunch*` 미해결)
- 현재 브랜치 코드 탐색 결과 (`app/session/WorkproofLaunchRequest.kt` 존재, 참조부와 타입 정합 필요)
- `docs/DonDone_PRD_v1.5.md`의 P0 증빙 중심 UX 방향 (출퇴근/증빙 생성 진입 유지)

## Goal
- Android `compileDebugKotlin` 실패를 유발하는 `WorkproofLaunch*` 타입 해석 오류를 제거하고, 기존 PDF 생성 진입 이벤트 동작을 유지한다.

## In Scope
- `WorkproofLaunchRequest`, `WorkproofLaunchTarget` 선언/참조 정합성 복구
- 네비게이션 및 Workproof 화면의 launch request 소비 흐름 유지

## Out of Scope
- Workproof UI/문구/상태 전환 로직 변경
- 백엔드 API/DTO/DB 스키마 변경
- 인증/권한 정책 변경

## Affected Modules
### Backend
- 없음

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/*`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`

### Docs
- 본 실행계획 문서

### Shared
- 없음

## Contract Changes
- 없음 (앱 내부 상태 타입 정리만 수행)

## Security Notes
- 인증/토큰/노출 경로 변경 없음
- launch request는 로컬 상태 전달 이벤트로만 사용

## Maintainability Notes
- launch request 타입 선언 위치를 기존 세션 상태 선언 파일과 일관되게 맞춰 타입 발견 실패 위험을 낮춘다.
- 이벤트 타입을 단일 선언 지점으로 유지해 탐색성과 추적성을 높인다.

## Implementation Steps
1. 기존 세션 상태 선언 파일에 `WorkproofLaunchTarget`, `WorkproofLaunchRequest`를 추가한다.
2. 중복/충돌을 막기 위해 단독 선언 파일(`WorkproofLaunchRequest.kt`)은 제거한다.
3. `DemoSessionViewModel`, `DonDoneNavGraph`, `WorkproofScreen` 참조가 추가 import 없이 일관되게 해석되는지 확인한다.
4. deprecated warning(`LocalClipboardManager`)은 본 범위 밖으로 두고 컴파일 오류만 해결한다.

## Test Plan
- 가능 시 `:app:compileDebugKotlin` 또는 `:app:assembleDebug` 재실행
- 환경 제약으로 실행 불가 시, 변경 영향 파일 정적 검토 결과와 사용자 로컬 재검증 명령 제공

## Review Focus
- launch request 이벤트가 null-consume 패턴을 유지하는지
- 네비게이션/화면에서 `WorkproofLaunchTarget.PDF_CREATION` 분기 동작이 기존과 동일한지
- 불필요한 계약/보안 영향이 추가되지 않았는지

## Worktree Split Decision
- Single lane
- 이유: 동일 launch 타입을 세션/네비게이션/화면이 공통으로 참조하므로 병렬 분할 시 충돌 위험이 높고, 작업 크기가 작아 단일 레인 처리 효율이 높다.

## Commit Plan
- 1개 커밋: `fix: 모바일 workproof launch 타입 해석 오류 복구`

## Open Questions
- 없음

## Assumptions
- 사용자 목표는 모바일 빌드 복구이며 기능 확장/UX 변경은 요구하지 않는다.
- 컴파일 경고(`LocalClipboardManager` deprecation)는 현재 실패 원인이 아니므로 후속 분리 대응한다.
