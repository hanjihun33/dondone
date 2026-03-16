# Source Inputs
- 사용자 요구: 근무 탭에 지도를 넣고 싶음
- PRD 근거: `docs/DonDone_PRD_v1.5.md`
  - `7B. 근무 증거(WorkProof)`의 `이름, 주소, 지도 선택`
  - `출근/퇴근 시점에 위치 스냅샷 1회 저장(백그라운드 추적 없음)`
  - 위치정보는 근무 검증에 쓰이지만 단독 판단 기준이 아님
- 현재 Android 근무 탭 화면: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- 현재 Android 근무 표시 모델: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- 현재 데모 데이터 모델: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
- 탐색 메모:
  - 근무 탭은 현재 `오늘 근무` 카드만 메인 화면에 노출되고, 상세 화면에 달력/기록/변경 이력이 있음
  - 현재 데이터는 `workplaceName`, `workplaceAddress`만 있고 좌표/실제 위치 스냅샷 필드는 없음
  - Android 앱에 외부 지도 SDK 의존성은 아직 없음

# Goal
- 근무 탭 메인 화면에 사업장 위치를 직관적으로 보여주는 지도형 섹션을 추가한다.
- 현재 출근/퇴근 동선과 WorkProof 증빙 중심 메시지는 유지한다.
- 좌표/권한/외부 SDK 없이도 데모에서 자연스럽게 읽히는 수준의 위치 표현을 제공한다.

# In Scope
- `WorkproofUiModel.kt`에 지도형 표시용 사업장 정보 필드 추가
- `WorkproofScreen.kt`에 근무 탭 메인 화면용 지도 카드 섹션 추가
- 주소/사업장명/위치 검증 안내 문구를 evidence-first 맥락으로 노출

# Out of Scope
- 실제 지도 SDK 연동
- GPS 권한 요청, 실시간 위치 추적, 체크인/체크아웃 위치 저장 로직 추가
- 백엔드 API/DB/DTO 변경
- 상세 화면의 위치 스냅샷 타임라인 설계 확장

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- 필요 시 `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`는 읽기 전용 참고만 사용

## Docs
- `docs/execplans/active/2026-03-16-mobile-workproof-map-card.md`

## Shared
- 없음

# Contract Changes
- 외부 API/DB schema 변경 없음
- Android 내부 `WorkproofSummaryUiModel`에 사업장 지도 표시용 필드가 추가될 수 있음
- 화면 표시 문구는 PRD의 위치 검증 안내 톤에 맞춰 보강될 수 있음

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 위치 권한을 새로 요청하지 않음
- 위치정보를 단독 판단 근거처럼 보이게 만들지 않고, 근무 검증 보조 근거라는 문구를 유지

# Maintainability Notes
- 실제 지도 SDK가 없는 상태에서 억지 추상화는 만들지 않고, presentation 레벨의 지도형 카드로 한정한다.
- 위치 관련 문구와 표시 데이터는 `WorkproofUiModel.kt`에서 조립해 `WorkproofScreen.kt`는 렌더링 책임만 갖게 한다.
- `WorkproofScreen.kt`가 이미 큰 파일이므로, 지도 카드는 독립 컴포저블로 추가해 메인 구조 복잡도를 늘리지 않는다.

# Implementation Steps
1. PRD의 WorkProof 위치 요구와 현재 데모 데이터의 한계를 기준으로 표시 범위를 확정한다.
2. `WorkproofUiModel.kt`에 사업장명, 주소, 위치 안내 문구, 배지 텍스트 등 지도 카드에 필요한 표시 모델을 추가한다.
3. `WorkproofScreen.kt`의 `오늘 근무` 섹션 아래에 지도형 카드 컴포저블을 추가한다.
4. 카드 내부에 사업장명, 주소, 지도형 배경, 핀 포인트, 위치 검증 보조 안내를 배치한다.
5. 출근/퇴근 CTA와 시각적으로 충돌하지 않는지 확인하고 간격을 조정한다.

# Test Plan
- 정적 확인
  - 근무 탭 메인 화면에서 `오늘 근무` 카드 아래 지도 카드가 노출되는지 확인
  - 사업장명/주소가 현재 데모 데이터와 일치하는지 확인
  - 안내 문구가 위치 단독판단이 아닌 증빙 보조 성격으로 읽히는지 확인
- 가능하면 빌드 확인
  - `./gradlew :app:compileDebugKotlin`
  - 환경 제약 시 blocker 명시

# Review Focus
- 지도 카드가 현재 근무 탭 정보 구조를 해치지 않고 자연스럽게 들어가는가
- 위치 관련 문구가 PRD의 evidence-first 원칙과 충돌하지 않는가
- 내부 표시 모델 추가가 불필요하게 다른 탭까지 결합시키지 않는가

# Worktree Split Decision
- Single lane

표시 모델과 Compose 화면이 함께 바뀌며 범위가 작아 병렬 레인 이점이 거의 없다. 공통 DTO나 auth 규칙 변경도 없으므로 단일 레인에서 빠르게 닫는 편이 안전하다.

# Commit Plan
- 1개 커밋 기본
  - `feat: 근무 탭에 사업장 지도 카드 추가`

# Open Questions
- 없음

# Assumptions
- 사용자가 말한 `근무 탭`은 Android 앱의 `Route.WORKPROOF` 메인 화면이다.
- 현재 저장된 주소만으로도 지도형 UI를 넣는 것이 사용자 의도에 부합한다.
- 실제 지도 SDK 연동은 별도 요구가 있을 때 후속 작업으로 분리한다.
