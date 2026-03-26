# 2026-03-25 프로젝트 요구사항 명세서 작성

## Source Inputs
- `AGENTS.md` (요구사항 확인 항목, 문서화/보안/테스트 기준)
- `docs/DonDone_PRD_v1.5.md` (제품 목표, P0 범위, 상세 기능 요구사항)
- `docs/DonDone_P0_Functional_Spec_v0.md` (P0 기능 정리 방식과 용어 기준)
- `docs/DonDone_P0_API_Contract_v0.md` (API 계약 용어와 도메인 경계)
- `docs/DonDone_API_Spec_Implemented_v1.md` (구현 기준 API 카탈로그 구조)
- `prd-breakdown` 스킬 기반 범위 정리 결과 (P0 우선, evidence-first/testnet 제약 유지)

## Goal
`PRD v1.5`를 기준으로 팀이 바로 합의/검토에 사용할 수 있는 `프로젝트 요구사항 명세서` 초안(v1)을 작성한다. 문서는 기능/보안/비기능/수용 기준을 한 문서에서 확인 가능해야 한다.

## In Scope
- `docs/DonDone_Project_Requirements_Spec_v1.md` 신규 작성
- P0 중심 기능 요구사항 정리
- 포함/제외 범위 명시
- 기능별 수용 기준(acceptance criteria) 정리
- 보안/비기능 요구사항과 제약 조건 명시
- PRD 근거 섹션 매핑(추적성) 추가

## Out of Scope
- 백엔드/모바일 코드 변경
- DB 스키마/DTO/API 실제 계약 변경
- OpenAPI 산출물 생성
- P1 세부 요구사항 확장

## Affected Modules
### Backend
- 코드 영향 없음
- 문서상 요구 기준으로 `auth`, `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault` 모듈 경계만 명시

### Mobile
- 코드 영향 없음
- 상태 모델(loading/empty/error/success)과 디스클레이머 노출 요구사항만 명시

### Docs
- `docs/DonDone_Project_Requirements_Spec_v1.md` 신규
- `docs/execplans/active/2026-03-25-project-requirements-spec.md` 신규

### Shared
- 제품 제약 공통 규칙 유지:
  - `testnet/demo only`
  - `Wage = anomaly detection + evidence-first`
  - `Copilot = facts-only`

## Contract Changes
- 런타임 계약 변경 없음
- 문서 계약 정렬만 수행:
  - 상위 요구사항 문서(요구 기준)
  - 기존 API 계약 문서/구현 카탈로그(세부 계약)

## Security Notes
- 공개 경로 최소화 원칙 유지(`signup`, `login`, `/health`, Swagger)
- 보호 API JWT 필수, 타인 리소스 은닉 원칙(필요 시 `404`) 유지
- 토큰/개인정보/민감 키를 문서 예시에 하드코딩하지 않음
- Remittance/Vault는 실자금이 아닌 데모/테스트넷으로 제한

## Maintainability Notes
- 이 문서는 상위 요구사항으로 유지하고 endpoint 필드 상세는 기존 API 문서에 위임한다.
- PRD와 중복되는 긴 설명을 복제하지 않고, 구현 검토에 필요한 규칙/수용기준 중심으로 요약한다.
- 요구 ID(`FR-*`, `NFR-*`)를 부여해 후속 테스트/리뷰 문서 연결성을 확보한다.

## Implementation Steps
1. PRD 기준 P0 범위/제약/필수 문구를 추출한다.
2. 요구사항 확인 표로 기대 동작/범위/계약/보안/비기능 조건을 고정한다.
3. 기능 요구사항을 도메인별로 재구성하고 요구 ID 및 수용 기준을 작성한다.
4. 비기능/보안/계약/제외 범위를 정리한다.
5. PRD 추적성 매트릭스와 검토 체크리스트를 붙여 초안을 완성한다.

## Test Plan
- 문서 작업이므로 빌드 테스트는 없음
- 수동 검증:
  - P0 포함 도메인 누락 여부
  - testnet/demo 제약 반영 여부
  - Wage evidence-first 표현 유지 여부
  - 보안 요구(JWT/리소스 격리) 누락 여부

## Review Focus
- PRD와의 범위 불일치(포함/제외) 여부
- 요구사항이 구현 계약 문서와 충돌하는지 여부
- 보안/비기능 요구사항이 검증 가능한 문장인지 여부
- 데모 제약을 실서비스로 오해할 수 있는 표현 존재 여부

## Worktree Split Decision
- `Single lane`
- 이유: 이번 작업은 단일 문서 작성 중심이며 공유 계약/보안 용어를 동시에 다뤄 병렬 분할 이점이 낮고 문서 일관성이 더 중요하다.

## Commit Plan
1. `docs: add project requirements spec v1 draft`
2. `docs: add execplan for requirements spec drafting`

## Open Questions
- 이 문서를 P0 한정 문서로 유지할지, P1 요구사항까지 통합 확장할지 여부
- 수용 기준을 데모 시연 기준 중심으로 둘지, QA 테스트 케이스 수준까지 확장할지 여부
- 한국어 단일본 유지 여부(영문 병기 필요성)

## Assumptions
- 사용자 요청의 "프로젝트 요구사항 명세서"는 DonDone 전체가 아닌 `PRD v1.5 P0` 기준 초안을 의미한다.
- 현재 팀의 구현 계약 상세는 기존 API 문서가 소유하고, 본 문서는 상위 요구 기준을 소유한다.
- 이번 변경은 문서 추가만 포함하며 코드/계약 런타임 동작에는 영향을 주지 않는다.
