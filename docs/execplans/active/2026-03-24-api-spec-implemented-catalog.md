# 2026-03-24 API 명세(구현 기준 카탈로그) 작성

## Source Inputs
- `AGENTS.md` (요구사항 확인 항목, 문서화 규칙, 테스트/보안 기준)
- `docs/DonDone_PRD_v1.5.md` (P0 범위/메시지 기준)
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/api/ApiResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/**/api/*Controller.java` 전수 스캔 결과

## Goal
- 현재 백엔드 코드 기준으로 실제 운영 가능한 API 엔드포인트를 한 문서에서 확인할 수 있는 구현 기준 명세서를 작성한다.

## In Scope
- 백엔드 컨트롤러 기준 엔드포인트 카탈로그 작성
- 공통 인증/권한 규칙, 공통 응답 envelope, 공통 에러 코드 규칙 반영
- 요청/응답 DTO 타입명 및 idempotency header 요구사항 표기
- 문서 경로 생성: `docs/DonDone_API_Spec_Implemented_v1.md`

## Out of Scope
- 엔드포인트 동작 로직 변경
- DTO 필드 단위 상세 샘플(JSON 예시) 전수 작성
- 모바일 앱/웹 UI 명세 변경
- DB 스키마 변경

## Affected Modules
### Backend
- 코드 변경 없음(참조만 수행)

### Mobile
- 변경 없음

### Docs
- `docs/execplans/active/2026-03-24-api-spec-implemented-catalog.md` (본 계획)
- `docs/DonDone_API_Spec_Implemented_v1.md` (신규 API 명세)

### Shared
- 변경 없음

## Contract Changes
- 런타임 API 계약 변경 없음
- 문서상 기준을 PRD 초안 중심(`DonDone_P0_API_Contract_v0.md`)에서 구현 기준 카탈로그로 보완

## Security Notes
- `SecurityConfig` 실제 규칙 기준으로 공개/권한 API를 명시
  - Public: `/api/auth/login`, `/api/auth/signup`, `/api/employer-auth/**`, `/health`, Swagger
  - `ROLE_EMPLOYER`: `/api/employer/**`
  - `ROLE_ADMIN`: `/api/admin/**`
  - 그 외 `/api/**`: `ROLE_USER` 또는 `ROLE_ADMIN`
- idempotency 적용 endpoint를 헤더 기준으로 표시

## Maintainability Notes
- 수작업 엔드포인트 목록 누락을 줄이기 위해 컨트롤러 전수 스캔 결과를 기준으로 문서를 작성한다.
- DTO 필드 상세는 빠르게 drift가 발생할 수 있어 타입 기준 카탈로그 + 코드 참조 경로 중심으로 유지한다.

## Implementation Steps
1. 컨트롤러, 보안 설정, 공통 응답/에러 클래스를 스캔한다.
2. 엔드포인트(method/path), request DTO, response 타입, idempotency 여부를 추출한다.
3. 접근권한(access) 규칙을 `SecurityConfig` 기준으로 분류한다.
4. `docs/DonDone_API_Spec_Implemented_v1.md` 문서에 공통 규칙 + 도메인별 카탈로그 표를 작성한다.
5. 추출 누락/중복(특히 같은 path의 params 분기)을 검토한다.

## Test Plan
- 코드 테스트는 수행하지 않음(문서 작업)
- 정적 검증:
  - 엔드포인트 수와 컨트롤러 스캔 결과 대조
  - `monthly-summary`와 같이 동일 path 다중 매핑 조건(`params`) 표기 확인
  - idempotency header 적용 endpoint 표기 확인

## Review Focus
- 엔드포인트 누락/중복 여부
- 권한 분류(access) 오기 여부
- 실제 `ApiResponse` 구조와 문서 설명의 일치 여부
- 향후 drift를 줄이기 위한 참조 경로/기준일 명시 여부

## Worktree Split Decision
- Single lane
- 이유: 이번 작업은 문서 단일 파일과 계획 문서 생성이 핵심이며, 공유 DTO/보안 규칙은 참조만 수행하므로 병렬 분할 이점이 작고 충돌 관리 비용이 더 크다.

## Commit Plan
1. `docs: 구현 기준 API 명세 카탈로그 추가`
2. `docs: api 명세 작성 실행계획 추가`

## Open Questions
- 팀에서 원하는 “정식 API 명세” 형식이 OpenAPI(JSON/YAML) 산출물인지, 현재와 같은 구현 카탈로그 Markdown인지 확정 필요
- DTO 필드 레벨 상세(필수/선택/검증 제약)를 후속 v2 범위로 확장할지 여부

## Assumptions
- 이번 요청의 “API 명세서”는 우선 구현 기준 엔드포인트/타입 카탈로그를 의미한다.
- 런타임 동작과 스키마는 변경하지 않고 문서만 작성한다.
- 상세 JSON example 전수는 후속 요구 시 별도 단계로 분리한다.
