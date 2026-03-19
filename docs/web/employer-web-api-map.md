# Employer Web API Map

## 2026-03-20 Addendum
- `PATCH /api/workproof/{id}` is a deprecated legacy worker endpoint and is not part of the new employer correction flow.
- After employer approve/reject, employer dashboard/workers/issues and worker wage/docs surfaces rely on the next query to re-read source-of-truth data.
- No separate cache/event invalidation contract is part of MVP Slice 5.

## 목적
- 웹 화면과 백엔드 API 요구사항을 1:1로 매핑한다.
- 구현 전에 `새로 만들 API`와 `공유 도메인 검증이 필요한 API`를 구분한다.

## 공통 원칙
- prefix 후보: `/api/employer/*`
- 웹 전용 DTO 사용
- 사업주 권한 검증 필수
- 검색/필터/페이지네이션 파라미터를 명시적으로 둔다
- 응답 envelope는 기존 백엔드 공통 포맷을 재사용할 수 있으면 재사용한다.
- company scope는 요청 파라미터가 아니라 `인증된 employer의 EmployerProfile.companyId`에서 서버가 해석한다.
- bare ID(`workerId`, `requestId`, `workplaceId`)를 받는 endpoint는 모두 target entity를 `EmploymentMembership` 기준으로 다시 검증해야 한다.
- MVP의 workplace scope는 서버가 관리하는 `defaultWorkplaceId` 또는 현재 선택 workplace로 해석한다.
- MVP에서는 company scope를 넓히는 request parameter를 두지 않는다. workplace switcher를 넣을 때만 별도 계약 갱신 후 scope parameter를 추가한다.

## 현재 웹 화면 인벤토리
| 화면 | 프론트 근거 파일 | 주요 사용자 행동 | 백엔드 성격 |
| --- | --- | --- | --- |
| Dashboard | `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx` | 주간 근태 조회, 검색, 상태 필터 | read-model |
| Workers | `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx` | 근로자 목록 조회, 상태 필터, 페이지네이션 | read-model |
| Issues | `apps/dondone-web/src/pages/issues/IssuesQueuePage.tsx` | 미처리 이슈 큐 조회, 정정 요청 승인/반려 | read-model + command |
| Settings | `apps/dondone-web/src/pages/settings/SettingsPage.tsx` | 사업장 위치/반경 조회 및 수정 | read-model + command |

## 화면별 API

### 0. Employer Auth
- 목적: employer 초대 수락과 웹 로그인 표면을 worker auth와 분리
- 후보 API
  - `POST /api/employer-auth/invitations/accept`
  - `POST /api/employer-auth/login`
- 최소 request 필드 예시
  - invitation accept: `token`, `email`, `password`, `displayName`
  - login: `email`, `password`
- 검증 포인트
  - invitation token의 1회성, 만료, 폐기 여부
  - token과 `email`, `companyId`, `EMPLOYER` role 바인딩 일치 여부
  - employer profile 비활성 또는 회사 연결 누락 계정 차단 여부

### 1. Dashboard
- 목적: 오늘 출근 현황 요약과 주간 근태 보드 조회
- 후보 API
  - `GET /api/employer/dashboard/summary`
  - `GET /api/employer/dashboard/attendance-board`
- 주요 파라미터
  - `weekStart`
  - `query`
  - `statuses`
  - `page`
  - `size`
- 검증 포인트
  - 어떤 근로자 집합을 기준으로 카운트하는지
  - 지각/결근/휴가 상태 산정 기준
  - 주간 보드 데이터가 `WorkProof`에서 바로 오지 않고 별도 read-model 조합이 필요한지
- 최소 응답 필드 예시
  - foundation summary: `activeWorkerCount`, `workingCount`, `completedCount`, `needsReviewCount`, `noRecordCount`, `asOf`
  - board: `workerId`, `name`, `role`, `avatarUrl`, `days[]`, `page`, `hasNext`
- Slice 4 foundation 메모
  - 현재 backend는 휴가/결근/지각 canonical source가 없어서 `GET /api/employer/dashboard/summary`를 `today scoped WorkProof` 기준 `WORKING/COMPLETED/NEEDS_REVIEW/NO_RECORD` 집계로 먼저 연다.
  - `leave/absent/late`는 schedule/leave/correction source가 고정될 때 별도 계약으로 다시 넓힌다.
  - `GET /api/employer/dashboard/attendance-board`는 `weekStart`가 없으면 현재 주 일요일 기준 7일 범위를 반환한다.
  - board day cell은 `recordStatus`(`CHECKED_IN/CHECKED_OUT`), `reflectionStatus`(`PENDING/REFLECTED/NEEDS_REVIEW`), `attendanceStatus`, `workedMinutes`를 함께 노출한다.
  - row filter `statuses`는 그 주간 7일 중 하나라도 일치하는 worker를 포함하는 방식으로 시작한다.
- scope 규칙
  - company는 서버가 employer profile에서 해석한다.
  - workplace는 MVP에서 서버가 `defaultWorkplaceId`를 적용한다.
  - worker 집계 대상은 해당 workplace의 활성 `EmploymentMembership`으로 한정한다.

### 2. Workers
- 목적: 소속 근로자 목록 조회, 상태 필터, 검색, 페이지네이션
- 후보 API
  - `GET /api/employer/workers`
  - `GET /api/employer/workers/{workerId}`
- 검증 포인트
  - 사번/팀/연락처/아바타 데이터 출처
  - 사업주가 조회 가능한 범위
  - 현재 출근 상태를 어느 기준 시각으로 계산하는지
- 최소 응답 필드 예시
  - `workerId`, `employeeCode`, `name`, `team`, `role`, `email`, `phone`, `avatarUrl`, `attendanceStatus`
  - pagination: `page`, `size`, `totalElements`, `totalPages`
- Slice 4 foundation 메모
  - `GET /api/employer/workers`는 현재 `User + EmploymentMembership + today scoped WorkProof`만으로 조합한다.
  - `attendanceStatus`는 `WORKING`, `COMPLETED`, `NEEDS_REVIEW`, `NO_RECORD` 네 값만 지원한다.
  - 대신 raw source 일관성을 위해 `recordStatus`(`CHECKED_IN/CHECKED_OUT`)와 `reflectionStatus`(`PENDING/REFLECTED/NEEDS_REVIEW`)를 함께 노출하고, `attendanceStatus`는 그 조합에서 파생한다.
  - `employeeCode`, `team`, `role`, `phone`, `avatarUrl`는 canonical profile source가 없으면 `null`을 허용한다.
  - query는 `name/email` 기준 case-insensitive 검색으로 시작하고, status filter/pagination은 foundation 단계에서 service 조합으로 지원한다.
  - `GET /api/employer/workers/{workerId}`는 detail foundation으로 `summary 필드 + membershipEffectiveFrom/effectiveTo + latestRecord + recentDays(최근 7일)`를 반환한다.
  - `latestRecord`는 현재 source-of-truth가 있는 범위만 포함한다: `workDate`, `clockInAt`, `clockOutAt`, `recordStatus`, `reflectionStatus`, `attendanceStatus`, `workedMinutes`, `needsReview`, `clockOutOutsideAllowedRadius`, `edited`, workplace snapshot, location labels.
- scope 규칙
  - `workerId` detail 조회 전 서버는 해당 worker의 활성 membership이 employer scope 안인지 재검증한다.
  - 단순 role 통과만으로 detail 조회를 허용하지 않는다.

### 3. Correction Requests
- 목적: 근로자 정정 요청 목록 조회 및 승인/반려 처리
- 후보 API
  - `GET /api/employer/issues`
  - `GET /api/employer/correction-requests`
  - `GET /api/employer/correction-requests/{requestId}`
  - `POST /api/employer/correction-requests/{requestId}/approve`
  - `POST /api/employer/correction-requests/{requestId}/reject`
- 검증 포인트
  - 승인 후 실제 WorkProof 반영 규칙
  - 처리자, 처리 시각, 감사로그 저장 방식
  - 이미 처리된 요청 재처리 방지 규칙
  - worker가 직접 수정하지 않고 정정 요청 제출로 전환될 때 app/web contract를 어떻게 맞출지
- 최소 응답 필드 예시
  - issue queue row: `itemType`, `issueStatus`, `requestId`, `workProofId`, `workerId`, `workerName`, `workDate`, `clockInAt`, `clockOutAt`, `requestedClockInAt`, `requestedClockOutAt`, `reason`, `reviewReasonCode`, `raisedAt`
  - `requestId`, `workerId`, `workerName`, `workDate`, `originalCheckIn`, `requestedCheckIn`, `reason`, `requestedAt`, `status`
  - detail: `decisionBy`, `decisionAt`, `decisionMemo`, `rejectReasonCode`, `attachmentCount`, `attachments[]`
- command request 예시 필드
  - approve: `decisionMemo`
  - reject: `decisionMemo`, `rejectReasonCode`
- Slice 5 foundation 메모
  - `GET /api/employer/issues`는 미처리 이슈 큐 read-model로 열고, `PENDING` correction request와 `NEEDS_REVIEW` WorkProof를 `itemType`으로 함께 노출한다.
  - 현재 backend는 employer-side issue queue read-model과 correction request history/command를 함께 연다: `GET /api/employer/issues`, `GET /api/employer/correction-requests`, `GET /api/employer/correction-requests/{requestId}`, `POST /api/employer/correction-requests/{requestId}/approve`, `POST /api/employer/correction-requests/{requestId}/reject`
  - queue filter는 `query`(worker name/email, reason), `statuses`, `page`, `size` foundation으로 시작한다.
  - detail은 `attachmentCount`와 함께 attachment metadata(`type`, `fileName`)를 노출하고, raw metadata json은 request/WorkProof 내부 보존 용도로 유지한다.
  - worker가 정정 요청을 제출할 때는 변경 내용, 사유, 증빙자료를 함께 보내고 employer가 승인/반려하는 승인형 흐름을 shared policy로 본다.
  - web detail page가 생기면 attachment metadata를 바로 붙일 수 있게 backend는 request/WorkProof에 attachment metadata json을 유지하되, storage path나 download URL은 아직 열지 않는다.
  - approve는 `CorrectionRequest.status` 변경, `WorkProof.updateTimes(...)`, `WorkProofAuditLog`, `CorrectionDecisionAudit`를 한 transaction에서 처리한다.
  - reject는 `WorkProof`를 수정하지 않고 request status와 `CorrectionDecisionAudit`만 기록한다.
  - employer issue queue는 correction request와 review가 필요한 record를 모두 담는 방향으로 확장 가능해야 한다.
  - correction request history/command는 `/api/employer/correction-requests/*`에 남기고, employer issue queue는 `/api/employer/issues`에서 action queue read-model로 분리한다.
  - 기존 worker direct edit endpoint `PATCH /api/workproof/{id}`는 deprecated legacy surface로 유지하고, 신규 client는 correction request submit flow를 우선 사용한다.
- scope 규칙
  - `requestId`만으로 처리하지 않고, 대상 request snapshot의 `companyId/workplaceId`가 현재 employer scope와 일치하는지 다시 검증한다.
  - scope 불일치 시 `403 FORBIDDEN`으로 처리한다.

### 4. Workplace Settings
- 목적: 출근 인정 위치와 반경 설정 조회/수정
- 후보 API
  - `GET /api/employer/workplace-settings`
  - `PUT /api/employer/workplace-settings`
- 검증 포인트
  - 반경 변경이 기존/미래 출퇴근 판정에 미치는 영향
  - 다중 사업장 지원 여부
  - 상세 주소 메모가 단순 표시용인지 판정에 쓰이는지
- 최소 응답 필드 예시
  - `workplaceId`, `name`, `address`, `detailAddress`, `latitude`, `longitude`, `allowedRadiusMeters`, `effectiveFrom`
- command request 예시 필드
  - `address`
  - `detailAddress`
  - `latitude`
  - `longitude`
  - `allowedRadiusMeters`
- scope 규칙
  - MVP에서는 `GET/PUT /api/employer/workplace-settings`가 employer의 `defaultWorkplaceId` 1건만 대상으로 한다.
  - 다중 workplace를 열기 전까지 client가 임의 `workplaceId`를 넘겨 대상 사업장을 바꾸지 않는다.

### 5. Employer Profile
- 목적: 회사 코드, 연결 근로자 수, 사업주 기본 정보 표시
- 후보 API
  - `GET /api/employer/profile`
- 최소 응답 필드 예시
  - `employerId`, `name`, `companyId`, `companyName`, `companyCode`, `connectedWorkerCount`, `defaultWorkplaceName`

## API 분류
### Read-model
- `GET /api/employer/issues`
- `GET /api/employer/dashboard/summary`
- `GET /api/employer/dashboard/attendance-board`
- `GET /api/employer/workers`
- `GET /api/employer/workers/{workerId}`
- `GET /api/employer/correction-requests`
- `GET /api/employer/correction-requests/{requestId}`
- `GET /api/employer/workplace-settings`
- `GET /api/employer/profile`

### Command
- `POST /api/employer/correction-requests/{requestId}/approve`
- `POST /api/employer/correction-requests/{requestId}/reject`
- `PUT /api/employer/workplace-settings`

## 공통 에러 케이스
- 인증 실패
- 권한 없음
- 조회 대상 없음
- 소속 불일치
- 이미 처리된 정정 요청 재처리
- 잘못된 사업장 설정 입력
- 초대 토큰 만료/폐기/재사용

## 구현 우선순위 메모
- `Workplace Settings`와 `Correction Requests`는 shared domain 영향이 커서, 구현 전 각 전용 계약 문서를 먼저 확정한다.
- `Dashboard`와 `Workers`는 그 다음 read-model 구현 대상으로 본다.

## 현재 백엔드와의 경계 메모
- 기존 `/api/workproof/*`, `/api/wage/*`는 worker 중심 API로 유지한다.
- employer web은 기존 endpoint를 억지로 조합하지 않고 `/api/employer/*` read-model을 별도 도입한다.
- 단, 내부 계산 로직은 재사용 가능한 부분만 선택적으로 공유한다.

## 구현 전 체크
- 실제로 새로운 top-level 엔드포인트가 필요한지
- 기존 앱 API를 억지로 조합하려는 부분이 없는지
- 각 API가 read-model인지 command인지 구분이 되는지
- API가 company/workplace scope를 명확히 가진 상태인지
- employer auth 표면이 `auth-and-role-policy.md`와 일치하는지
