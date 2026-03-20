# Scope
- Slice 3 `Workplace settings` backend foundation 마감 시점 backlog
- 기준 구현:
  - `GET /api/employer/workplace-settings`
  - `PUT /api/employer/workplace-settings`
  - `Workplace` settings metadata
  - `WorkProof` workplace snapshot 고정

# Closed In Slice 3
- settings authz를 `EmployerAccessScope.defaultWorkplaceId` 기준으로 고정했다.
- 영향 범위 해석은 `EmploymentMembership.companyId/workplaceId` 기준으로 고정했다.
- settings 변경은 미래 `check-in/check-out`부터 적용하고 과거 완료 `WorkProof`는 자동 재판정하지 않도록 문서/코드에 반영했다.
- 과거 `WorkProof` detail/PDF가 live `Workplace` 변경을 따라가지 않도록 workplace snapshot 고정 저장으로 보강했다.

# Hardening Backlog
## 1. `detailAddress -> mapLabel` 임시 매핑 재평가
- 현재는 `detailAddress`를 `Workplace.mapLabel` 저장소에 매핑한다.
- foundation 단계에서는 안전하지만, worker-side `mapLabel` 의미와 장기적으로 분리할 필요가 있다.
- Hardening 또는 shared workplace contract 재작업 시:
  - 독립 필드 승격 필요 여부
  - worker API/PDF/기존 workplace 생성 흐름 영향
  - migration 비용

## 2. settings 저장 시점과 출퇴근 이벤트 경합 검증
- 정책은 `server time` 기준으로 고정했지만, 거의 동시에 발생하는 save/check-in/check-out ordering 테스트는 아직 약하다.
- Hardening에서 확인할 것:
  - save 직후 check-in
  - save 직전 check-in, save 직후 check-out
  - DB transaction ordering에 따른 effective snapshot 해석

## 3. multi-workplace와 관리자 재계산은 별도 재스코프
- multi-workplace switcher
- 예약 효력 시점
- 과거 `WorkProof` 관리자 재계산 도구
- 이 셋은 Slice 3 미완료가 아니라 의도적 제외 범위다. 다음에 열면 새 execplan로 분리한다.

# Reading Order
1. `docs/web/implementation-slices.md`
2. `docs/execplans/active/2026-03-19-web-workplace-settings-foundation.md`
3. 이 문서
4. 이후 해당 시점의 active review note

# Decision Rule At Hardening
- 각 항목을 `fixed`, `accepted risk`, `rescope` 중 하나로 분류한다.
- 분류 결과는 새 review note 또는 hardening execplan에 남긴다.
