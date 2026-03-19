# Implementation Slices

## 목적
- 웹 구현 순서를 위험도 기준으로 고정한다.
- 공유 도메인 변경이 큰 작업을 뒤섞지 않고 단계적으로 진행한다.
- 새 대화에서도 이 문서만 보면 현재 어디까지 왔고 다음에 무엇을 해야 하는지 알 수 있게 한다.

## 사용 규칙
- 각 slice는 `not_started`, `in_progress`, `blocked`, `done` 중 하나의 상태를 가진다.
- 새 대화에서 작업을 이어갈 때는 먼저 이 문서의 `진행 상태판`을 본다.
- 상태가 바뀌면 이 문서와 현재 `docs/execplans/active/` 문서를 같이 갱신한다.
- `blocked`가 되면 막힌 이유와 풀기 위한 선행조건을 한 줄로 적는다.

## 현재 기준 참조 문서
- 현재 active execplan:
  - `docs/execplans/active/2026-03-19-web-auth-profile-foundation.md`
  - `docs/execplans/active/2026-03-19-web-workplace-settings-foundation.md`
  - `docs/execplans/active/2026-03-19-web-worker-directory-dashboard-read-model.md`
  - `docs/execplans/active/2026-03-19-web-correction-request-flow.md`
- 현재 web 기준 문서 인덱스:
  - `docs/web/README.md`

## 현재 작업 컨텍스트
- 현재 단계는 Slice 2 `Auth and profile foundation` backend 구현과 회귀 보강이 끝난 상태다.
- 기존 앱 API 변경 없이 employer 전용 auth/profile 경계를 분리한 채 `EMPLOYER` role, `EmployerProfile`, `EmployerInvitationToken`, `EmploymentMembership` 기반 authz foundation을 반영했다.
- 리뷰 반영으로 invitation token hash 저장, 이메일 canonicalization 공통화, `Workplace.companyId` 기반 company-workplace binding 검증까지 완료했다.
- Slice 3 `Workplace settings` backend foundation은 완료 상태로 정리했다.
- `GET/PUT /api/employer/workplace-settings`, `EmployerAccessScope` 기반 default workplace 해석, `EmploymentMembership` 기반 영향 범위 집계, settings metadata additive 필드, `WorkProof` workplace snapshot 고정까지 반영했다.
- 설정 변경 효력은 저장 시점 이후 미래 `check-in/check-out`부터 적용하고, 기존 완료 `WorkProof`는 자동 재판정하지 않으며, 상세/PDF도 record 시점 snapshot을 우선 사용하도록 고정했다.
- Slice 4 `Worker directory and dashboard read-model` backend foundation을 시작했다.
- `GET /api/employer/workers`, `GET /api/employer/dashboard/summary`, `GET /api/employer/dashboard/attendance-board`, `EmploymentMembership` scope 조회, today/week `WorkProof` 기반 status 조합, search/status filter/pagination foundation 테스트를 추가했다.
- 현재 read-model status는 `WORKING`, `COMPLETED`, `NEEDS_REVIEW`, `NO_RECORD`로만 고정했고, 휴가/결근/지각은 canonical source가 생길 때까지 후속 범위로 남긴다.
- raw source 일관성을 위해 employer read-model에도 `recordStatus`(`CHECKED_IN/CHECKED_OUT`)와 `reflectionStatus`(`PENDING/REFLECTED/NEEDS_REVIEW`)를 함께 노출한다.
- Slice 4 web-independent read-model scope는 일단 완료로 보고, app/web 공통 정책이 필요한 worker profile source와 `late/leave/absent`는 active follow-up note에 `temporary` / `shared_policy_pending`으로 넘긴다.
- Slice 5 `Correction request flow` foundation을 시작했다.
- `GET /api/employer/correction-requests`, `GET /api/employer/correction-requests/{requestId}`, `POST /api/employer/correction-requests/{requestId}/approve`, `POST /api/employer/correction-requests/{requestId}/reject`와 `CorrectionRequest`, `CorrectionDecisionAudit`, `WorkProofAuditLog` 연동 테스트를 추가했다.

## 진행 상태판
| 순서 | Slice | 상태 | 마지막 결과 | 다음 작업 | 선행 문서 |
| --- | --- | --- | --- | --- | --- |
| 1 | 문서/경계 고정 | `done` | 검증 페르소나 리뷰를 통해 scope/auth/invitation/migration 경계 누락을 보완했고 기준 문서와 active execplan 정렬 방향을 확보함 | Slice 2 착수 전 employer auth/profile 최소 계약을 구현 단위로 내린다 | `employer-web-direction.md`, `employer-worker-domain-map.md` |
| 2 | Auth and profile foundation | `done` | `POST /api/employer-auth/invitations/accept`, `POST /api/employer-auth/login`, `GET /api/employer/profile`, `EMPLOYER` role, `EmployerProfile`, `EmployerInvitationToken`, `EmploymentMembership` authz foundation을 구현했고 리뷰 이슈와 backend 테스트를 정리함 | Slice 3 `Workplace settings` 계약과 설정 변경 효력 규칙을 고정한다 | `auth-and-role-policy.md`, `shared-entity-validation.md` |
| 3 | Workplace settings | `done` | `GET/PUT /api/employer/workplace-settings`, employer 전용 DTO/service/controller, `Workplace` settings metadata additive 필드, settings authz/validation 테스트, `WorkProof` workplace snapshot 고정, 관련 문서 정리를 완료함 | Slice 4 read-model scope와 worker list/dashboard 입력 소스를 고정한다 | `workplace-settings-contract.md`, `shared-entity-validation.md` |
| 4 | Worker directory and dashboard read-model | `done` | `GET /api/employer/workers`, `GET /api/employer/workers/{workerId}`, `GET /api/employer/dashboard/summary`, `GET /api/employer/dashboard/attendance-board` foundation, active membership/week overlap scope, `recordStatus/reflectionStatus + attendanceStatus` 조합, search/status filter/pagination backend 테스트를 추가함 | shared-policy pending 항목은 follow-up note에서 계속 추적한다 | `employer-web-api-map.md`, `employer-worker-domain-map.md` |
| 5 | Correction request flow | `in_progress` | `GET /api/employer/correction-requests`, `GET /api/employer/correction-requests/{requestId}`, `POST /api/employer/correction-requests/{requestId}/approve`, `POST /api/employer/correction-requests/{requestId}/reject`, `CorrectionRequest`, `CorrectionDecisionAudit`, `WorkProofAuditLog` 연동 foundation과 backend 테스트를 추가함 | worker-side request create, attachment/detail 표면, invalidation 규칙 같은 후속 항목을 정리한다 | `correction-request-flow.md`, `shared-entity-validation.md` |
| 6 | Hardening | `not_started` | 미시작 | 테스트, 리뷰, 리스크 정리와 prior slice follow-up 회수 | 관련 review note |

## 지금 기준 다음에 해야 할 일
1. worker direct edit flow를 correction request submit 흐름으로 옮길 범위를 정리한다.
2. correction detail attachment는 증빙자료 의미를 유지한 채 web 상세 표면을 언제 열지 결정한다.
3. employer issue queue에 correction request와 review 대상 record를 어떻게 함께 담을지 정리한다.
4. 승인 후 화면 갱신은 재조회로 유지하고, cache/event invalidation은 hardening에서 재평가한다.

## Hardening 재확인 backlog
- `docs/reviews/active/2026-03-19-web-auth-profile-followups.md`
- `docs/reviews/active/2026-03-19-web-workplace-settings-followups.md`
- `docs/reviews/active/2026-03-19-web-worker-read-model-followups.md`
- `docs/reviews/active/2026-03-19-web-correction-request-followups.md`
- 이 문서들은 Slice 2~4를 닫거나 진행하면서 의도적으로 미룬 항목을 모아둔 backlog다.
- 이 중 `temporary`, `shared_policy_pending` 라벨이 붙은 항목은 마지막 정렬 단계에서 누락 없이 `fixed / accepted risk / rescope`로 반드시 닫는다.
- Slice 6 `Hardening`에 들어가기 전에 반드시 다시 읽고, 남은 항목을 `fixed / accepted risk / rescope`로 분류한다.

## 재스코프 트리거
- 웹 요구사항을 맞추려면 기존 앱 API contract 변경이 필수로 보일 때
- 공통 엔티티 변경이 worker flow를 직접 깨기 시작할 때
- employer auth를 별도 시스템으로 분리해야만 요구사항을 맞출 수 있을 때
- company/workplace/membership 관계를 문서만으로 더 이상 정리할 수 없고 DB 설계 선결정이 필요한 때

## 새 대화에서 이어가는 방법
1. `docs/web/README.md`를 열어 문서 역할을 확인한다.
2. 이 문서의 `진행 상태판`에서 현재 `in_progress` 또는 첫 `not_started` slice를 찾는다.
3. 해당 slice의 `선행 문서`를 읽는다.
4. `현재 active execplan`을 열어 현재 세션 작업 범위를 확인한다.
5. 막힌 항목이나 미룬 작업이 있으면 `docs/reviews/active/`의 최신 review note를 같이 본다.
6. Slice 6 `Hardening`을 시작할 때는 `docs/reviews/active/2026-03-19-web-workplace-settings-followups.md`를 먼저 읽는다.
7. Slice 4를 이어갈 때는 `docs/reviews/active/2026-03-19-web-worker-read-model-followups.md`를 먼저 읽는다.

## Slice 정의

### Slice 1. 문서/경계 고정
- `docs/web/*` 초안 확정
- 웹 전용 API namespace와 역할 정책 정리
- 완료 조건
  - API 분리 원칙과 공유 도메인 검증 포인트가 문서로 고정됨
  - `docs/execplans/active/` 실행계획과 참조 관계가 맞음

### Slice 2. Auth and profile foundation
- 웹 전용 로그인/회원가입 정책
- 고용주 프로필/회사 코드 조회
- 완료 조건
  - 사업주가 웹에 로그인할 수 있는 최소 경계가 생김
  - 역할 검증 방식이 문서와 코드에서 일치함

### Slice 3. Workplace settings
- 사업장 위치/반경 조회 및 수정
- 완료 조건
  - 설정 변경이 어떤 기록에 영향을 주는지 규칙이 문서와 일치함
  - 과거 기록 재판정 여부가 명시됨

### Slice 4. Worker directory and dashboard read-model
- 근로자 목록 조회
- 대시보드 요약과 주간 근태 보드 조회
- 완료 조건
  - 웹 화면 목데이터를 API로 대체할 수 있음
  - 조회 범위가 membership 기반으로 제한됨

### Slice 5. Correction request flow
- 정정 요청 목록/상세/승인/반려
- 감사로그
- 완료 조건
  - 승인/반려가 실제 도메인 상태 전이와 연결됨
  - 승인 결과가 WorkProof와 집계에 반영됨

### Slice 6. Hardening
- 테스트 보강
- 리뷰
- 리스크 정리

## Slice별 선행 문서
| Slice | 선행 문서 |
| --- | --- |
| 1 | `employer-web-direction.md`, `employer-worker-domain-map.md` |
| 2 | `auth-and-role-policy.md`, `shared-entity-validation.md` |
| 3 | `workplace-settings-contract.md`, `shared-entity-validation.md` |
| 4 | `employer-web-api-map.md`, `employer-worker-domain-map.md` |
| 5 | `correction-request-flow.md`, `shared-entity-validation.md` |
| 6 | `docs/reviews/active/*` 관련 review note |

## 각 Slice 공통 체크
- `shared-entity-validation.md` 갱신 여부
- 앱 API 영향 없음 확인
- 권한 경계 확인
- 필요한 review note 생성 여부 확인

## 검증 게이트
### Gate A. slice 시작 전
- 필요한 기준 문서가 최신인지 확인
- 이번 slice가 앱 API 변경을 요구하는지 다시 확인

### Gate B. endpoint 계약 고정 전
- read-model인지 command인지 구분
- 권한 범위와 scope 파라미터 확인

### Gate C. 구현 후
- 기존 worker flow에 영향이 없는지 확인
- 필요한 테스트와 review note 작성 여부 판단
