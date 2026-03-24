# Source Inputs
- Root guidance:
  - `AGENTS.md`
- Skill guidance:
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
- 사용자 요청:
  - 모바일 급여점검을 세그먼트 버튼 대신 실제 3페이지처럼 나뉜 단계형 플로우로 바꾸고 싶음
- 현재 구현:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- 참조 구현:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- 탐색 메모:
  - 현재 급여 화면은 `요약 / 상세 근거 / 다음 행동` 세그먼트 전환 구조로 구현되어 있음
  - 사용자는 카드 접기형보다 아예 페이지를 넘기는 듯한 3단계 플로우를 선호함
  - DTO/API/Auth 변경 없이 Compose 레벨 재구성으로 대부분 닫힘

# Goal
급여점검 탭을 세그먼트 전환 UI 대신 `결과 확인`, `계산 근거`, `다음 행동`의 3단계 페이지형 플로우로 재구성해, 같은 화면 안에서 현재 단계만 크게 보여주고 기존 입금 기록/급여 확인 생성/보조 액션 흐름은 유지한다.

# In Scope
- 급여점검 화면 상단 안내를 단계형 페이지 흐름에 맞게 정리
- 기존 섹션을 `결과 확인 / 계산 근거 / 다음 행동` 페이지로 재배치
- 상단 진행 표시와 이전/다음 이동 추가
- 필요한 경우 `WageScreen.kt` 내부 composable 분리 및 재사용 정리
- 필요 시 급여 화면 표시 전용 내부 enum/상수 추가

# Out of Scope
- 백엔드 API/DTO/DB schema 변경
- 급여 계산 로직 변경
- 새 navigation route 또는 bottom sheet 추가
- 메뉴/송금/근무 탭 동작 변경
- 급여 판단 문구의 도메인 의미 변경

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageScreen.kt`
- 필요 시 소규모 정리:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageUiModel.kt`

## Docs
- `docs/execplans/active/2026-03-23-mobile-wage-step-layout.md`

## Shared
- 없음

# Contract Changes
- 외부 API/DTO/DB 계약 변경 없음
- Android 내부 화면 계약도 유지
- `WageUiModel` 공개 필드는 확대하지 않고 `WageScreen.kt` 내부 단계 상태만 사용한다

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 급여 결과는 계속 참고용 추정치로 표시
- 사용자가 단계 전환 중에도 민감 데이터 범위가 바뀌지 않도록 기존 표시 데이터만 재배치한다

# Maintainability Notes
- `WageScreen.kt`는 이미 길고 섹션 책임이 섞여 있으므로, 이번 변경은 “새 화면 추가”보다 “기존 큰 카드 분해 + 재조합”에 집중해야 한다
- 단계 전환 상태는 화면 내부의 단일 enum으로 관리해 viewmodel까지 UI 전용 상태를 퍼뜨리지 않는다
- 현재 단계만 크게 보여주고, 나머지 단계는 상단 진행 표시에서 바로 이동할 수 있게 해 정보 밀도를 낮춘다
- 기존 `WageDifferenceCard`가 요약/근거/액션을 한 번에 책임지고 있어 hotspot이므로, 요약 정보와 액션 블록을 분리해 재사용 가능하게 만드는 편이 안전하다

# Implementation Steps
1. 급여 화면 상단을 단계형 페이지 흐름에 맞는 짧은 안내 헤더로 바꾼다
2. 화면 내부 상태로 현재 페이지 단계를 관리하고, surface state가 `CONTENT`일 때만 현재 단계 본문을 노출한다
3. 상단에 3단계 진행 표시를 두고 현재 단계와 다른 단계로 직접 이동할 수 있게 한다
4. `결과 확인` 페이지에 차액 상태와 실입금 입력을 묶는다
5. `계산 근거` 페이지에 안내, 월간 요약, 추정 급여, 확인한 근거를 묶는다
6. `다음 행동` 페이지에 급여 확인 생성, 진행 단계, 보조 액션을 묶는다
7. 하단 이전/다음 이동 버튼으로 페이지 전환을 제공한다
8. 가능하면 Android 컴파일 또는 assemble로 정적 회귀를 확인한다

# Test Plan
- 정적 확인:
  - 각 단계가 페이지처럼 분리되어 현재 단계 본문만 노출되는지 확인
  - 상단 진행 표시와 하단 이전/다음 버튼이 같은 상태를 바라보는지 확인
  - 실입금 입력/적용 동작이 유지되는지 확인
  - `급여 확인 생성` CTA와 보조 액션이 `다음 행동` 페이지에서만 노출되는지 확인
  - locked/unlocked 상태에 따라 행동 영역이 올바르게 유지되는지 확인
- 가능 시 빌드:
  - `cd apps/dondone-mobile/android && ./gradlew.bat :app:assembleDebug`
  - 환경 제약 시 blocker 명시

# Review Focus
- 단계형 페이지 플로우가 실제 읽기 부담을 줄이는지
- 핵심 결과가 `결과 확인` 페이지에서 충분히 보이는지
- 근거/액션 분리 중 상태 회귀나 CTA 누락이 없는지
- `WageScreen.kt` 내부 분해가 과도한 중복 없이 유지보수성을 개선했는지

# Worktree Split Decision
- Single lane

이번 변경은 한 화면 내부의 섹션 구조와 상태 표시가 함께 움직인다. `WageScreen.kt`가 단일 hotspot이고 새 DTO나 라우트는 없으므로, 한 레인에서 구조를 일관되게 정리하는 편이 가장 안전하다.

# Commit Plan
- `docs: update mobile wage step flow execplan`
- `feat: refactor wage screen into staged pages`

# Open Questions
- 상단 진행 표시에서 각 단계에 어느 정도 보조 정보를 남길지
- 단계 전환 시 현재 스크롤을 항상 상단으로 올릴지 여부

# Assumptions
- 사용자가 말한 “페이지를 나누기”는 실제 nav route 분리보다 같은 급여 탭 내 페이지형 단계 전환을 의미한다
- MVP 범위상 사용자는 먼저 핵심 결과를 보고, 필요할 때만 근거와 후속 액션을 확인하는 흐름이 적절하다
- 단계 페이지는 3개가 적정하며, 4개 이상으로 쪼개면 오히려 사용 흐름이 끊긴다
