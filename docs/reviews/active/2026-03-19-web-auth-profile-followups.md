# Scope
- Slice 2 `Auth and profile foundation` 마감 시점 deferred work
- 기준 구현:
  - `POST /api/employer-auth/invitations/accept`
  - `POST /api/employer-auth/login`
  - `GET /api/employer/profile`
  - `EmployerProfile`, `EmployerInvitationToken`, `EmploymentMembership`, `EmployerAccessScope`

# Closed In Slice 2
- employer web auth/profile 최소 경계를 backend에 열었다.
- invitation token은 DB row + hash 저장, 이메일 canonicalization 공통화, company-workplace binding 검증까지 고정했다.
- employer authorization source of truth를 `EmployerProfile + EmploymentMembership`으로 고정했다.

# Hardening And Rescope Backlog
## 1. `Workplace.companyId` additive 연결 승격 여부 재평가
- 현재 `Workplace.companyId`는 employer scope의 company-workplace binding을 증명하는 보조 연결이다.
- Hardening 또는 shared domain 리팩터링 시 다시 판단할 것:
  - app/web 공통 필수 소속 키로 승격할지
  - 기존 worker 생성/조회 흐름에 어떤 migration 비용이 생기는지
  - `Workplace.user`와 공존하는 과도기 모델을 언제 정리할지

## 2. DB 차원의 이메일 canonical uniqueness 보강
- 현재는 애플리케이션 레벨 canonicalization으로 중복을 막고 있다.
- 아직 `lower(email)` 기준 유니크 인덱스나 동등한 DB 보강은 없다.
- Hardening에서 정리할 것:
  - 인덱스/migration 추가 여부
  - 기존 데이터 충돌 정리 전략
  - worker/employer 계정 분리 정책과의 정합성

## 3. invitation 발급/운영 bootstrap 경로 재스코프
- Slice 2는 accept/login/profile만 열었고 invitation 발급은 seed/bootstrap 전제로 남겼다.
- 이 항목은 Slice 2 미완료가 아니라 의도적 제외 범위다.
- 추후 실제 운영 경로가 필요해지면 별도 execplan로 분리할 것:
  - 누가 invitation을 발급하는지
  - company/default workplace binding을 어떤 UI/API에서 검증하는지
  - revoke/reissue/audit를 어떤 엔티티와 권한으로 열지

# Reading Order
1. `docs/web/implementation-slices.md`
2. `docs/execplans/active/2026-03-19-web-auth-profile-foundation.md`
3. 이 문서
4. 이후 해당 시점의 active review note

# Decision Rule At Hardening
- 각 항목을 `fixed`, `accepted risk`, `rescope` 중 하나로 분류한다.
- 분류 결과는 새 review note 또는 hardening execplan에 남긴다.
