# Source Inputs
- Root guidance: `AGENTS.md`
- Private context:
  - `.private/kwanwoo/README.md`
  - `.private/kwanwoo/context/CURRENT.md`
  - `.private/kwanwoo/context/NEXT.md`
  - `.private/kwanwoo/decisions/DECISIONS.md`
  - `.private/kwanwoo/features/workproof.md`
  - `.private/kwanwoo/features/wage.md`
- Shared product inputs:
  - `docs/DonDone_PRD_v1.5.md`
  - `docs/DonDone_P0_API_Contract_v0.md`

# Goal
PRD v1.5와 P0 API 계약 초안 v0의 공통 범위를 기준으로, 팀이 구현 착수 전에 읽을 수 있는 `DonDone` P0 기능 명세 초안을 작성한다. 기능 명세는 API 초안보다 한 단계 상위에서 기능 의도, 포함/제외 범위, 핵심 흐름, 주요 규칙, API 매핑, v0 가정, 열린 질문을 정리해야 한다.

# In Scope
- 새 공유 문서 추가:
  - `docs/DonDone_P0_Functional_Spec_v0.md`
- 공통 범위 확정:
  - `auth`, `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault`
- 공통 문서 원칙 반영:
  - PRD와 API 초안의 교집합 범위 유지
  - `확정 / v0 가정 / 구현 중 조정 가능` 경계 드러내기
  - PRD에는 있으나 이번 API 초안에서 의도적으로 제외된 항목 메모 남기기
- 세션 마감 동기화:
  - `.private/kwanwoo/context/CURRENT.md`
  - `.private/kwanwoo/context/NEXT.md`
  - 필요 시 `.private/kwanwoo/logs/`

# Out of Scope
- API 계약 자체 변경
- backend/mobile 구현 변경
- Swagger, DTO, validation 상세 설계
- `home`, `copilot`, `demo time travel`, `P1` 범위 명세

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- `docs/DonDone_P0_Functional_Spec_v0.md`
- `docs/execplans/active/2026-03-10-p0-functional-spec-draft.md`

## Private Context
- `.private/kwanwoo/context/CURRENT.md`
- `.private/kwanwoo/context/NEXT.md`
- `.private/kwanwoo/logs/2026-03-10.md` (파일이 있거나 새로 만들 필요가 있을 때만)

# Requirement Clarification
- expected behavior:
  - API 계약서를 반복해서 읽지 않아도 기능 범위와 흐름을 이해할 수 있는 상위 기능 명세가 필요하다.
- exact scope:
  - P0 공통 범위는 `auth`, `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault`다.
- contract changes:
  - 이번 작업은 새 기능 명세 문서 작성이며 API/DTO/DB 스키마 변경은 하지 않는다.
- security impact:
  - 문서에는 기존 JWT 보호 원칙, 타인 리소스 은닉, testnet/demo 한계를 그대로 반영한다.
- non-functional impact:
  - 문서 생성/송금 비동기 처리, evidence-first UX, backward compatibility 관점의 현재 가정을 명시한다.

# Security Notes
- auth 공개 경로는 기존 API 초안 기준(`signup`, `login`, `/health`, Swagger)만 유지한다.
- 나머지 기능은 JWT 보호 대상이라는 점을 기능 명세에도 명시한다.
- Wage 결과는 법률/재무 최종 판단이 아니라 anomaly detection + evidence-first라는 PRD 제약을 유지한다.
- Advance, Remittance, Vault는 실거래가 아니라 demo/testnet 시뮬레이션이라는 한계를 반복 명시한다.

# Maintainability Notes
- 기능 명세는 API 표 필드를 다시 복제하지 않고, 기능-흐름-규칙 중심으로 정리한다.
- 각 도메인 섹션은 동일한 템플릿을 사용한다:
  - `목표 / 범위 / 범위 제외 / 핵심 흐름 / 주요 규칙 / API 매핑 / v0 가정 / 열린 질문`
- 문서 전반의 용어는 현재 공유 문서 표현과 맞춘다:
  - `배경`, `v0 메모`, `확장 메모`, `self-contained`

# Implementation Steps
1. PRD와 API 초안의 공통 범위와 제외 범위를 확정한다.
2. 새 기능 명세 문서의 공통 템플릿과 상위 섹션 구조를 작성한다.
3. 도메인별 기능 명세를 PRD 세부 항목과 API 매핑에 맞춰 채운다.
4. PRD에 있지만 이번 API 초안에서 제외한 항목을 범위 제외 또는 메모로 표시한다.
5. 세션 종료용 private context를 현재 결과에 맞게 동기화한다.

# Verification
- 문서 자체 검토:
  - 모든 도메인이 동일한 템플릿을 따르는지 확인
  - 포함/제외 범위가 `docs/DonDone_P0_API_Contract_v0.md`와 어긋나지 않는지 확인
  - PRD 핵심 표현(`testnet/demo`, `evidence-first`, `anomaly detection`)이 유지되는지 확인
- 실행 테스트:
  - 없음. 문서 작업이라 자동 테스트 대상이 아니다.

# Worktree Split Decision
- Single lane

공통 범위 판단과 문서 톤을 동시에 맞춰야 하므로 병렬 분리는 안전하지 않다. 하나의 문서 축에서 작성하고 마지막에 private context만 함께 정리한다.

# Commit Plan
- 1개 커밋 후보
- 범위: `docs/p0 functional spec draft`

# Open Questions
- 새 기능 명세 문서에서 도메인 간 의존 관계를 별도 섹션으로 분리할지 여부
- Documents/Claim 쪽 `summary`, `relatedDocuments[]`, `checklist[]`, `relatedLinks[]` 같은 중첩 object 설명을 이번 문서에서는 어느 정도까지 기능 관점으로 풀어쓸지 여부

# Assumptions
- 기능 명세 문서는 구현용 상세 DTO 문서가 아니라 범위 정렬용 상위 문서다.
- `features/workproof.md`, `features/wage.md`는 루트가 아니라 `.private/kwanwoo/features/` 아래 문서를 기준으로 읽는다.
- 이번 세션에서는 새 설계 결정보다 기능 범위 정리에 집중하므로 `DECISIONS.md`는 변경하지 않을 가능성이 높다.