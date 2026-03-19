# Scope
- Slice 5 `Correction request flow` foundation 이후 남은 backend/docs 후속 정리
- 기준 범위
  - `GET /api/employer/issues`
  - `GET /api/employer/correction-requests`
  - `GET /api/employer/correction-requests/{requestId}`
  - `POST /api/employer/correction-requests/{requestId}/approve`
  - `POST /api/employer/correction-requests/{requestId}/reject`
  - `POST /api/workproof/{workProofId}/correction-requests`
  - `PATCH /api/workproof/{id}` deprecated legacy surface

# Closed In Slice 5 Foundation
- employer correction queue/history/command foundation 완료
- worker correction request create backend 완료
- correction detail attachment metadata(`type`, `fileName`) 노출 완료
- employer issue queue foundation 완료
  - `PENDING` correction request
  - `NEEDS_REVIEW` record
  - `itemType`으로 함께 노출
- legacy direct edit endpoint 정책 고정 완료
  - `PATCH /api/workproof/{id}`는 deprecated legacy surface로 유지
  - 새 클라이언트는 `POST /api/workproof/{workProofId}/correction-requests` 사용
- invalidation policy 고정 완료
  - 별도 cache/event invalidation 없음
  - approve/reject 후 다음 조회에서 source-of-truth 재조회로 최신 반영

# Deferred Work Order
## 1. attachment detail surface
- 라벨: `temporary`, `shared_policy_pending`
- 현재 임시 처리
  - correction detail 응답은 `attachmentCount`와 attachment metadata(`type`, `fileName`, `downloadAvailable=false`)까지만 노출한다.
  - request와 `WorkProof`에는 attachment metadata json을 내부 보존용으로 저장한다.
- 지금 확정하지 않은 이유
  - employer web에서 첨부를 목록/상세 어디에 어떻게 보여줄지와 download contract가 아직 닫히지 않았다.
- 닫히는 조건
  - attachment metadata의 web 표시 위치와 download contract가 고정된다.

## 2. review-required record detail/command surface
- 라벨: `deferred`
- 현재 상태
  - employer issue queue foundation은 `GET /api/employer/issues`까지 열려 있다.
  - `NEEDS_REVIEW` record는 목록 row만 있고 별도 detail/resolve surface는 없다.
- 지금 확정하지 않은 이유
  - correction request approval flow foundation을 먼저 닫았고, review-needed record의 별도 command/state transition은 아직 제품 정책이 더 필요하다.
- 닫히는 조건
  - `NEEDS_REVIEW` record용 employer detail/resolve contract 또는 accepted risk가 고정된다.

## 3. worker direct edit flow migration
- 라벨: `deferred`
- 현재 상태
  - backend worker create endpoint는 이미 `POST /api/workproof/{workProofId}/correction-requests`로 열려 있다.
  - mobile worker direct edit는 아직 local-only mock이다.
  - backend `PATCH /api/workproof/{id}`는 deprecated legacy로만 유지한다.
- 지금 확정하지 않은 이유
  - 현재 대화 범위는 backend/API 우선이며 mobile 변경은 제외다.
- 닫히는 조건
  - mobile/client가 correction request submit flow로 전환되고 direct edit mock을 대체한다.

## 4. web issues API wiring
- 라벨: `deferred`
- 현재 상태
  - web issues 화면은 아직 mock/local state 기반이다.
  - backend에는 `GET /api/employer/issues`와 correction approve/reject API가 준비되어 있다.
- 지금 확정하지 않은 이유
  - 현재 범위는 backend/API 우선이며 web 구현은 후속이다.
- 닫히는 조건
  - web issues 화면이 backend issue queue read-model과 command를 실제로 연결한다.

# Fixed In This Session
## dashboard/wage/docs invalidation strategy
- 라벨: `fixed`
- 결정
  - MVP에서는 별도 cache/event invalidation을 두지 않는다.
  - employer dashboard/workers/issues, worker wage summary, docs/PDF는 다음 조회에서 source-of-truth를 다시 읽어 최신 상태를 반영한다.
- 근거
  - approve/reject는 `CorrectionRequest`, `WorkProof`, `WorkProofAuditLog`, `CorrectionDecisionAudit`를 한 transaction에서 갱신한다.
  - 현재 backend에는 dedicated cache layer나 materialized projection invalidation이 없다.
- 다시 열리는 조건
  - cache, projection store, precomputed artifact가 실제로 추가될 때만 hardening 항목으로 재오픈한다.

# Reading Order
1. `docs/web/implementation-slices.md`
2. `docs/execplans/active/2026-03-19-web-correction-request-flow.md`
3. `docs/web/correction-request-flow.md`
4. 이 follow-up note

# Decision Rule For Next Session
- 다음 대화에서 Slice 5를 이어가면 먼저 이 문서의 `Deferred Work Order`를 확인한다.
- 각 항목은 `fixed`, `deferred`, `temporary`, `shared_policy_pending`, `hardening` 중 하나로 다시 분류한다.
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
- `GET /api/employer/issues` foundation 완료
- `GET /api/employer/correction-requests` foundation 완료
- `GET /api/employer/correction-requests/{requestId}` foundation 완료
- `POST /api/employer/correction-requests/{requestId}/approve` foundation 완료
- `POST /api/employer/correction-requests/{requestId}/reject` foundation 완료
- `POST /api/workproof/{workProofId}/correction-requests` foundation 완료
- `PATCH /api/workproof/{id}` deprecated legacy 정책 고정 완료
- correction detail attachment metadata(`type`, `fileName`) 노출 완료
- invalidation policy는 `다음 조회 source-of-truth 재조회`로 고정 완료
- backend employer/workproof targeted tests 통과 완료

이번 대화 범위:
- Slice 5 correction request flow backend/docs 후속 정리
- attachment/detail surface 후속 정리
- review-required record detail/command 후속 정리
- 가능하면 worker direct edit flow migration 후속 범위 정리

후속 작업 순서:
- 1. attachment/detail surface 후속 정리
- 2. review-required record detail/command 후속 정리
- 3. worker direct edit flow migration 후속 범위 정리
- 4. web issues API wiring 후속 범위 정리

임시/후속 항목:
- `temporary / shared_policy_pending`: correction detail은 현재 `attachmentCount`와 attachment metadata(`type`, `fileName`)까지만 노출
- `deferred`: `NEEDS_REVIEW` record detail/command surface는 아직 미구현
- `deferred`: worker correction request create backend는 구현됐지만 worker client migration은 아직 미구현이며 mobile 범위는 현재 제외
- `deferred`: web issues 화면은 아직 mock 기반이며 backend issue queue wiring은 후속

제외 범위:
- 기존 worker API 제거
- mobile 변경
- web 화면 구현
- multi-workplace switcher 구현

작업 시작 전에 현재 코드 기준으로 correction request flow에서 남은 계약/가정/리스크를 짧게 정리하고 바로 구현해줘.
```
