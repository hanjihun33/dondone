# Source Inputs
- 사용자 요구: 근무 탭을 홈 탭의 톤앤매너에 맞춰 다시 구성
- 현재 홈 탭 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
- 현재 근무 탭 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- 현재 근무 탭 표시 모델: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- 탐색 메모:
  - 근무 탭은 현재 `PunchCard / CalendarCard / RecentLogsCard / AuditCard` 중심의 카드형 구조
  - 홈 탭은 화이트 캔버스 위에 얇은 디바이더, 단순 섹션 헤더, key-value row 중심 리듬
  - 모바일 presentation 범위에서 대부분 닫히며 DTO/API/Auth 영향 없음

# Goal
- 근무 탭을 홈 탭과 같은 화이트 캔버스, 얇은 디바이더, 단순한 정보 위계로 재구성한다.
- 현재 근무 데이터 구조와 기능(출근/퇴근, 월별 달력, 기록 수정, 변경 이력)은 유지한다.
- 카드/그라데이션 중심 표현은 줄이고, 홈 탭과 자연스럽게 이어지는 섹션 흐름으로 정리한다.

# In Scope
- `WorkproofScreen.kt` 메인 레이아웃 재구성
- 필요 시 `WorkproofUiModel.kt` 표시용 문구/그룹화 필드 정리
- 기존 수정 바텀시트가 메인 화면 톤과 충돌하지 않도록 최소 정리

# Out of Scope
- 근무 기록 저장 로직 변경
- reducer/viewmodel 도메인 규칙 변경
- 백엔드/API/Auth 계약 변경
- 실제 위치/첨부 파일 처리 로직 변경

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- 필요 시 `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/ScreenChrome.kt`

## Docs
- `docs/execplans/active/2026-03-14-mobile-workproof-home-tone-alignment.md`

## Shared
- 없음

# Contract Changes
- 외부 DTO/API/DB schema 변경 없음
- Android 내부 `WorkproofUiModel`에 표시 전용 필드가 추가되거나 불필요 필드가 줄 수 있음

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 첨부 파일 선택/수정 저장 흐름은 기존 동작 유지
- 실제 위치 기반 검증이나 민감 권한 처리 확대 없음

# Maintainability Notes
- 홈 탭 스타일을 맞춘다고 공용 디자인 시스템 추상화를 과하게 늘리지는 않는다.
- 근무 탭 메인 구조 정리에 집중하고, 수정 바텀시트/월별 달력 로직은 가능한 한 분리 유지한다.
- 현재 `WorkproofScreen.kt`는 길이가 길어 복잡도 hotspot이므로, 필요한 경우 메인 화면 섹션 단위로만 정리하고 도메인 로직은 `WorkproofUiModel.kt`에 둔다.

# Implementation Steps
1. 홈 탭의 섹션 구조를 기준으로 근무 탭의 핵심 정보 우선순위를 재정의한다.
2. `WorkproofUiModel.kt`에서 홈 톤에 맞는 짧은 문구/요약 표현이 필요하면 보강한다.
3. `WorkproofScreen.kt` 메인 레이아웃을 `오늘 근무 / 근무 달력 / 최근 기록 / 변경 이력` 섹션 흐름으로 재구성한다.
4. 기존 카드/그라데이션/보조 설명을 줄이고 key-value, 리스트, 얇은 divider 중심으로 정리한다.
5. 수정 바텀시트와 메인 화면 연결이 깨지지 않는지 확인한다.
6. 달력 월 이동 시 상세 화면의 최근 기록/변경 이력도 같은 월 기준으로 보이도록 동기화한다.
7. 근무 시간 수정 바텀시트도 홈 탭 리듬에 맞게 카드 의존도를 줄이고 단일 CTA 중심으로 재구성한다.
8. `WorkproofScreen.kt`의 유니코드 이스케이프 문자열을 정상 한글로 복원하고, 불필요한 상태/파라미터와 중첩 래퍼를 정리한다.

# Test Plan
- 정적 확인
  - 출근/퇴근 버튼 enabled 조건 유지 확인
  - 최근 기록 수정 버튼과 바텀시트 진입 유지 확인
- 달력 이전/다음 월 탐색 유지 확인
- 다른 월로 이동했을 때 최근 기록/변경 이력이 함께 비워지거나 해당 월 기준으로 바뀌는지 확인
- 근무 시간 수정 바텀시트에서 사유 선택, 메모 입력, 첨부 선택, 저장/닫기 흐름이 유지되는지 확인
- 가능하면 빌드 확인
  - `:app:compileDebugKotlin`
  - 환경 제약 시 blocker 명시

# Review Focus
- 홈 탭과 시각적 리듬이 자연스럽게 이어지는가
- 근무 탭의 핵심 행동(출근/퇴근, 기록 수정)이 이전보다 더 빨리 읽히는가
- 달력/기록/변경 이력이 과하게 장식적이지 않고 정보 구조로 정리되었는가
- presentation 리팩토링이 기존 동작을 깨지 않았는가

# Worktree Split Decision
- Single lane

근무 탭 메인 화면과 표시 모델이 함께 움직이고, `WorkproofScreen.kt` 내부 섹션 간 결합이 있어 단일 레인에서 정합성을 맞추는 편이 안전하다.

# Commit Plan
- 1개 커밋 기본
  - `feat: 근무 탭 UI를 홈 톤앤매너로 정렬`

# Open Questions
- 최근 기록과 변경 이력을 모두 메인 화면에 유지할지, 일부를 축약할지
- 수정 바텀시트 톤까지 이번 범위에서 맞출지, 메인 화면만 우선 정리할지

# Assumptions
- 사용자가 말한 `근무 탭`은 현재 `Route.WORKPROOF` 메인 화면을 의미한다.
- 홈 탭 톤앤매너는 화이트 캔버스, 디바이더, 얇은 카드 경계, 짧은 문구 위계를 의미한다.
- 출근/퇴근/수정 기능 자체는 유지하고 시각 구조만 우선 정리한다.
