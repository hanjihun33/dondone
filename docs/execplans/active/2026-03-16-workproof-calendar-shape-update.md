# Source Inputs
- 사용자 요구: 근무 일지 달력에서 `출퇴근 안 한 날`은 흰 배경으로, `출퇴근한 날`은 원형으로 표시
- 현재 상태 모델: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- 현재 달력 렌더링: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- 탐색 메모:
  - `WorkproofCalendarCell`이 셀 배경/보더/텍스트를 모두 사각형 기준으로 렌더링
  - `WorkproofCalendarTone.MISSING`도 현재는 회색 라운드 네모 셀
  - 외부 DTO/API/Auth/Security 영향 없음

# Goal
- 근무 일지 달력의 날짜 셀을 기록 유무 중심으로 다시 보이게 만든다.
- 기록이 없는 날은 비워진 흰 배경으로 정리하고, 기록이 있는 날은 원형 강조로 바꾼다.
- 기존 날짜 의미(`MISSING/PARTIAL/COMPLETE/MODIFIED`)와 상호작용은 유지한다.

# In Scope
- `WorkproofCalendarCell`의 셀 모양, 배경, 테두리, 텍스트 표현 수정
- 필요 시 `WorkproofLegendItem`의 마커 모양을 달력과 맞게 수정
- 필요 시 달력 색상 상수 미세 조정

# Out of Scope
- 출퇴근 상태 계산 로직 변경
- 근무 일지 외 다른 화면의 달력 리디자인
- DTO/API/DB/Auth 변경

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- 필요 시 `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`

## Docs
- `docs/execplans/active/2026-03-16-workproof-calendar-shape-update.md`

## Shared
- 없음

# Contract Changes
- 없음
- 내부 UI 표시 방식만 변경

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 민감 정보 처리 변화 없음

# Maintainability Notes
- 상태 판정 로직은 가능한 그대로 두고, 표현 변경은 `WorkproofScreen.kt`에 닫아 둔다.
- `MISSING`과 기록 있음 상태를 shape 차원에서 분리하되, 색상 의미는 기존 tone 체계를 재사용한다.
- 범례가 달력과 다른 shape를 가리키지 않도록 함께 정리한다.

# Implementation Steps
1. `WorkproofCalendarCell`에서 비기록일과 기록일의 shape를 분리한다.
2. `MISSING`은 흰 배경/무테두리 또는 매우 약한 강조만 남기고 숫자 중심으로 정리한다.
3. `PARTIAL/COMPLETE/MODIFIED`는 원형 셀로 바꾸고 기존 tone 색상 의미를 유지한다.
4. 현재 날짜 강조가 shape 변경 이후에도 읽히는지 맞춘다.
5. 범례 마커도 원형/중성 톤 기준으로 맞춘다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew --no-daemon -Dkotlin.incremental=false :app:compileDebugKotlin`
- 달력 셀 렌더링에서 import/shape/border 오류 없는지 확인
- 가능하면 실제 화면에서 `기록 없음`, `출근만`, `출퇴근 완료`, `수정됨`, `오늘` 상태를 눈으로 확인

# Review Focus
- 기록 없는 날이 과하게 강조되지 않는가
- 기록 있는 날이 원형으로 명확히 구분되는가
- 오늘 표시가 여전히 읽히는가
- 범례와 실제 셀 표현이 일치하는가

# Worktree Split Decision
- Single lane

달력 셀과 범례가 같은 파일 안에서 묶여 있고, 상태별 shape와 색상을 함께 조정해야 하므로 단일 레인으로 진행한다.

# Commit Plan
- `feat: 근무 달력 날짜 셀 디자인 정리`

# Open Questions
- 없음

# Assumptions
- 사용자가 말한 달력은 `근무 일지` 화면의 달력이다.
- `출퇴근한 날`에는 `PARTIAL`, `COMPLETE`, `MODIFIED`를 모두 포함한다.
- 기록 없는 날은 흰 바탕에 가깝게 단순화하고, 기록 있는 날만 shape 강조를 준다.
