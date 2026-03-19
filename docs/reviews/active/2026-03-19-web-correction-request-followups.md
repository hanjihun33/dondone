# Scope
- Slice 5 `Correction request flow` foundation 이후 미룬 후속 작업 정리
- 기준 구현
  - `GET /api/employer/correction-requests`
  - `GET /api/employer/correction-requests/{requestId}`
  - `POST /api/employer/correction-requests/{requestId}/approve`
  - `POST /api/employer/correction-requests/{requestId}/reject`
  - `POST /api/workproof/{workProofId}/correction-requests`
  - `CorrectionRequest`
  - `CorrectionDecisionAudit`
  - `WorkProofAuditLog` 연동

# Closed In Slice 5 Foundation
- employer correction queue는 `/api/employer/*` 전용 surface로 먼저 열고 기존 worker API contract는 바꾸지 않았다.
- worker는 직접 확정 수정하지 않고 correction request를 보내며, employer가 승인/반려하는 정책을 고정했다.
- approve는 request status, `WorkProof.updateTimes(...)`, `WorkProofAuditLog`, `CorrectionDecisionAudit`를 한 transaction으로 묶었다.
- reject는 `WorkProof`를 수정하지 않고 request status와 `CorrectionDecisionAudit`만 남긴다.
- attachment는 정정 요청 증빙자료라는 의미만 먼저 고정했고, web 표시 방식은 후속으로 남겼다.

# Deferred Work Order
## 1. attachment detail surface
- 라벨: `temporary`, `shared_policy_pending`
- 현재 임시 처리
  - correction detail 응답은 `attachmentCount`와 attachment metadata(`type`, `fileName`)까지만 노출한다.
  - request와 `WorkProof`에는 attachment metadata json을 내부 보존용으로만 저장한다.
- 지금 확정하지 않은 이유
  - 첨부의 의미는 정정 요청 증빙자료로 고정했고 detail API의 안전한 metadata shape도 열었지만, employer web에서 목록/상세 어디에 어떻게 보여줄지와 다운로드 계약은 아직 UI/스토리지 계약으로 닫히지 않았다.
- 닫히는 조건
  - attachment metadata의 web 표시 위치와 download contract가 고정된다.

## 2. review-required record detail/command surface
- 라벨: `deferred`
- 현재 상태
  - employer issue queue foundation은 `GET /api/employer/issues`로 열었고, `PENDING` correction request와 `NEEDS_REVIEW` record를 `itemType`으로 함께 노출한다.
  - 다만 review-required record는 아직 read-model row만 있고 별도 detail/command surface는 없다.
- 지금 확정하지 않은 이유
  - 현재 Slice 5 범위는 queue boundary와 correction request approval flow foundation까지이고, review-required record 처리 규칙까지 열면 별도 command/state transition 계약이 추가로 필요하다.
- 닫히는 조건
  - `NEEDS_REVIEW` record에 대한 employer detail/resolve contract 또는 accepted risk가 고정된다.

## 3. legacy direct edit endpoint lifecycle
- 라벨: `deferred`
- 현재 상태
  - backend에는 기존 worker direct edit endpoint `PATCH /api/workproof/{id}`가 남아 있다.
  - correction request create endpoint `POST /api/workproof/{workProofId}/correction-requests`는 이미 추가됐다.
- 지금 확정하지 않은 이유
  - 현재 대화 범위는 backend correction flow foundation과 web API 정리까지이고, 기존 worker/mobile flow를 걷어내는 변경은 제외 범위다.
- 닫히는 조건
  - `PATCH /api/workproof/{id}`를 legacy 유지, deprecated, 제거 중 무엇으로 둘지 정책과 문서가 고정된다.

## 4. worker direct edit flow migration
- 라벨: `deferred`
- 현재 상태
  - shared policy는 고정했고 backend worker create endpoint도 열었다: `POST /api/workproof/{workProofId}/correction-requests`
  - worker app은 아직 기존 direct edit flow를 쓰고 있고, 현재 수정 저장은 backend `PATCH /api/workproof/{id}` 호출이 아니라 local-only mock 갱신이다.
- 지금 정리할 일
  - mobile/client 범위가 열릴 때 worker 수정 저장을 correction request submit flow로 전환한다.
  - local-only worker edit draft를 어떤 API/상태로 대체할지 정리한다.
- 닫히는 조건
  - worker client와 backend contract가 correction request submit 흐름으로 맞춰진다.

## 5. dashboard/wage/docs invalidation strategy
- 라벨: `hardening`
- 현재 고정
  - 승인 후 사용자 화면은 즉시 최신처럼 보여야 하고, foundation 구현은 관련 화면 재조회 방식으로 먼저 간다.
- 나중에 정리할 일
  - cache/event/read-model invalidation을 별도 전역 정책으로 승격할지 판단한다.
- 닫히는 조건
  - dashboard, workers, wage summary, docs/PDF까지 포함한 invalidation 또는 cache 무효화 전략이 정리된다.

# Reading Order
1. `docs/web/implementation-slices.md`
2. `docs/execplans/active/2026-03-19-web-correction-request-flow.md`
3. `docs/web/correction-request-flow.md`
4. 이후 이 active review note

# Decision Rule For Next Session
- 다음 대화에서 Slice 5를 이어가면 먼저 이 문서의 `Deferred Work Order`를 확인한다.
- 각 항목은 `deferred`, `temporary`, `shared_policy_pending`, `rescope`, `hardening` 중 하나로 다시 분류한다.
- `temporary`, `shared_policy_pending`, `deferred` 항목은 다음 대화 시작 문구에도 같은 표현으로 남긴다.

# Next Conversation Prompt
```text
Slice 5 correction request flow backend로 이어가자.

먼저 아래 문서부터 확인해줘:
- `docs/web/README.md`
- `docs/web/implementation-slices.md`
- `docs/web/employer-web-api-map.md`
- `docs/web/correction-request-flow.md`
- `docs/web/shared-entity-validation.md`
- `docs/execplans/active/2026-03-19-web-correction-request-flow.md`
- `docs/reviews/active/2026-03-19-web-correction-request-followups.md`
- `docs/reviews/active/2026-03-19-web-worker-read-model-followups.md`
- `docs/reviews/active/2026-03-19-web-workplace-settings-followups.md`
- `docs/reviews/active/2026-03-19-web-auth-profile-followups.md`

현재 완료 상태:
- Slice 2 auth/profile foundation 완료
- Slice 3 workplace settings foundation 완료
- Slice 4 worker directory/dashboard read-model 완료
- Slice 5 correction request flow foundation 진행 중
- `GET /api/employer/correction-requests` foundation 완료
- `GET /api/employer/correction-requests/{requestId}` foundation 완료
- `GET /api/employer/issues` foundation 완료
- `POST /api/employer/correction-requests/{requestId}/approve` foundation 완료
- `POST /api/employer/correction-requests/{requestId}/reject` foundation 완료
- `POST /api/workproof/{workProofId}/correction-requests` foundation 완료
- `CorrectionRequest`, `CorrectionDecisionAudit`, `WorkProofAuditLog` 연동 완료
- backend employer/workproof targeted tests 통과 완료

이번 대화 범위:
- Slice 5 correction request flow backend/docs 이어서 진행
- legacy worker direct edit endpoint 정책 정리
- attachment/detail surface 후속 정리
- review-required record detail/command 후속 정리
- 가능하면 invalidation hardening 기준까지 정리

후속 작업 순서:
- 1. legacy direct edit endpoint lifecycle 정리
- 2. dashboard/wage/docs invalidation strategy hardening 정리
- 3. attachment detail surface 후속 정리
- 4. review-required record detail/command 후속 정리
- 5. worker direct edit flow migration 후속 범위 정리

임시/후속 항목:
- `deferred`: worker correction request create backend는 구현됐지만 worker client migration은 아직 미구현이며 mobile 범위는 현재 제외
- `temporary / shared_policy_pending`: correction detail은 현재 `attachmentCount`와 attachment metadata(`type`, `fileName`)까지만 노출
- `deferred`: employer issue queue는 `GET /api/employer/issues`로 열렸지만 review-required record detail/command는 아직 미구현

제외 범위:
- 기존 앱 API 변경
- mobile 변경
- multi-workplace switcher 구현

작업 시작 전에 현재 코드 기준으로 correction request flow에서 다음으로 고정해야 할 계약/가정/리스크를 짧게 정리하고 바로 구현해줘.
```
