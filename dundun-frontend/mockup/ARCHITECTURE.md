# Mockup Architecture (Android Migration Ready)

## 목적
`mockup.html`을 Android Native(Compose)로 전환할 때, 상태/네비게이션/도메인 계산을 분리해 이전 비용을 줄이기 위한 구조입니다.

## 현재 파일 구조
- `mockup.html`
  - UI 마크업 전용
  - 인라인 스크립트 제거, 외부 JS 조합으로 실행
- `mockup.boot.js`
  - 앱 설정(`mainTabs`)
  - 다국어 사전(`translations`)
  - 시드 상태 팩토리(`createInitialState`)
  - Android 매핑 메타(`architecture.routes`, `architecture.modules`, `architecture.scope`)
- `mockup.domain.js`
  - 포맷/다국어 헬퍼(`formatKRW`, `tr`)
  - 급여/근무/미리받기/이자 계산 로직(`compute*`)
  - 앱 상태 기반 도메인 파생값 계산 책임
- `mockup.navigation.js`
  - 오버레이 열기/닫기
  - 탭 전환, 하단 네비게이션/헤더 표시 제어
  - 화면 이동 상태(`state.ui.currentTab`) 동기화
- `mockup.app.js`
  - 화면 렌더/이벤트 핸들러 조립
  - 도메인/네비게이션 모듈 결합
  - HTML `onclick`과 연결되는 엔트리 함수 제공

## Android 매핑
- `home` -> `feature:home`
- `workproof` -> `feature:workproof`
- `send` -> `feature:finance`
- `transfer` -> `feature:remittance`
- `manage` -> `feature:account-manage`
- `wage` -> `feature:wage`
- `vault` -> `feature:menu`

## 리팩토링 원칙
1. 시드 데이터/문구/탭 설정은 `boot`로 분리
2. 계산/파생값은 `domain`으로 분리하고 UI 렌더에서 재사용
3. 탭/오버레이 이동 규칙은 `navigation`으로 분리
4. UI 로직에서는 문자열 route 하드코딩 대신 `Route` 상수 사용
5. P0/P1 범위는 `architecture.scope`로 분리 관리

## 다음 단계 권장
1. `renderHome`/`renderSend`/`renderWage`를 feature 파일로 추가 분리
2. `state` 변경 함수를 `actions` 레이어로 분리해 UI에서 직접 변경 최소화
3. Android 전환 시 `domain` 함수 시그니처를 ViewModel 단위로 1:1 대응
