## Source Inputs
- 사용자 요구사항: 관리자 웹페이지를 `미리받기`와 `회사등록` 두 축으로 재구성
- 코드 탐색: `apps/dondone-web/src/pages/admin/AdminPage.tsx`
- 코드 탐색: `apps/dondone-web/src/pages/admin/components/AdminAdvanceRequestSection.tsx`
- 코드 탐색: `apps/dondone-web/src/shared/styles/global.css`
- API 계약 확인: `apps/dondone-web/src/shared/api/admin.ts`
- 백엔드 DTO 확인: `apps/dondone-backend/src/main/java/com/workproofpay/backend/admin/api/dto/response/AdminAdvanceRequestItemResponse.java`

## Goal
관리자 웹페이지의 정보 구조를 `미리받기`와 `회사등록` 두 섹션으로 재편하고, 회사 등록/조회 플로우를 모달 기반으로 단순화하며, 미리받기 표의 가독성과 필터링을 개선한다.

## In Scope
- 관리자 페이지 내부 IA를 2개 섹션으로 재구성
- 좌측 관리자 전용 네비게이션을 `미리받기`, `회사등록` 2개 항목으로 변경
- 미리받기 섹션 제목/부제목/회사별 필터/상태 셀 레이아웃 조정
- 등록된 회사 테이블 컬럼 재구성
- 회사 등록 폼을 모달로 이동
- 회사 코드 조회를 테이블 내부 즉시 조회 방식으로 변경
- 회사 상세 하단 박스를 제거하고 필요한 정보를 기존 테이블 행 내부로 흡수

## Out of Scope
- 백엔드 엔드포인트 추가/수정
- 관리자 advance 응답에 없는 새 데이터(예: 지갑주소) 노출
- 모바일/백엔드 화면 및 계약 변경
- 회사 등록 정책/검증 규칙 변경

## Affected Modules
### Backend
- 없음. 기존 admin API 계약 유지

### Mobile
- 없음

### Docs
- `docs/execplans/active/2026-03-25-admin-web-ia-split.md`

### Shared
- `apps/dondone-web/src/app/AppShell.tsx`
- `apps/dondone-web/src/pages/admin/AdminPage.tsx`
- `apps/dondone-web/src/pages/admin/components/AdminAdvanceRequestSection.tsx`
- `apps/dondone-web/src/shared/styles/global.css`

## Contract Changes
- API/DTO 계약 변경 없음
- 프론트 내부 상태만 추가 예정
- `지갑주소 hover` 요구는 현재 계약상 주소 데이터가 없어 직접 구현 불가

## Security Notes
- 인증/인가 경로 변경 없음
- 관리자 전용 화면 구조만 조정
- 코드 조회 동작은 기존 admin token 기반 API를 그대로 사용

## Maintainability Notes
- `AdminPage.tsx`가 이미 회사 등록, 코드 조회, 상세 토글을 함께 보유하므로 추가 분기는 작은 지역 컴포넌트/상태로 제한한다
- 스타일은 기존 `.admin-*` 네이밍 안에서 확장해 전역 CSS 결합도를 늘리지 않는다
- 회사 행 확장 정보는 별도 하단 섹션 대신 행 내부 렌더링으로 합쳐 상태 흐름을 단순화한다

## Implementation Steps
1. 관리자 좌측 네비게이션 구성을 `미리받기`, `회사등록` 2개로 재정의하고 앵커/섹션 이동 방식을 정한다
2. `AdminPage.tsx`에서 섹션 순서를 `미리받기 -> 회사등록`으로 재구성한다
3. 회사 등록 폼을 모달로 이동하고 등록된 회사 헤더 우측 `회사 추가` 버튼으로 연다
4. 등록된 회사 테이블에서 불필요한 상태 컬럼을 제거하고, 코드 조회/고용주 상세 데이터를 테이블 내부에 재배치한다
5. `AdminAdvanceRequestSection.tsx`에 회사별 필터와 제목/상태 표시 레이아웃을 반영한다
6. `global.css`에 신규 섹션 탭/모달/tooltip/행 확장 스타일을 추가한다

## Test Plan
- `npm.cmd run build`로 타입 체크 및 번들 빌드 시도
- `/admin`에서 다음 수동 확인
- 좌측 메뉴 `미리받기`/`회사등록` 이동
- 회사별 필터 전환
- 상태 셀 너비와 hover 텍스트 표시
- 회사 추가 모달 열기/닫기/등록 성공
- 회사 코드 즉시 조회
- 회사 상세 정보 행 내부 확장/축소

## Review Focus
- 관리자 화면 구조가 요구한 두 섹션으로 충분히 분리되었는지
- 회사 등록 모달 전환 후 기존 생성/코드 발급 플로우가 유지되는지
- 테이블 컬럼 축소 후 필요한 운영 정보가 손실되지 않았는지
- 주소 미제공 계약 제약을 UI가 오해 없이 드러내는지

## Worktree Split Decision
- Single lane

관리자 화면은 `AdminPage.tsx`, `AdminAdvanceRequestSection.tsx`, 전역 CSS가 동시에 움직이며 동일한 화면 계약을 공유한다. 공유 상태와 레이아웃 영향이 커서 병렬 분리 이점보다 충돌 위험이 크다.

## Commit Plan
- 1개 커밋: `feat: 관리자 웹 IA를 미리받기와 회사등록 중심으로 재구성`

## Open Questions
- 사용자가 말한 `지갑주소 hover`는 실제 지갑주소 필드 추가를 원하는지, 아니면 현재 상태 식별자 전체 노출 의도인지 불명확하다
- 좌측 메뉴 2개가 라우트 분리인지, 동일 페이지 내 섹션 점프인지 명시되지 않았다

## Assumptions
- 이번 작업은 동일 `/admin` 페이지 안의 섹션 분리와 좌측 메뉴 라벨/앵커 변경으로 처리한다
- 지갑주소 데이터는 계약에 없으므로, 현재 노출 가능한 상태 식별자(`tx hash` 등)에 hover 전체 텍스트를 제공하는 선에서 처리한다
- 회사별 필터는 별도 API 없이 현재 응답의 `companyName` 기준 클라이언트 필터로 처리한다
