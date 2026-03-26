# Source Inputs

- 사용자 요구: 근로자 근무 기록에서 정상 퇴근은 `반영됨`, 정정요청 제출 후는 `검토 중`, 고용주 반려 후는 `제외됨`으로 보여야 함
- 모바일 상태 매핑 확인: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- 모바일 수정요청 제출 흐름 확인: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- 모바일 원격 상태 동기화 확인: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionMappers.kt`
- 백엔드 correction request 생성 흐름 확인: `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofCorrectionRequestService.java`
- 백엔드 records reflectionStatus 계산 확인: `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
- 백엔드 workproof domain 상태 확인: `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`

# Goal

근로자 정정요청 제출 시 해당 근무 기록의 상태가 즉시 `검토 중`으로 조회되고, 이후 고용주가 반려하면 `제외됨`으로 조회되도록 backend 상태 전이와 mobile 표시를 일관되게 맞춘다.

# In Scope

- backend correction request 생성 시 workproof 상태 전이 보강
- backend records 응답의 `reflectionStatus`가 correction review 흐름을 반영하도록 조정
- mobile이 backend의 `NEEDS_REVIEW`/`EXCLUDED`를 현재 라벨 체계에 맞게 표시하는지 확인하고 필요한 최소 수정 반영
- 해당 상태 전이에 대한 최소 테스트 보강

# Out of Scope

- 반려 사유 상세 문구 노출
- 고용주 검토/반려 UI 자체 구현
- correction request 상세 조회 API 추가
- wage/documents 화면의 문구 확장 정비

# Affected Modules

## Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofCorrectionRequestService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
- 관련 테스트 파일(기존 workproof integration/service test 범위에서 최소 추가)

## Mobile

- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionMappers.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- 필요 시 관련 test 파일

## Docs

- `docs/execplans/active/2026-03-26-workproof-correction-status.md`

## Shared

- `reflectionStatus` contract semantics (`PENDING`, `NEEDS_REVIEW`, `REFLECTED`, `EXCLUDED`)에 대한 backend/mobile 해석 일치

# Contract Changes

- DTO shape 변경은 피하고 기존 `reflectionStatus` 의미만 바로잡는다.
- correction request `PENDING`이 존재하는 동안 해당 workproof record 조회 결과의 `reflectionStatus`는 `NEEDS_REVIEW`로 보이게 맞춘다.
- 고용주 반려 처리 경로가 이미 `EXCLUDED`를 만들면 그대로 소비하고, 아니면 그 전이 지점을 별도 후속 과제로 남긴다.

# Security Notes

- 인증/인가 규칙은 유지한다.
- correction request 생성 권한, worker membership 검증, exposed path는 변경하지 않는다.
- 상태 전이만 수정하고 민감정보 노출 범위는 늘리지 않는다.

# Maintainability Notes

- correction request 상태와 workproof 반영 상태가 분리된 구조이므로, 한쪽만 바꾸는 임시 로직을 여러 레이어에 중복하지 않는다.
- 가능하면 backend에서 조회용 상태를 일관되게 계산하고 mobile은 그 결과를 그대로 소비하게 유지한다.
- `PENDING` vs `NEEDS_REVIEW` 의미 혼선을 줄이기 위해 매퍼 보정은 최소화하고 source-of-truth를 backend read path에 둔다.

# Implementation Steps

1. backend에서 correction request 생성 시 auto-approve가 아닌 경우 workproof의 조회 상태가 `NEEDS_REVIEW`로 보이도록 domain/service 전이를 수정한다.
2. backend records/read model 경로에서 `reflectionStatus` 계산이 수정요청 review 상태와 반려 상태를 올바르게 반환하는지 정리한다.
3. mobile sync mapper와 workproof label/detail text가 backend 반환값을 그대로 `검토 중`/`제외됨`으로 표시하는지 확인하고 필요한 최소 수정만 반영한다.
4. 관련 테스트를 추가하거나 갱신해 제출 후 `NEEDS_REVIEW`, 반려 후 `EXCLUDED`가 유지되는지 검증한다.

# Test Plan

- Backend
  - correction request service 또는 integration test로 review-needed request 제출 후 해당 record 조회의 `reflectionStatus == NEEDS_REVIEW` 검증
  - 반려 상태가 이미 구현돼 있으면 record 조회의 `reflectionStatus == EXCLUDED` 검증
- Mobile
  - `WorkproofUiModel` 매핑 테스트 또는 mapper 테스트로 `NEEDS_REVIEW -> 검토 중`, `EXCLUDED -> 제외됨` 확인
- Verification Commands
  - backend: 관련 단일/소수 test 클래스 우선 실행
  - mobile: `:app:compileDebugKotlin` 및 필요한 최소 unit test

# Review Focus

- correction request 생성 직후 reflected record가 review-needed로 조회되는지
- auto-approve 경로는 여전히 `반영됨`을 유지하는지
- rejected 상태가 `제외됨`으로 일관되게 보이는지
- wage summary 등 reflected/needs-review 집계가 의도치 않게 깨지지 않는지

# Worktree Split Decision

Single lane

`reflectionStatus` contract와 shared workproof entity/state 계산이 함께 움직이므로 backend와 mobile을 병렬로 찢으면 merge risk가 높다. 먼저 backend source-of-truth를 고정한 뒤 mobile 확인을 같은 레인에서 마무리하는 편이 안전하다.

# Commit Plan

- 1 commit: backend workproof correction status transition and read-model fix
- 1 commit: mobile status mapping/test alignment if mobile diff가 실제로 필요할 때만 추가

# Open Questions

- 고용주 반려 처리 API/서비스가 현재 저장소에 구현돼 있는지
- records 조회에서 pending correction request를 `NEEDS_REVIEW`로 직접 노출할지, entity financialStatus를 갱신할지 어느 쪽이 현재 구조에 더 일관적인지

# Assumptions

- 사용자 기대 동작은 제품 의도와 일치한다: 정상 반영 `반영됨`, 정정요청 대기 `검토 중`, 반려 `제외됨`
- 기존 mobile 라벨 체계는 유지하고 backend가 올바른 `reflectionStatus`를 주는 방향이 우선이다
- 반려 사유 표시는 후속 작업으로 미룬다
