# Source Inputs

- `AGENTS.md`
- `docs/DonDone_PRD_v1.5.md`
  - 6.3 급여일 전: 미리받기
  - 7C. 미리받기(Advance, 데모 시뮬레이션)
  - 12.2 미리받기 안내 문구
- `docs/DonDone_P0_API_Contract_v0.md`
  - `POST /api/advance/requests`
  - `GET /api/advance/requests`
  - `GET /api/advance/requests/{requestId}`
- 기존 구현
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/advance/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/api/RemittanceAdminController.java`
  - `apps/dondone-web/src/pages/admin/LegacyAdminPage.tsx`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/advance/**`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`
- 확인 결과
  - `AdvanceRequestStatus`는 이미 `SUBMITTED / APPROVED / REJECTED / NEEDS_REVIEW`를 갖고 있다.
  - 하지만 `AdvanceService.createRequest()`는 아직 신청 즉시 `AdvanceRequest.approve(...)`로 저장한다.
  - admin 웹의 미리받기 승인 표는 현재 `LegacyAdminPage.tsx`의 mock 상태다.
  - mobile advance 실연동은 `approvedAmount`를 non-null로 가정하고 있어 status 변경 계약을 함께 손봐야 한다.

# Goal

관리자가 근로자 미리받기 요청을 실제로 조회하고 승인/반려할 수 있게 만든다. worker 신청은 더 이상 즉시 `APPROVED`가 아니라 `SUBMITTED`로 저장되고, admin 결정 후 `APPROVED / REJECTED`로 전이된다. 이 변경에 맞춰 admin 웹 mock을 실연동으로 바꾸고, mobile advance 화면도 pending 상태를 깨지지 않게 맞춘다.

# In Scope

- backend advance 요청 생성 흐름을 `SUBMITTED` 저장으로 변경
- admin 전용 advance 운영 API 추가
  - pending/terminal 목록 조회
  - 상세 조회
  - 승인
  - 반려
- admin 웹에서 미리받기 요청 표를 mock이 아닌 backend 실연동으로 교체
- mobile advance 실연동이 `SUBMITTED` 상태와 nullable `approvedAmount`를 처리하도록 조정
- 관련 validation, authz, 테스트, 문서 갱신

# Out of Scope

- 부분 승인(요청액보다 적은 승인금액)
- employer 승인 플로우
- remittance transfer admin 기능 확장
- 실제 금융 파트너/회수/정산 연동
- advance 정책 엔진 자체의 대규모 재설계

# Affected Modules

## Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/advance/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/admin/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
- `apps/dondone-backend/src/test/java/**/advance/**`
- `apps/dondone-backend/src/test/java/**/admin/**`

## Mobile

- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/advance/**`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/**`

## Docs

- `docs/execplans/active/2026-03-20-admin-advance-approval-flow.md`
- `docs/DonDone_P0_API_Contract_v0.md`
- `docs/web/implementation-slices.md`

## Shared

- admin role/session 계약
- advance request status 표현(`SUBMITTED / APPROVED / REJECTED`)
- demo disclaimer 문구

# Contract Changes

- worker advance create/list/detail 응답에서 `status=SUBMITTED`가 실제로 발생한다.
- worker advance 응답의 `approvedAmount`는 pending/rejected 상태에서 nullable이 될 수 있다.
- 신규 admin API
  - `GET /api/admin/advance/requests`
  - `GET /api/admin/advance/requests/{requestId}`
  - `POST /api/admin/advance/requests/{requestId}/approve`
  - `POST /api/admin/advance/requests/{requestId}/reject`
- admin list/detail 응답에는 최소한 아래가 포함된다.
  - requestId
  - worker name/email
  - company/workplace
  - requestedAmount
  - status
  - requestedAt
  - eligibility snapshot summary
- approve/reject command는 optional memo를 받을 수 있다.

# Security Notes

- admin advance API는 `ROLE_ADMIN`만 접근 가능해야 한다.
- worker advance API는 계속 본인 요청만 조회 가능해야 한다.
- approve/reject는 `SUBMITTED` 상태에서만 허용해야 한다.
- pending 상태의 `approvedAmount` null 처리 때문에 client 파싱 예외가 나지 않도록 backend/mobile 양쪽에서 방어가 필요하다.

# Maintainability Notes

- advance 도메인 상태 전이는 `AdvanceRequest` 모델이 소유해야 한다. controller/service에 전이 규칙을 흩뿌리지 않는다.
- admin advance DTO는 remittance admin DTO와 섞지 않고 `advance` 또는 `admin` 하위에서 명확히 분리한다.
- mobile은 status별 UI 문구와 금액 포맷 분기를 mapper/UI 모델에 모으고, raw DTO null 체크를 화면 곳곳에 퍼뜨리지 않는다.

# Implementation Steps

1. advance request 상태 전이 정책 확정
   - create는 `SUBMITTED`
   - admin approve는 요청액 전액 승인
   - admin reject는 반려 메모만 기록
2. backend advance 모델/서비스 보강
   - `AdvanceRequest.submit(...)`, `approve(...)`, `reject(...)` 정리
   - list/detail/create 응답 mapper에서 nullable `approvedAmount` 반영
3. admin advance API 추가
   - 목록/상세/승인/반려 controller, DTO, service
   - worker/company/workplace join 정보 read-model 구성
4. admin 웹 실연동
   - legacy mock의 미리받기 승인 표를 현재 `/admin`으로 흡수하거나 current admin page에 section 추가
   - 필터(`전체/대기/승인/반려`)와 승인/반려 액션 연결
5. mobile advance 계약 보정
   - `approvedAmount` nullable 대응
   - 신청 직후 토스트/상태 문구를 `승인 대기` 기준으로 변경
   - 이력/상세에서 `SUBMITTED`를 정상 렌더링
6. docs/tests 갱신

# Test Plan

- backend targeted tests
  - advance create가 `SUBMITTED`로 저장되는지
  - admin approve/reject 상태 전이
  - admin 권한 없는 토큰 접근 차단
  - worker가 본인 요청만 조회 가능한지
- mobile/build
  - affected Kotlin compile/build check
  - pending status와 nullable amount 파싱 회귀 확인
- web
  - `apps/dondone-web` typecheck
  - admin approval section 로딩/empty/error/success 확인

# Review Focus

- `AdvanceService.createRequest()`가 더 이상 즉시 승인하지 않는지
- `approvedAmount` nullable 전환이 mobile/web 파싱을 깨지 않는지
- admin approve/reject가 중복 처리되거나 terminal state를 다시 건드리지 않는지
- admin list가 worker 개인정보를 과도하게 노출하지 않는지

# Worktree Split Decision

Single lane

advance status contract이 backend, admin web, mobile advance에 동시에 걸려 있어 공유 DTO와 상태 의미가 계속 움직인다. 지금은 병렬 분리보다 한 lane에서 계약을 고정하고 한 번에 맞추는 편이 merge risk가 낮다.

# Commit Plan

1. `feat: 관리자 미리받기 승인 api 추가`
2. `feat: 관리자 미리받기 승인 화면 연동`
3. `fix: 모바일 미리받기 승인 대기 상태 반영`
4. `docs: 미리받기 승인 흐름 문서 정리`

# Open Questions

- admin approve/reject에 decision memo만 둘지, reject reason code까지 둘지
- `NEEDS_REVIEW`를 admin advance에도 실제로 쓸지, 이번 범위는 `SUBMITTED/APPROVED/REJECTED`만 닫을지
- admin approval section을 `/admin`에 바로 넣을지, legacy admin page를 단계적으로 흡수할지

# Assumptions

- 이번 범위의 미리받기 운영자는 service admin이다.
- MVP에서는 부분 승인 없이 요청액 전액 승인만 허용한다.
- worker mobile은 `SUBMITTED`를 “승인 대기”로 표현하면 충분하다.
- 기존 remittance admin 기능과는 별도 lane으로 취급한다.
