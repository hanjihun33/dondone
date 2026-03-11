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
  - `docs/DonDone_P0_Functional_Spec_v0.md`

# Goal
PRD v1.5 기준 P0 전체가 같은 문서 안에서 보이도록, `DonDone` P0 기능 명세와 P0 API 계약 초안을 함께 재정리한다. 이미 상세 계약이 있는 도메인은 그대로 유지하고, `Money Home`, `Copilot`, `Demo Time Travel`은 최소 기능/계약 스케치를 추가하며, `W7 WorkProof Integrity`는 WorkProof 하위 P0 규칙으로 명확히 반영한다.

# In Scope
- 공유 문서 업데이트:
  - `docs/DonDone_P0_Functional_Spec_v0.md`
  - `docs/DonDone_P0_API_Contract_v0.md`
- P0 전체 범위 재정리:
  - `home`, `auth`, `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault`, `copilot`, `demo time travel`
- 문서 원칙 반영:
  - PRD P0 전체를 한 문서 안에서 보이게 유지
  - `P0-확정 / P0-초안 / P0-후속 세부화` 상태를 드러내기
  - `Money Home`, `Copilot`, `Demo Time Travel`은 최소 기능/계약 스케치 추가
  - `W7 WorkProof Integrity`는 WorkProof 하위 규칙으로 흡수
- 세션 마감 동기화:
  - `.private/kwanwoo/context/CURRENT.md`
  - `.private/kwanwoo/context/NEXT.md`
  - 필요 시 `.private/kwanwoo/logs/`

# Out of Scope
- backend/mobile 구현 변경
- Swagger 생성 또는 실제 DTO/validation 코드 반영
- P1 범위 전체
- Copilot 자유 대화형 확장, Home 개인화 추천 고도화, Demo Time Travel의 발표 스크립트 자체 설계

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- `docs/DonDone_P0_Functional_Spec_v0.md`
- `docs/DonDone_P0_API_Contract_v0.md`
- `docs/execplans/active/2026-03-10-p0-functional-spec-draft.md`

## Private Context
- `.private/kwanwoo/context/CURRENT.md`
- `.private/kwanwoo/context/NEXT.md`
- `.private/kwanwoo/logs/2026-03-10.md` (파일이 있거나 새로 만들 필요가 있을 때만)

# Requirement Clarification
- expected behavior:
  - P0 기능 명세와 API 계약 초안만 읽어도 PRD 기준 P0 전체 범위와 확정도 차이를 함께 이해할 수 있어야 한다.
- exact scope:
  - PRD P0 범위인 `home`, `auth`, `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault`, `copilot`, `demo time travel`를 같은 문서 안에 보이게 정리한다.
- contract changes:
  - 구현 코드 변경은 없지만, `home`, `copilot`, `demo`에 대한 최소 API 스케치를 공유 문서에 추가한다.
- security impact:
  - 문서에는 기존 JWT 보호 원칙, 타인 리소스 은닉, testnet/demo 한계, Copilot facts-only 제약을 그대로 반영한다.
- non-functional impact:
  - 문서 생성/송금 비동기 처리, evidence-first UX, demo mode용 `asOf` 재현성, backward compatibility 관점의 현재 가정을 명시한다.

# Security Notes
- auth 공개 경로는 기존 API 초안 기준(`signup`, `login`, `/health`, Swagger)만 유지한다.
- 나머지 기능은 JWT 보호 대상이라는 점을 기능 명세와 API 계약서에 명시한다.
- Wage 결과는 법률/재무 최종 판단이 아니라 anomaly detection + evidence-first라는 PRD 제약을 유지한다.
- Advance, Remittance, Vault는 실거래가 아니라 demo/testnet 시뮬레이션이라는 한계를 반복 명시한다.
- Copilot은 facts-only, 숫자 재계산 금지, 추정 금지 원칙을 유지한다.
- Demo Time Travel은 demo account / demo mode 한정이라는 제약을 반복 명시한다.

# Maintainability Notes
- 기능 명세는 API 표 필드를 다시 복제하지 않고, 기능-흐름-규칙 중심으로 정리한다.
- 각 도메인 섹션은 동일한 템플릿을 사용한다:
  - `상태 / 목표 / 범위 / 범위 제외 / 핵심 흐름 / 주요 규칙 / API 매핑 / v0 가정 / 열린 질문`
- 문서 전반의 용어는 현재 공유 문서 표현과 맞춘다:
  - `배경`, `v0 메모`, `확장 메모`, `self-contained`
- 상세 계약이 덜 굳은 P0 항목은 문서에서 제외하지 않고 최소 스케치 + pending note로 남긴다.

# Implementation Steps
1. PRD 기준 P0 전체와 기존 기능/API 문서의 제외 지점을 다시 대조한다.
2. 실행 계획과 문서 상단 범위/상태 표기를 `P0 전체 포함` 기준으로 갱신한다.
3. 기능 명세에 `Money Home`, `Copilot`, `Demo Time Travel` 섹션을 추가하고, `W7 WorkProof Integrity`를 WorkProof 하위 규칙으로 반영한다.
4. API 계약서에 `home`, `copilot`, `demo` 최소 스케치와 `X-Demo-AsOf` 공통 규칙을 추가한다.
5. 세션 종료용 private context를 현재 결과에 맞게 동기화한다.

# Verification
- 문서 자체 검토:
  - 기능 명세가 P0 전체를 빠짐없이 포함하는지 확인
  - API 계약서에 `home`, `copilot`, `demo` 최소 스케치가 추가됐는지 확인
  - 기능 명세와 API 계약서의 상태 표기와 범위가 서로 어긋나지 않는지 확인
  - PRD 핵심 표현(`testnet/demo`, `evidence-first`, `anomaly detection`, `facts-only`)이 유지되는지 확인
- 실행 테스트:
  - 없음. 문서 작업이라 자동 테스트 대상이 아니다.

# Worktree Split Decision
- Single lane

P0 범위 재정리와 상태 표기를 두 문서에서 함께 맞춰야 하므로 병렬 분리는 안전하지 않다. 문서 한 축에서 수정하고 마지막에 private context만 함께 정리한다.

# Commit Plan
- 1개 커밋 후보
- 범위: `docs/p0 scope realignment`

# Open Questions
- `Money Home` 조합 응답을 단일 endpoint로 고정할지, 후속에 카드 단위 분리 가능성을 메모로 남길지 여부
- `Copilot`을 단일 intent 기반 endpoint로 둘지, explain/claim-summary/translate 분리 endpoint로 둘지 여부
- `Demo Time Travel`의 `GET /api/demo/state?asOf=` 응답을 메타 중심으로 둘지, 조합 상태까지 포함할지 여부

# Assumptions
- 기능 명세 문서는 구현용 상세 DTO 문서가 아니라 범위 정렬용 상위 문서다.
- `home`, `copilot`, `demo`는 이번 세션에서 구현 상세를 고정하지 않고 최소 스케치만 남긴다.
- `W7`은 별도 독립 모듈보다 WorkProof 하위 신뢰 규칙으로 정리하는 편이 현재 PRD 해석에 더 자연스럽다.
- `features/workproof.md`, `features/wage.md`는 루트가 아니라 `.private/kwanwoo/features/` 아래 문서를 기준으로 읽는다.