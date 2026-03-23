# 2026-03-23 Employer Review Confirm

## Source Inputs
- 사용자 요청: `검토 완료`부터 추가
- 기존 employer issue read API 구현
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/EmployerIssueReadModelController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerIssueReadModelService.java`
- 기존 correction request command 패턴
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/EmployerCorrectionRequestController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerCorrectionRequestService.java`
- issue queue web 구현
  - `apps/dondone-web/src/pages/issues/IssuesQueuePage.tsx`
  - `apps/dondone-web/src/pages/issues/components/IssueQueueList.tsx`
  - `apps/dondone-web/src/shared/api/employer.ts`

## Goal
- 고용주가 `REVIEW_REQUIRED_RECORD`를 상세 확인 후 `검토 완료`로 처리할 수 있게 한다.
- 처리 후 해당 기록은 issue queue에서 빠지고, 사실 데이터(반경 밖 퇴근 여부 등)는 유지한다.

## In Scope
- employer backend command endpoint 추가
- `WorkProof` review confirm 도메인 메서드 추가
- employer web issues 화면에 `검토 완료` 액션 추가
- backend integration test 및 web 타입 체크

## Out of Scope
- `검토 반려` 액션
- review memo / audit 테이블 확장
- worker 앱 후속 액션
- `NEEDS_REVIEW` 생성 조건 확장

## Affected Modules
### Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/*`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/*`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/dto/response/*`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/EmployerIssueReadModelIntegrationTest.java`

### Mobile
- 변경 없음

### Docs
- 본 execplan

### Shared
- `apps/dondone-web/src/shared/api/employer.ts`

## Contract Changes
- 신규 endpoint 추가
  - `POST /api/employer/issues/review-records/{workProofId}/confirm`
- response 최소안
  - `workProofId`
  - `reflectionStatus`
  - `confirmedAt`
- 기존 read endpoint/DTO는 유지

## Security Notes
- employer 인증 필수
- `EmployerAccessScopeService` 기준 `defaultWorkplaceId` scope 강제
- `NEEDS_REVIEW` 상태인 work proof만 confirm 가능해야 함
- correction request command와 권한/경로를 섞지 않음

## Maintainability Notes
- read-model service에 상태 변경 로직을 섞지 않고 command service를 분리한다.
- `WorkProof`의 review confirm 규칙은 엔티티 메서드로 캡슐화한다.
- web은 confirm 성공 후 로컬 부분 업데이트보다 `reloadCollections()` 재사용으로 단순화한다.

## Implementation Steps
1. `WorkProof.confirmReview()` 추가
2. employer issue command controller/service 및 response DTO 추가
3. scoped `NEEDS_REVIEW` record만 confirm하도록 backend 구현
4. backend integration test에 confirm 성공/재조회 제외/상세 404 회귀 추가
5. web employer API에 confirm 함수 추가
6. issue queue UI에서 review item에만 `검토 완료` 버튼 추가
7. confirm 성공 시 목록 재조회 및 에러 상태 정리

## Test Plan
- backend
  - `EmployerIssueReadModelIntegrationTest`
    - confirm 성공
    - confirm 후 queue 제외
    - confirm 후 review detail 404
    - worker token 또는 out-of-scope employer 차단
- web
  - `tsc -b`

## Review Focus
- `NEEDS_REVIEW -> REFLECTED` 전이만 허용되는지
- confirm 후 issue queue와 상세 조회 결과가 즉시 정렬되는지
- review record action이 correction request 흐름에 영향 없는지
- web에서 correction request와 review record 액션이 혼동되지 않는지

## Worktree Split Decision
- Single lane

공유 DTO와 employer issues 화면의 공통 상태가 같이 움직여서 backend/web을 분리하면 merge 리스크가 높다. 이번 작업은 단일 lane으로 처리한다.

## Commit Plan
- `feat: 검토 필요 기록 확인 완료 추가`
- 필요 시 web 문구/상태 정리는 별도 `refactor`로 분리

## Open Questions
- `검토 완료` 시 response를 detail로 넓힐지 최소 DTO로 둘지

## Assumptions
- 현재 MVP에서는 `검토 완료`만 제공하고 `반려`는 후속이다.
- confirm 후 해당 record는 employer issue detail 조회 대상에서 제외되어 404가 나도 괜찮다.
