# Scope
- Slice 5 `Correction request flow` foundation 구현 중 의도적으로 미룬 후속 작업
- 기준 구현:
  - `GET /api/employer/correction-requests`
  - `GET /api/employer/correction-requests/{requestId}`
  - `POST /api/employer/correction-requests/{requestId}/approve`
  - `POST /api/employer/correction-requests/{requestId}/reject`
  - `CorrectionRequest`
  - `CorrectionDecisionAudit`
  - `WorkProofAuditLog` 연동

# Closed In Slice 5 Foundation
- employer correction queue는 `/api/employer/*` 전용 surface로 열고 기존 worker API contract는 바꾸지 않았다.
- request scope는 MVP에서 `CorrectionRequest` snapshot의 `companyId/workplaceId`와 현재 employer scope 일치 여부로 먼저 고정했다.
- approve는 request status, `WorkProof.updateTimes(...)`, `WorkProofAuditLog`, `CorrectionDecisionAudit`를 한 transaction으로 묶었다.
- reject는 `WorkProof`를 수정하지 않고 request status와 `CorrectionDecisionAudit`만 기록하도록 고정했다.

# Deferred Work Order
## 1. worker-side correction request create implementation
- 라벨: `now`
- 현재 상태
  - shared policy는 고정했다: worker는 WorkProof를 직접 확정 수정하지 않고 correction request를 제출하고, employer가 승인/반려한다.
  - employer-side queue/decision endpoint만 먼저 열려 있고 worker-side create API는 아직 없다.
- 지금 해야 할 일
  - worker-side request create payload를 `requested times + reason + attachment metadata` 기준으로 구체화한다.
  - 기존 worker 직접 수정 flow를 correction request submit flow로 바꾸는 API/backend 범위를 정리한다.
- 닫히는 조건
  - worker-side correction request create contract와 backend endpoint가 구현된다.

## 2. attachment detail surface
- 라벨: `temporary`, `shared_policy_pending`
- 현재 임시 처리
  - correction detail 응답은 `attachmentCount`만 노출한다.
  - request와 `WorkProof`에는 attachment metadata json을 내부 보존용으로만 저장한다.
- 지금 확정하지 않은 이유
  - 첨부의 의미는 정정 요청 증빙자료로 고정했지만, employer web에서 목록/상세 어디에 어떻게 보여줄지는 아직 UI 계약으로 닫히지 않았다.
- 닫히는 조건
  - attachment metadata의 canonical response shape와 web 상세 표면을 고정한다.

## 3. correction queue vs outside-radius review boundary
- 라벨: `rescope`
- 현재 상태
  - 정책 방향은 고정했다: employer issue queue는 worker correction request와 review가 필요한 record를 둘 다 담는 방향으로 본다.
  - 다만 현재 foundation 구현은 correction request queue만 먼저 연 상태다.
- 지금 해야 할 일
  - review 대상 record를 correction queue와 같은 surface로 노출할지, 별도 탭/타입 필드로 구분할지 정리한다.
- 닫히는 조건
  - employer issue queue contract에 review 대상 record가 포함되는 방식이 고정된다.

## 4. dashboard/wage/docs invalidation strategy
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
3. 이 문서
4. 이후 새 active execplan 또는 review note

# Decision Rule For Next Session
- 다음 대화에서 Slice 5를 이어가면 먼저 이 문서의 `Deferred Work Order`를 확인한다.
- 각 항목은 `now`, `hardening`, `rescope` 중 하나로 다시 분류한다.
- `temporary`, `shared_policy_pending` 항목은 다음 대화 시작 문구에도 같은 표현으로 남긴다.

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
- `POST /api/employer/correction-requests/{requestId}/approve` foundation 완료
- `POST /api/employer/correction-requests/{requestId}/reject` foundation 완료
- `CorrectionRequest`, `CorrectionDecisionAudit`, `WorkProofAuditLog` 연동 완료
- backend employer correction targeted tests 통과 완료

이번 대화 범위:
- Slice 5 correction request flow backend 이어서 진행
- worker-side correction request create 후속 범위 정리
- attachment/detail surface와 invalidation 규칙 정리
- 가능하면 correction queue와 outside-radius review 경계 재검토

후속 작업 순서:
- 1. worker-side correction request create 구현 범위 정리
- 2. attachment detail surface 정리
- 3. correction queue vs outside-radius review boundary 정리
- 4. dashboard/wage/docs invalidation strategy hardening 정리

임시 처리된 공통 정책 항목:
- `now`: worker-side correction request create는 정책 고정 후 미구현 상태
- `temporary / shared_policy_pending`: correction detail은 현재 `attachmentCount`만 노출
- `rescope`: correction queue는 장기적으로 correction request와 review 대상 record를 둘 다 담는 방향이지만 현재는 correction request만 구현

제외 범위:
- 기존 앱 API 변경
- mobile 변경
- multi-workplace switcher 구현

작업 시작 전에 현재 코드 기준으로 correction request flow에서 다음으로 고정해야 할 계약/가정/리스크를 짧게 정리하고 바로 구현해줘.
```
