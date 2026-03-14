# Source Inputs
- 사용자 요구: 메뉴 탭을 홈 탭의 톤앤매너에 맞춰 재구성
- 현재 홈 탭 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
- 현재 메뉴 탭 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuScreen.kt`
- 현재 메뉴 표시 모델: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuUiModel.kt`
- 탐색 메모:
  - 메뉴 탭은 현재 `서비스 카드 + 문서 카드 + 3개 바텀시트` 중심의 카드형 구조
  - 홈 탭은 화이트 캔버스, 얇은 디바이더, 단순 섹션 헤더, 리스트/키밸류 중심 리듬
  - 모바일 presentation 범위에서 대부분 닫히며 DTO/API/Auth 영향 없음

# Goal
- 메뉴 탭 메인 화면을 홈 탭과 같은 화이트 캔버스, 얇은 디바이더, 단순한 정보 위계로 재구성한다.
- 서비스 진입점과 문서 진입점은 유지하되, 과한 카드 표현과 설명성 문구를 줄인다.
- 문서/신고 준비/설정 바텀시트도 메인 화면과 같은 리듬으로 맞춘다.

# In Scope
- `MenuScreen.kt` 메인 레이아웃 재구성
- `MenuScreen.kt` 내부 바텀시트 UI 리듬 정리
- 필요 시 `MenuUiModel.kt`의 표시 전용 필드 정리

# Out of Scope
- 메뉴 탭 도메인 기능 변경
- reducer/viewmodel 규칙 변경
- 백엔드/API/Auth 계약 변경
- 실제 공유/다운로드/복사 기능 구현 확대

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuUiModel.kt`

## Docs
- `docs/execplans/active/2026-03-14-mobile-menu-home-tone-alignment.md`

## Shared
- 없음

# Contract Changes
- 외부 DTO/API/DB schema 변경 없음
- Android 내부 `MenuUiModel`의 표시 전용 필드가 줄어들 수 있음

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 실제 문서 공유/다운로드 동작은 기존 데모 범위를 유지
- 법률/신고 관련 안내는 참고용 표현을 유지하되 과한 설명은 줄인다

# Maintainability Notes
- `MenuScreen.kt`는 메인 화면과 바텀시트 정의가 한 파일에 모여 있어 복잡도 hotspot이다.
- 이번 범위는 공용 디자인 시스템 추상화를 늘리기보다, 홈 탭과 비슷한 섹션 헬퍼를 메뉴 파일 내부에서 재사용하는 수준으로 제한한다.
- 바텀시트까지 같은 톤으로 맞추되, 기능 로직과 상태 전환은 건드리지 않고 프레젠테이션 구조만 정리한다.

# Implementation Steps
1. 홈 탭 섹션 리듬을 기준으로 메뉴 탭 정보 우선순위를 `서비스 / 문서` 흐름으로 재정의한다.
2. `MenuUiModel.kt`에서 실제 사용하지 않거나 과한 설명을 만드는 필드가 있으면 정리한다.
3. `MenuScreen.kt` 메인 화면을 화이트 캔버스 + 얇은 디바이더 + 리스트형 row 중심으로 재구성한다.
4. 문서 카드와 서비스 액션을 홈 탭 리듬에 맞는 단순한 row/section 구조로 바꾼다.
5. 문서 시트, 신고 준비 시트, 설정 시트도 카드 의존도를 줄이고 단일 CTA/명확한 섹션 흐름으로 재정리한다.
6. 후속 리팩토링으로 `MenuScreen.kt`의 빈 subtitle 렌더링, 중복 액션 row, 마지막 divider, 미사용 UI 모델 필드를 제거해 화면 구조를 단순화한다.

# Test Plan
- 정적 확인
  - 서비스 진입점 4개 동작 유지 확인
  - 문서 상세, 신고 준비, 설정 시트 진입/닫기 유지 확인
  - 문서 상태별 CTA enabled 흐름 유지 확인
- 가능하면 빌드 확인
  - `:app:assembleDebug`
  - 환경 제약 시 blocker 명시

# Review Focus
- 홈 탭과 시각적 리듬이 자연스럽게 이어지는가
- 메뉴 탭 메인 화면이 카드 나열이 아니라 정보 흐름으로 읽히는가
- 각 바텀시트가 같은 톤을 유지하면서도 기능 구분은 명확한가
- 표시 모델 정리가 실제 사용 흐름을 깨지 않았는가

# Worktree Split Decision
- Single lane

메뉴 메인 화면과 바텀시트, 표시 모델이 한 파일/한 흐름으로 강하게 결합되어 있어 단일 레인에서 정합성을 맞추는 편이 안전하다.

# Commit Plan
- 1개 커밋 기본
  - `feat: 메뉴 탭 UI를 홈 톤앤매너로 정렬`

# Open Questions
- 문서 섹션을 모두 메인 화면에 유지할지, 일부를 요약할지
- 신고 준비 시트의 설명 문구를 얼마나 줄일지

# Assumptions
- 사용자가 말한 `메뉴 탭`은 현재 `Route.MENU` 메인 화면과 그 내부 바텀시트를 포함한다.
- 홈 탭 톤앤매너는 화이트 캔버스, 얇은 디바이더, 리스트형 섹션 흐름, 과한 카드 제거를 의미한다.
- 기능 자체는 유지하고 프레젠테이션 구조와 문구 위계만 우선 정리한다.
