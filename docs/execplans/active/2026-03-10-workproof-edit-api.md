## Source Inputs

- `docs/DonDone_PRD_v1.5.md` sections 7B W4, 13.5
- Repository `AGENTS.md`
- Existing `workproof` backend module exploration
- Current Android/mockup workproof edit UI exploration

## Goal

`PATCH /api/workproof/{id}`를 추가해 반영 완료된 WorkProof의 근무 시간을 수정하고, 수정 사유와 첨부 개수를 반영하며, 변경 전/후 시간을 감사 로그로 자동 저장하는 최소 W4 백엔드 슬라이스를 구현한다.

## In Scope

- `PATCH /api/workproof/{id}` endpoint 추가
- 수정 요청 DTO 추가
- WorkProof 시간/메모/수정 사유/첨부 개수 업데이트
- 변경 전/후 시간, 수정 사유, 첨부 개수, 수정 시각을 저장하는 감사 로그 엔티티/리포지토리 추가
- 인증/소유권/유효성 검증 테스트 추가

## Out of Scope

- 첨부 파일 바이너리 업로드와 실제 스토리지 연동
- 감사 로그 조회 전용 API
- Proof Pack 문서 반영 API
- pending WorkProof를 보완해 reflected로 바꾸는 별도 흐름
- 위치 스냅샷 재수집/수정

## Affected Modules

### Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/WorkProofController.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/dto/request/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/repo/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/workproof/WorkProofIntegrationTest.java`

### Mobile

- 직접 수정 없음
- 현재 Workproof UI가 요구하는 수정 사유/첨부 존재 여부와 감사 로그 문구를 서버 계약으로 뒷받침

### Docs

- 본 실행 계획

### Shared

- 공통 `ApiResponse` 형식 유지
- JWT 인증 규칙 유지

## Contract Changes

- Add `PATCH /api/workproof/{id}`
- Request body fields:
  - `clockInAt` required
  - `clockOutAt` required
  - `editReason` required
  - `memo` optional (`null` means keep existing memo, empty string clears it)
  - `attachmentCount` optional
  - `attachments` optional
    - each item: `type`, `fileName`, `fileRef`
    - when both `attachmentCount` and `attachments` are provided, their size must match
    - when `attachmentCount` is omitted, server derives it from `attachments.size`
- Response body:
  - existing `WorkProofResponse` 재사용
  - edited record 응답에서는 원본 디바이스/서버/GPS 증거 스냅샷을 `null`로 내려 수정된 시간과 혼동되지 않게 함
  - 수정 후 `edited=true`, `updatedAt` 갱신
- DB:
  - `work_proofs`는 기존 컬럼 재사용
  - `work_proof_audit_logs` 테이블 추가
  - 감사 로그는 시간뿐 아니라 수정 전/후 `editReason`, `memo`, `attachmentCount`까지 보존
  - 선택 증거 메타는 `work_proofs`와 감사 로그의 JSON 컬럼에 저장해 이후 문서 생성/이력 조회에서 재사용 가능하게 함

## Security Notes

- 기존 JWT 인증 필수 유지
- 수정은 본인 소유 WorkProof에만 허용
- `PATCH`도 `GET /{id}`와 같은 소유권 검사를 사용
- 공개 경로, 토큰 처리, `SecurityConfig` 변경 없음

## Maintainability Notes

- 생성 검증과 수정 검증을 같은 validator 계층에 두되, create/update 규칙은 메서드로 분리한다
- WorkProof 엔티티가 수정 규칙과 상태 갱신을 직접 소유하도록 해 service가 필드 세팅 나열로 비대해지지 않게 한다
- 감사 로그는 최소 범위의 별도 엔티티로 저장하되, 첨부 파일 메타/유형/해시 같은 확장 필드는 이번에 추가하지 않는다
- 현재 모바일/mockup은 첨부 개수만 소비하므로, 응답 DTO는 그대로 두고 WorkProof/감사 로그 저장 계약만 먼저 확장한다

## Implementation Steps

1. 실행 계획을 추가하고 단일 레인 범위를 고정한다.
2. 수정 요청 DTO를 추가한다.
3. WorkProof 엔티티에 수정용 도메인 메서드를 추가한다.
4. 감사 로그 엔티티/리포지토리를 추가하고 변경 전/후를 저장한다.
5. Controller/Service/Validator에 `PATCH /api/workproof/{id}` 흐름을 연결한다.
6. 인증, 소유권, 검증, 수정 결과를 통합 테스트로 확인한다.
7. `./gradlew test --no-daemon --rerun-tasks`로 회귀를 확인한다.

## Test Plan

- `WorkProofIntegrationTest`
  - reflected WorkProof 수정 성공
  - 수정 후 `edited=true`, `updatedAt` 반영
  - `attachments` 메타 저장 및 `attachmentCount` 일치 규칙 확인
  - 타 사용자 수정 시 `WORKPROOF_NOT_FOUND`
  - `editReason` 누락 validation 실패
  - `clockOutAt <= clockInAt` 실패
  - pending WorkProof 수정 차단
  - 감사 로그 저장 확인
- 전체 백엔드 회귀
  - `./gradlew test --no-daemon --rerun-tasks`

## Review Focus

- 수정 규칙이 create 흐름과 뒤섞이지 않았는지
- 감사 로그가 현재 WorkProof 값 덮어쓰기와 충돌 없이 before/after를 보존하는지
- 수정된 WorkProof 응답이 기존 증거 스냅샷을 현재 근무 증거처럼 오해시키지 않는지
- 응답 계약이 기존 모바일/데모 흐름을 깨지 않는지

## Worktree Split Decision

Single lane

엔티티, DTO, validator, service, 테스트가 함께 움직이는 작은 백엔드 계약 변경이라 병렬 분리 이점이 없다.

## Commit Plan

- Commit 1: `docs: workproof edit api 실행계획 추가`
- Commit 2: `feat: workproof 수정 api와 감사 로그 추가`

## Open Questions

- 첨부 메타를 `attachmentCount` 외 별도 구조로 언제 열지
- 감사 로그를 추후 `GET /api/workproof/{id}`에 포함할지 전용 API로 분리할지

## Assumptions

- 이번 슬라이스의 “첨부”는 파일 업로드가 아니라 첨부 개수 기록만 의미한다
- `attachments`는 바이너리 업로드가 아니라 `type/fileName/fileRef` 메타만 저장한다
- 수정 API는 이미 `clockOutAt`이 있는 reflected WorkProof만 대상으로 한다
- `workDate`는 수정하지 않고 기존 값 유지 + `clockInAt` 날짜 일치만 허용한다
