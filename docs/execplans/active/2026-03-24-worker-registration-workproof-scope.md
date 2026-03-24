# 2026-03-24 Worker Registration WorkProof Scope

## Source Inputs
- 사용자 재현: 근로자 등록 코드 redeem 성공 토스트 후 근무 화면 변화 없음
- [WorkerCompanyRegistrationService.java](/C:/c202/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/service/WorkerCompanyRegistrationService.java)
- [WorkProofLane1Service.java](/C:/c202/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java)
- [BackendWorkproofRepository.kt](/C:/c202/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/workproof/BackendWorkproofRepository.kt)
- dev PostgreSQL 확인 결과
  - `employment_memberships`에 redeem worker membership 생성됨
  - `work_contracts`는 `workplace_id=1`만 active
  - worker가 속한 `workplace_id=2`는 active contract 없음

## Goal
근로자 등록 코드 redeem 후 worker 앱의 WorkProof 로딩이 실제 소속 근무지와 active contract를 기준으로 동작하도록 backend scope와 demo seed를 정렬한다.

## In Scope
- backend WorkProof workplace/contract 조회를 worker membership 기준으로 확장
- demo employer seed의 등록 대상 workplace에 active contract 추가
- worker registration 이후 workplace/contract/workproof 조회 회귀 테스트 추가

## Out of Scope
- 모바일 fallback 제거
- worker UI 레이아웃 변경
- wage/remittance/vault membership scope 전면 정리
- employer issues 정책/문구 재정리

## Affected Modules
### Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/repo/WorkplaceRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/repo/WorkContractRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/repo/EmploymentMembershipRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/bootstrap/DevEmployerInitializer.java`
- 관련 integration test 파일

### Mobile
- 계약/응답 shape 변경이 없다면 직접 수정 없음
- backend 수정 후 existing mobile flow가 의도대로 작동하는지만 확인

### Docs
- 본 execplan 문서

### Shared
- auth role/JWT 규칙 변경 없음
- worker registration contract shape 유지

## Contract Changes
- `GET /api/workproof/workplaces`는 worker가 직접 생성한 근무지뿐 아니라 active membership로 접근 가능한 근무지를 포함한다.
- `GET /api/workproof/contracts/current`와 check-in/check-out/create contract의 workplace 접근 기준을 worker membership까지 확장한다.
- response DTO shape 자체는 유지한다.

## Security Notes
- worker는 여전히 자기 membership scope 내 workplace만 접근 가능해야 한다.
- employer-owned workplace를 membership 없는 worker가 직접 조회/출퇴근할 수 없도록 scope 검증을 유지한다.
- authn/authz rule, JWT, exposed path는 변경하지 않는다.

## Maintainability Notes
- 기존 `workplace.userId` 기반 lane1 가정과 `employment_membership` 기반 worker scope가 충돌하므로, scope 판단 로직을 service 내부에서 한 군데로 모아 중복 조건이 퍼지지 않게 해야 한다.
- demo seed는 worker membership, active contract, workproof sample이 같은 workplace를 바라보도록 맞춰야 한다.
- 모바일 문제를 backend에서 우회하지 말고 source-of-truth를 backend scope로 고정한다.

## Implementation Steps
1. `WorkProofLane1Service`의 selectable workplace, owned workplace, active contract 조회를 membership-aware helper로 재구성한다.
2. 필요한 repository query를 추가해 active membership workplace 목록과 membership-scoped workplace 접근을 조회한다.
3. demo employer seed에서 worker들이 속한 workplace에 active contract를 생성하고 sample workproof가 해당 contract를 바라보게 보강한다.
4. worker registration redeem 후 worker가 workproof workplace/current contract를 조회할 수 있는 integration test를 추가한다.
5. demo seed reset과 기존 employer flow 회귀가 깨지지 않는지 targeted verification을 수행한다.

## Test Plan
- backend integration
  - worker registration redeem 후 `GET /api/workproof/workplaces`
  - worker registration redeem 후 `GET /api/workproof/contracts/current`
  - seeded worker의 today workproof 조회/출퇴근 관련 regression
- compile/test 명령
  - `.\gradlew.bat test --tests ...`
  - 필요 시 `compileJava`

## Review Focus
- membership 없는 worker가 employer workplace에 접근하지 못하는지
- active contract scope가 workplace owner 기준으로만 남아 있지 않은지
- demo seed가 `workplace -> contract -> workproof` 연결을 일관되게 가지는지
- 기존 employer seed reset/reseed 경로가 다시 깨지지 않는지

## Worktree Split Decision
- Single lane

auth/workproof scope, shared seed, integration test가 함께 움직이고 DTO/접근 규칙이 동시에 바뀌므로 병렬 분리가 안전하지 않다.

## Commit Plan
1. `feat(backend): allow worker workproof scope via membership`
2. `test(backend): cover worker registration workproof access`

## Open Questions
- worker가 membership으로 접근 가능한 여러 workplace를 가질 때 lane1 선택 우선순위를 어떻게 둘지
- worker가 직접 workplace를 만드는 임시 흐름을 유지할지 후속에서 제거할지

## Assumptions
- P0 기준 worker WorkProof는 소속 workplace 1개를 우선 사용하는 것으로 본다.
- 현재 문제 해결에는 active membership workplace 우선 선택만으로 충분하다.
- mobile은 backend contract shape가 유지되면 추가 코드 변경 없이 동작해야 한다.
