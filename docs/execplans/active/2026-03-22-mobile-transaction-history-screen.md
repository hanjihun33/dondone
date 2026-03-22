## Source Inputs
- 사용자 확정 요구사항:
  - 계좌/지갑 관리 페이지에서 지갑 row 탭 시 거래내역 페이지로 이동
  - 거래내역 수정 범위는 카테고리, 메모만 허용
  - 블록 탐색기 링크 제외
  - 조회 기준은 캘린더 월
  - 상세 페이지에서 tx 해시, 블록 시간, 상태 제외
- 참고 이미지:
  - `C:\Users\SSAFY\Documents\workspace\img\Transaction history_main.png`
  - `C:\Users\SSAFY\Documents\workspace\img\Transaction history_detail.png`
  - `C:\Users\SSAFY\Documents\workspace\img\Transaction history_modify.png`
- 기존 모바일 구조:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/Route.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Components.kt`

## Goal
DonDone 모바일 금융 영역에 거래내역 메인/상세/수정 화면을 추가하고, 계좌/지갑 관리에서 선택한 지갑 기준으로 거래내역 흐름을 탐색할 수 있게 만든다.

## In Scope
- Android Compose 기준 거래내역 메인 화면 추가
- Android Compose 기준 거래내역 상세 화면 추가
- Android Compose 기준 거래내역 수정 화면 추가
- `finance/account` 에서 거래내역 메인으로 이동하는 네비게이션 추가
- 로컬 데모/원격 지갑 모두에서 동작하는 거래내역 UI 모델 추가
- 카테고리/메모 수정 상태를 반영하는 세션 상태 추가
- 최소 route/navigation 테스트 보강

## Out of Scope
- 백엔드 API 추가 또는 변경
- 온체인 원본 tx 수정
- 블록 탐색기 링크 노출
- tx 해시, 블록 시간, 상태 상세 노출
- 실제 검색 서버 연동

## Affected Modules
### Backend
- 없음

### Mobile
- `app/navigation/Route.kt`
- `app/navigation/DonDoneNavGraph.kt`
- `app/navigation/ScreenChrome.kt`
- `app/session/DemoSessionViewModel.kt`
- `app/session/DemoSessionReducer.kt`
- `domain/model/DemoModels.kt`
- `data/demo/DemoSeedFactory.kt`
- `feature/finance/presentation/AccountManageScreen.kt`
- 거래내역 신규 presentation 파일들

### Docs
- `docs/execplans/active/2026-03-22-mobile-transaction-history-screen.md`

### Shared
- 없음

## Contract Changes
- 백엔드/모바일 API 계약 변경 없음
- 모바일 내부 UI 계약 추가:
  - 거래내역 목록/상세/수정용 UI model
  - 카테고리/메모 수정 저장용 세션 상태

## Security Notes
- 인증/인가 규칙 변경 없음
- 원격 지갑 거래내역은 기존 remittance 로드 상태를 읽기 전용으로 사용
- 수정 기능은 로컬 메타데이터만 갱신하며 원본 tx 데이터 변경으로 오해되지 않도록 UI 구분 유지

## Maintainability Notes
- 로컬 데모 계좌와 원격 지갑 데이터 소스가 달라서 화면 내부에서 직접 분기하지 말고 공통 거래내역 UI model로 변환해야 한다
- 거래내역 수정 상태는 송금 상태와 분리된 전용 상태로 두어 transfer flow 회귀를 줄인다
- route 문자열에 path parameter가 추가되므로 제목/child route 판별 로직은 prefix 기반으로 안전하게 보강해야 한다

## Implementation Steps
1. 거래내역 feature용 도메인/세션 상태 구조 정의
2. 로컬 데모 및 원격 remittance payload를 공통 거래내역 UI model로 매핑
3. 거래내역 메인/상세/수정 Compose 화면 구현
4. account row 탭 시 거래내역 메인으로 이동하도록 account 화면 동선 조정
5. navigation route, chrome title, nav graph 연결
6. 카테고리/메모 수정 액션과 저장 반영 연결
7. route/navigation 테스트 보강 및 최소 검증 실행

## Test Plan
- `RouteTest` 에 거래내역 child route 판별 테스트 추가
- 가능하면 Android unit test로 거래내역 UI model 변환 또는 route helper 검증 추가
- `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.app.navigation.RouteTest`

## Review Focus
- account row 탭이 기존 계좌 선택 동작과 충돌하지 않는지
- 캘린더 월 기준 탐색이 화면 상태와 맞게 유지되는지
- 상세 페이지에 제외하기로 한 기술 정보가 섞이지 않았는지
- 카테고리/메모 수정이 메인/상세 양쪽에 일관되게 반영되는지
- route prefix 처리로 기존 top bar/title 동작이 깨지지 않는지

## Worktree Split Decision
- Single lane

shared navigation, session state, finance UI model이 함께 변하므로 병렬 분리 시 충돌 위험이 높다.

## Commit Plan
- 1차: 거래내역 feature + navigation + session state
- 2차: 테스트 보강 및 문서 정리

## Open Questions
- 없음

## Assumptions
- 거래내역 수정은 원본 tx 수정이 아니라 사용자 메타데이터 수정이다
- 검색은 로컬 필터 수준으로 충분하다
- 원격 지갑 거래내역은 기존 remittance transfer summary 기반으로 표시한다
