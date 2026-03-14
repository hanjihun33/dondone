# Source Inputs
- 사용자 요구: 급여 점검 페이지를 홈 탭의 톤앤매너에 맞게 재구성
- 현재 홈 탭 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
- 현재 급여 점검 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageScreen.kt`
- 현재 급여 표시 모델: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageUiModel.kt`
- 탐색 메모:
  - 급여 화면은 현재 `헤더/안내/입금 확인/차액 확인`이 모두 카드형 패널로 나뉘어 있음
  - 홈 탭은 화이트 캔버스, 얇은 디바이더, 단순 섹션 헤더, key-value/행동 중심 리듬
  - 모바일 presentation 범위에서 대부분 닫히며 DTO/API/Auth 영향 없음

# Goal
- 급여 점검 화면을 홈 탭과 같은 화이트 캔버스, 얇은 디바이더, 단순한 정보 위계로 재구성한다.
- 실입금 입력, 차액 확인, 다음 행동 흐름은 유지하되 과한 카드/그라데이션/배지를 줄인다.
- 코파일럿 바텀시트는 유지하되 메인 화면의 시각적 밀도를 낮춘다.

# In Scope
- `WageScreen.kt` 메인 레이아웃 재구성
- 필요 시 `WageUiModel.kt`의 미사용 표시 필드 정리
- 급여 관련 바텀시트가 아니라 메인 화면 위계 정리에 집중

# Out of Scope
- WageEstimator 계산 로직 변경
- reducer/viewmodel 도메인 규칙 변경
- 백엔드/API/Auth 계약 변경
- 실제 급여 판단 로직 확대

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageUiModel.kt`

## Docs
- `docs/execplans/active/2026-03-14-mobile-wage-home-tone-alignment.md`

## Shared
- 없음

# Contract Changes
- 외부 DTO/API/DB schema 변경 없음
- Android 내부 `WageUiModel`의 표시 전용 필드가 줄어들 수 있음

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 급여 결과는 계속 참고용 추정으로 유지
- 법률/재무 최종 판단이 아니라는 안내는 유지하되 과한 설명 중복은 줄인다

# Maintainability Notes
- `WageScreen.kt`는 카드형 헬퍼가 많아 복잡도 hotspot이다.
- 이번 범위는 공용 디자인 시스템을 크게 바꾸기보다, 메인 화면 섹션 위계만 홈 탭 리듬으로 재조정한다.
- 계산/도메인 문구는 `WageUiModel.kt`, 화면 배치는 `WageScreen.kt`가 갖는 경계를 유지한다.

# Implementation Steps
1. 홈 탭 구조를 기준으로 급여 점검의 핵심 흐름을 `실입금 입력 / 요약 / 차액 확인` 중심으로 재배치한다.
2. `WageUiModel.kt`에서 현재 화면에서 쓰지 않는 표시 필드가 있으면 정리한다.
3. `WageScreen.kt`의 큰 카드/히어로/장식 배지를 줄이고 화이트 캔버스 + 디바이더 기반 섹션으로 전환한다.
4. 차액 확인 섹션의 행동 버튼과 보조 액션을 유지하되 정보 위계를 단순화한다.
5. 법률/참고용 안내는 유지하되 반복되는 설명 문구는 줄인다.

# Test Plan
- 정적 확인
  - 실입금 입력/적용 동작 유지 확인
  - 차액 상태별 주요 CTA 유지 확인
  - 코파일럿 칩과 바텀시트 진입 유지 확인
- 가능하면 빌드 확인
  - `:app:assembleDebug`
  - 환경 제약 시 blocker 명시

# Review Focus
- 홈 탭과 시각적 리듬이 자연스럽게 이어지는가
- 실입금 입력과 차액 확인 흐름이 이전보다 더 빠르게 읽히는가
- 과한 카드/그라데이션 제거가 핵심 행동성을 해치지 않았는가
- 미사용 표시 필드 정리가 실제 상태 표현을 깨지 않았는가

# Worktree Split Decision
- Single lane

급여 메인 화면과 표시 모델이 함께 움직이고 `WageScreen.kt` 내부 섹션 결합이 강해 단일 레인에서 정합성을 맞추는 편이 안전하다.

# Commit Plan
- 1개 커밋 기본
  - `feat: 급여 점검 화면을 홈 톤앤매너로 정렬`

# Open Questions
- 코파일럿 칩 영역을 메인 화면에 유지할지, 일부만 남길지
- 차액 확인 섹션의 보조 액션을 어느 정도까지 메인 화면에 노출할지

# Assumptions
- 사용자가 말한 `급여점검 페이지`는 현재 `Route.WAGE` 메인 화면을 의미한다.
- 홈 탭 톤앤매너는 화이트 캔버스, 얇은 디바이더, 단순 섹션 헤더, 과한 카드 제거를 의미한다.
- 기능 자체는 유지하고 프레젠테이션 구조와 문구 위계만 우선 정리한다.
