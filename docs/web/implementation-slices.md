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
- 현재 web 기준 문서 인덱스:
  - `docs/web/README.md`

## 현재 작업 컨텍스트
- 현재 단계는 Slice 2 `Auth and profile foundation` 범위 고정 및 구현 착수 준비 단계다.
- 아직 앱 API 변경은 범위 밖이다.
- 이번 범위는 `invitation token contract`, `employer role/profile`, `membership authz`를 웹 전용 경계 안에서 구현 단위로 고정한다.
- 현재 backend 근거상 `UserRole = USER/ADMIN`, `Workplace.user`, `WorkProof.user` 구조를 먼저 감안해야 한다.

## 진행 상태판
| 순서 | Slice | 상태 | 마지막 결과 | 다음 작업 | 선행 문서 |
| --- | --- | --- | --- | --- | --- |
| 1 | 문서/경계 고정 | `done` | 검증 페르소나 리뷰를 통해 scope/auth/invitation/migration 경계 누락을 보완했고 기준 문서와 active execplan 정렬 방향을 확보함 | Slice 2 착수 전 employer auth/profile 최소 계약을 구현 단위로 내린다 | `employer-web-direction.md`, `employer-worker-domain-map.md` |
| 2 | Auth and profile foundation | `in_progress` | invitation token contract, employer role/profile, membership authz 범위를 웹 전용 경계로 다시 잘랐음 | employer auth/profile foundation 구현과 테스트 계획으로 내린다 | `auth-and-role-policy.md`, `shared-entity-validation.md` |
| 3 | Workplace settings | `not_started` | 미시작 | `Workplace` 설정 저장 모델과 효력 시점 규칙 확정 | `workplace-settings-contract.md`, `shared-entity-validation.md` |
| 4 | Worker directory and dashboard read-model | `not_started` | 미시작 | worker list/dashboard용 read-model 입력 소스 확정 | `employer-web-api-map.md`, `employer-worker-domain-map.md` |
| 5 | Correction request flow | `not_started` | 미시작 | 정정 요청 엔티티와 승인 반영 규칙 확정 | `correction-request-flow.md`, `shared-entity-validation.md` |
| 6 | Hardening | `not_started` | 미시작 | 테스트, 리뷰, 리스크 정리 | 관련 review note |

## 지금 기준 다음에 해야 할 일
1. `docs/execplans/active/2026-03-19-web-auth-profile-foundation.md` 기준으로 backend/web 모듈 범위를 고정한다.
2. employer invitation accept/login/profile DTO와 security rule의 최소 계약을 구현한다.
3. `EmployerProfile + EmploymentMembership` 기반 membership authz helper와 회귀 테스트를 추가한다.
4. 기존 앱 API contract가 범위 안으로 들어오지 않는지 구현 중 계속 검증한다.

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
5. 막힌 항목이 있으면 `docs/reviews/active/`의 최신 review note를 같이 본다.

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
