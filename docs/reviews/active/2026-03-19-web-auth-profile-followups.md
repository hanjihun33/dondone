# Scope
- Slice 2 `Auth and profile foundation` 이후 남은 hardening / rescope 항목
- 대상 범위
  - `POST /api/employer-auth/invitations/accept`
  - `POST /api/employer-auth/login`
  - `GET /api/employer/profile`
  - `EmployerProfile`
  - `EmployerInvitationToken`
  - `EmploymentMembership`
  - `EmployerAccessScope`

# Closed In Slice 2
- employer web auth/profile foundation 완료
- invitation token hash 저장, email canonicalization, company-workplace binding foundation 완료
- employer authorization source of truth를 `EmployerProfile + EmploymentMembership`로 고정 완료

# Hardening And Rescope Backlog
## 1. `Workplace.companyId` additive 승격 여부
- 현재 상태
  - `Workplace.companyId`는 employer scope와 company-workplace binding 검증용 additive 필드로 들어가 있다.
- 남은 판단
  - app/web 공통 도메인에서 장기 source-of-truth로 승격할지
  - worker flow migration과 함께 어떻게 정리할지

## 2. DB canonical uniqueness 정리
- 현재 상태
  - `EmailNormalizer`로 저장/조회 입력을 정규화한다.
  - repository는 과도기 호환성 때문에 `findByEmailIgnoreCase`, `existsByEmailIgnoreCase`를 유지한다.
  - 아직 `lower(email)` 기준 DB uniqueness나 동등한 DB 보강은 없다.
- 남은 판단
  - `lower(email)` 기준 migration과 uniqueness 보강 여부
  - 정리가 끝나면 단순 `findByEmail` 계열로 수렴할지 여부
- 리뷰 반영
  - reviewer가 지적한 `IgnoreCase` 조회 유지 이유와 정리 시점은 이 항목에 포함해서 관리한다.
  - 별도 중복 backlog는 만들지 않는다.

## 3. invitation 발급 / 운영 경로 재스코프
- 현재 상태
  - Slice 2에서는 accept/login/profile까지만 닫았고 invitation 발급/운영 경로는 의도적으로 제외했다.
- 남은 판단
  - invitation 생성 UI/API 필요 여부
  - revoke/reissue/audit 필요 여부
  - company/default workplace binding 운영 경로와 함께 열지

# Reading Order
1. `docs/web/implementation-slices.md`
2. `docs/execplans/active/2026-03-19-web-auth-profile-foundation.md`
3. 관련 web 문서
4. 이 follow-up note

# Decision Rule At Hardening
- 각 항목은 `fixed`, `accepted risk`, `rescope` 중 하나로 닫는다.
- 닫힌 판단은 hardening execplan 또는 review note에 연결한다.
