# Source Inputs
- 사용자 요구: 홈 탭에서 확정된 톤앤매너와 컴포넌트 스타일을 유지한 채 금융 탭부터 순차 리디자인
- 기존 홈 실행 계획: `docs/execplans/active/2026-03-13-mobile-home-toss-reference-redesign.md`
- 홈 기준 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
- 금융 탭 현재 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt`
- 공통 앱 크롬: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- explorer 메모:
  - 홈은 `화이트 배경 + 얇은 구분선 + 리스트형 섹션` 구조로 이미 정리됨
  - 금융 탭은 아직 카드/패널/그라데이션 중심이라 홈과 시각 언어가 분리되어 보임
  - 상태 계산과 DTO는 `FinanceHomeUiModel.kt`에서 이미 충분히 조합되어 있어 presentation 중심 수정이 가능함

# Goal
- Android 금융 탭을 홈 탭과 동일한 정보 위계와 톤앤매너로 재구성한다.
- 홈에서 확정한 `화이트 캔버스 + 절제된 포인트 컬러 + 얇은 섹션 구분 + 텍스트 중심 메트릭 행` 규칙을 금융 탭에도 적용한다.
- 금융 탭의 기존 기능 진입점(계좌 관리, 송금, 미리받기, 예치 이자, 급여 요약)은 유지한다.

# In Scope
- 금융 탭 메인 화면의 섹션 순서, 위계, 여백, CTA 표현 재구성
- 금융 탭 전용 색/구분선/칩/버튼/메트릭 행을 홈 탭 스타일에 맞게 정리
- 금융 탭의 데모 고지 문구를 현재 위치보다 더 자연스러운 섹션 흐름 안에 유지
- 필요 시 금융 탭에서만 쓰는 경량 로컬 컴포넌트 정리

# Out of Scope
- `FinanceHomeUiModel.kt`의 계산 로직 변경
- 미리받기/예치 이자 바텀시트의 대규모 구조 개편
- 백엔드/API/DB/Auth 계약 변경
- 다른 탭(급여, 문서, 메뉴)의 동시 리디자인

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt`
- 필요 시 `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- 참고 기준:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`

## Docs
- `docs/execplans/active/2026-03-13-mobile-finance-style-alignment.md`

## Shared
- 없음

# Contract Changes
- DTO/API/DB schema/API response 변경 없음
- `FinanceHomeUiModel` 데이터 구조 변경은 원칙적으로 없음
- 화면 표현용 문자열 조합이 추가로 필요하면 `presentation` 내부에서만 소비하고 외부 계약은 유지

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 금융 정보 노출 범위 변경 없음
- 데모 환경 안내 문구는 유지해 실제 자금 이동 오해를 방지

# Maintainability Notes
- 홈 스타일을 그대로 복사해 전역 디자인 시스템으로 성급히 승격하지 않는다.
- 금융 탭 고유 레이아웃은 `feature/finance/presentation` 내부에서 우선 닫고, 공통화 가치가 명확한 패턴만 추후 승격한다.
- `FinanceHomeScreen.kt`가 이미 길기 때문에, 섹션 단위 추출은 허용하되 상태 계산 로직은 `FinanceHomeUiModel.kt`에 남긴다.
- 바텀시트는 이번 작업의 핵심이 아니므로, 메인 탭 리듬을 맞추는 최소 조정만 허용한다.

# Implementation Steps
1. 금융 탭의 현재 카드형 섹션을 홈 탭과 같은 리스트형 정보 흐름으로 다시 배치한다.
2. 상단 계좌 영역을 홈의 `이번 달 내 돈` 섹션과 같은 톤으로 정리하되, 금융 탭 기능에 맞게 `계좌 관리`와 `송금하기` 진입을 유지한다.
3. `돈 나누기`, `미리받기`, `예치 이자`, `급여 요약` 영역을 각각 섹션 헤더 + 메트릭 행 + 보조 설명 + CTA 구조로 재구성한다.
4. 섹션 사이를 카드 중첩 대신 디바이더와 여백으로 분리한다.
5. 금융 탭 전용 버튼/칩/메트릭/섹션 컴포넌트를 홈 톤에 맞게 정리한다.
6. 데모 고지 문구를 화면 말미의 자연스러운 안내 블록으로 유지한다.
7. 필요 시 바텀시트 헤더/패널의 시각 톤만 최소 조정해 메인 화면과 괴리를 줄인다.

# Test Plan
- 정적 확인
  - `FinanceHomeScreen.kt` import/참조 정리 확인
  - `FinanceHomeUiModel.kt`와의 필드 불일치 여부 확인
- 빌드/검증
  - 가능하면 `./gradlew :app:compileDebugKotlin`
  - 가능하면 `./gradlew :app:assembleDebug`
- UX 확인
  - 홈 탭과 금융 탭 간 상단/본문/하단 크롬 톤 일관성 확인
  - `계좌 관리`, `송금하기`, `미리받기 신청`, `예치 액션`, `급여 확인` 진입점 유지 확인

# Review Focus
- 홈 탭과 같은 앱으로 보일 정도로 시각 언어가 정렬되었는가
- 카드 중첩이 제거되고 정보 위계가 더 빨리 읽히는가
- CTA 우선순위가 홈 탭 리듬과 충돌하지 않는가
- 데모 고지와 근거 중심 카피가 유지되는가
- 불필요한 전역 토큰 변경 없이 금융 탭 범위에서 정리되었는가

# Worktree Split Decision
- Single lane

금융 탭 메인 화면은 단일 파일 내 섹션과 로컬 컴포넌트 결합도가 높고, 홈 탭 스타일을 참고해 반복적으로 조정해야 합니다. 상태 계약은 고정하지만 레이아웃과 시각 표현이 한 파일에서 같이 움직이므로 병렬 분할 이점이 작습니다.

# Commit Plan
- 1개 커밋 기본
  - `feat: align finance tab with home tab design language`

# Open Questions
- 금융 탭 바텀시트까지 홈 톤으로 얼마나 맞출지
- `돈 나누기` 섹션을 유지하되 우선순위를 계좌/미리받기 대비 어디에 둘지

# Assumptions
- 사용자가 말한 "앱의 홈 탭"은 현재 Android 앱 기준 화면을 의미한다.
- 이번 요청의 핵심은 메인 금융 탭 UI/UX 정렬이며, 도메인 계산과 흐름은 유지한다.
- 홈 탭 스타일은 `HomeScreen.kt`의 현재 구현을 기준으로 삼는다.

# Follow-up Scope Note
- 추가 사용자 요구에 따라 `미리받기 신청`, `예치 이자` 바텀시트도 홈 탭과 같은 화이트 캔버스, 얇은 디바이더, 단일 주 CTA 중심 구조로 재구성한다.
- 이 후속 조정은 `feature/finance/presentation` 범위의 UI 레이아웃 변경이며 계약, 계산 로직, 보안 규칙은 유지한다.
- 후속 정리로 `FinanceHomeScreen.kt` 내부의 미사용 섹션/헬퍼와 불필요한 진입 파라미터를 제거해 파일 복잡도를 낮춘다.
