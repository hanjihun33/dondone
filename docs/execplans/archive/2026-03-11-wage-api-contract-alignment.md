# Source Inputs
- 사용자 요청: `develop` 브랜치의 API 문서를 기준으로 현재 브랜치에서 `wage` 관련 API 문서를 정리
- 기준 PRD: `docs/DonDone_PRD_v1.5.md`
- 기준 API 문서: `docs/DonDone_P0_API_Contract_v0.md` (origin/develop)
- 참고 문서: `docs/DonDone_P0_Functional_Spec_v0.md` (origin/develop)
- 구현 기준 확인: `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/**`, `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/**`

# Goal
`develop`에 있는 P0 API 계약 문서를 현재 브랜치에 반영하고, `wage` 계약 서술을 최근 PRD 방향에 맞게 정리한다. 제품은 근로자 1차 사용자를 유지하되, 문제 해결 단계에서만 고용주가 확인/설명/정정에 참여하는 구조가 API 문서에 드러나도록 수정한다.

# In Scope
- `docs/DonDone_P0_API_Contract_v0.md` 현재 브랜치 반영
- 필요 시 `docs/DonDone_P0_Functional_Spec_v0.md`와 관련 execplan 현재 브랜치 반영
- `wage` 섹션의 목표, endpoint 설명, request/response, 상태명, 고용주 참여 조건 문구 수정
- PRD와 어긋나는 `차액 감지`, `실제 입금액 입력`, `고용주 상시 감시` 식 표현 정리

# Out of Scope
- backend 코드 수정
- DTO/entity/controller 구현 변경
- auth/authz 정책 변경
- mobile/mockup 카피 수정

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- `docs/DonDone_P0_API_Contract_v0.md`
- 필요 시 `docs/DonDone_P0_Functional_Spec_v0.md`
- `docs/execplans/active/2026-03-11-wage-api-contract-alignment.md`

## Shared
- 없음

# Contract Changes
- 구현 계약 변경 없음
- 문서상 `wage` 흐름을 `근로자 확인 우선 + 문제 발생 시 확인 요청 + 고용주 보조 참여`로 재서술
- 현재 구현이 아직 지원하지 않는 employer 상태 필드는 `후속 계약 필요` 또는 `v0 가정` 수준으로만 남긴다

# Security Notes
- `/api/wage/**`는 계속 JWT 보호 대상 기준을 유지한다
- 고용주 참여를 문서에 적더라도 현재 auth 모델이 이를 지원하지 않는다는 점을 흐리지 않는다
- `참고용 추정`, `최종 급여 확정 아님`, `법률 자문 아님` 가드레일을 유지한다

# Maintainability Notes
- PRD와 API 문서 사이에서 `급여 확인`, `급여 점검`, `Wage Shield` 용어가 다시 어긋나지 않도록 한 축으로 맞춘다
- 사용자 상태명과 내부 탐지 개념을 분리해 후속 DTO 명명 충돌을 줄인다
- 현재 구현과 미래 확장 가정을 같은 문단에 섞지 않고, `배경`, `v0 메모`, `후속`으로 나눠 적는다

# Implementation Steps
1. `origin/develop`의 API 계약 문서를 현재 브랜치에 반영한다
2. `wage` 섹션의 endpoint와 상태명을 현재 PRD 방향에 맞춰 검토한다
3. 근로자 1차 사용자를 유지하면서, 고용주 참여 조건을 `확인 요청 이후`로 제한해 문구를 수정한다
4. 차액/이상/확인 상태 용어를 정리한다
5. 관련 Functional Spec 문서가 같은 용어 축을 따라가는지 최소 점검한다

# Test Plan
- 수동 검토:
- `wage` 계약이 근로자 우선 흐름으로 읽히는지 확인
- 고용주가 상시 감시 주체처럼 읽히지 않는지 확인
- 현재 구현과 다른 부분은 `v0 메모` 또는 후속 메모로 분리됐는지 확인
- `rg -n "차액 감지|실제 입금액|고용주|확인 요청|Wage Shield"`로 용어 일관성 점검

# Review Focus
- PRD와 API 계약 문서 간 제품 논리 일치 여부
- worker-first 정체성 유지 여부
- employer 참여 범위가 과도하게 약속되지 않았는지
- 현행 구현과 문서 사이 간극이 위험하게 숨겨지지 않았는지

# Worktree Split Decision
- Single lane

API 계약 문서 import와 `wage` 서술 수정은 같은 문서 안에서 연속적으로 이뤄져야 한다. shared DTO나 auth 계약은 이번에 바꾸지 않지만, 용어 정합성이 핵심이라 병렬 분할 이점이 거의 없다.

# Commit Plan
- `docs: align wage API contract with worker-first wage confirmation flow`

# Open Questions
- 현재 v0 API 문서에 employer 참여형 상태를 어느 수준까지 문서화할지
- `wage verification` 명칭을 유지할지, `wage confirmation` 계열로 바꿀지

# Assumptions
- 사용자는 코드보다 문서 정리를 우선 원한다
- `develop`의 API 계약 문서는 현재 브랜치에 없는 기준 문서이며, 이를 바탕으로 현 브랜치 문서를 정리하는 것이 맞다
- 이번 작업은 구현 변경 없이 문서 정합성 확보가 목적이다
