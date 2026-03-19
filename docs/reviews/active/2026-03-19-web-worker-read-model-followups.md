# Scope
- Slice 4 `Worker directory and dashboard read-model` foundation 진행 중 의도적으로 미룬 후속 작업
- 기준 구현:
  - `GET /api/employer/workers`
  - `GET /api/employer/dashboard/summary`
  - `GET /api/employer/dashboard/attendance-board`
  - `EmployerAccessScope + EmploymentMembership` scope
  - `recordStatus/reflectionStatus + attendanceStatus` read-model foundation

# Closed In Slice 4 Foundation
- employer read-model scope를 `EmployerProfile.companyId + defaultWorkplaceId`와 `EmploymentMembership` 기준으로 고정했다.
- worker list, dashboard summary, attendance board를 현재 `WorkProof`가 증명 가능한 상태만으로 열었다.
- raw source 일관성을 위해 employer read-model에도 `recordStatus`와 `reflectionStatus`를 함께 노출했다.
- worker legacy ownership(`Workplace.user`, `WorkProof.user`)은 authz source로 쓰지 않도록 유지했다.
- `GET /api/employer/workers/{workerId}`를 같은 scope 규칙으로 열고, detail 응답을 `latestRecord + recentDays(최근 7일)` foundation으로 고정했다.

# Deferred Work Order
## 1. worker profile canonical source 결정
- 라벨: `temporary`, `shared_policy_pending`
- 현재 `employeeCode`, `team`, `role`, `phone`, `avatarUrl`는 canonical source가 없어서 `null` 허용으로 뒀다.
- 현재 임시 처리
  - `GET /api/employer/workers`, `GET /api/employer/workers/{workerId}`에서 위 필드는 `null` 허용으로 유지한다.
- 지금 확정하지 않은 이유
  - worker signup, contract, membership 어느 흐름에서도 이 필드를 생성/수정하는 source-of-truth가 없다.
  - web만 보고 먼저 정하면 app/web 공통 worker profile 정책과 충돌할 수 있다.
- 닫히는 조건
  - app/web 공통 worker profile policy를 정하고 source를 `User 확장`, `WorkerProfile`, 별도 projection 중 하나로 고정한다.
- 다음 단계에서 결정할 것:
  - 기존 `User` 확장으로 갈지
  - `WorkerProfile` 별도 엔티티를 열지
  - employer web 전용 projection으로 버틸지
- 이 결정 전에는 프론트 mockup의 풍부한 worker card 필드를 backend 계약으로 과장하지 않는다.

## 2. 지각/휴가/결근 canonical source 재스코프
- 라벨: `temporary`, `shared_policy_pending`
- 현재 employer dashboard mockup에는 `late/leave/absent`가 있지만 backend source-of-truth는 아직 없다.
- 현재 임시 처리
  - summary/board는 `WORKING/COMPLETED/NEEDS_REVIEW/NO_RECORD` foundation 상태만 유지한다.
- 지금 확정하지 않은 이유
  - `WorkProof`만으로는 지각/휴가/결근을 안정적으로 판정할 공통 정책과 입력 source가 부족하다.
  - app/web이 같은 상태축을 써야 하는데 schedule/leave/correction 책임 경계가 아직 열리지 않았다.
- 닫히는 조건
  - schedule/leave/correction 중 어떤 도메인이 canonical source인지 고정하고 app/web 공통 상태 해석 규칙을 맞춘다.
- 다음 단계에서 정리할 것:
  - lateness를 schedule/contract/start-time로 계산할지
  - leave를 별도 leave domain으로 둘지
  - absent를 “해당일 no record”와 구분할 기준이 필요한지
- 이 축이 고정되기 전까지 summary/board의 foundation 상태(`WORKING/COMPLETED/NEEDS_REVIEW/NO_RECORD`)를 유지한다.

## 3. attendance-board 확장 규칙
- 현재 board는 `weekStart` 기준 7일 범위, row filter는 “주간 중 하루라도 status 일치” 규칙으로 시작했다.
- 다음 단계에서 재평가할 것:
  - page/hasNext만으로 충분한지
  - 날짜축 정렬/locale 노출을 API에서 더 가져갈지
  - week-based query를 dedicated read-model이나 Querydsl projection으로 옮길지

## 4. Querydsl/read-model 분리 시점 판단
- foundation 단계는 service 조합으로 구현했다.
- 아래 신호가 나오면 분리 후보로 올린다.
  - scoped worker 수 증가로 성능 저하가 보일 때
  - search/status/week filter 조합이 더 복잡해질 때
  - worker detail/correction queue와 동일 worker snapshot을 여러 endpoint가 반복 조합하기 시작할 때

## 5. 기존 app API contract와 worker legacy ownership 범위 재검증
- Slice 4 구현이 커질수록 employer web이 기존 worker API나 legacy ownership에 다시 기대는지 점검해야 한다.
- 다음 단계에서도 계속 확인할 것:
  - employer endpoint가 기존 `/api/workproof/*` contract 변경을 요구하지 않는지
  - `Workplace.user`, `WorkProof.user`가 authz source처럼 다시 쓰이지 않는지
  - shared entity 변경이 worker app flow를 직접 깨지 않는지
- 이 항목은 별도 feature가 아니라 Slice 4 진행 중 계속 닫아야 하는 검증 작업이다.

# Reading Order
1. `docs/web/implementation-slices.md`
2. `docs/execplans/active/2026-03-19-web-worker-directory-dashboard-read-model.md`
3. 이 문서
4. 이후 새 active execplan 또는 review note

# Decision Rule For Next Session
- 다음 대화에서 Slice 4를 이어가면 먼저 이 문서의 `Deferred Work Order`를 확인한다.
- 각 항목은 `now`, `hardening`, `rescope` 중 하나로 다시 분류한다.
- `temporary`, `shared_policy_pending` 항목은 다음 대화 시작 문구에도 같은 표현으로 남긴다.
- 새로 미루는 항목이 생기면 이 문서나 후속 review note에 즉시 추가한다.

# Next Conversation Prompt
```text
Slice 4 backend/read-model로 이어가자.

먼저 아래 문서부터 확인해줘:
- `docs/web/README.md`
- `docs/web/implementation-slices.md`
- `docs/web/employer-web-api-map.md`
- `docs/web/employer-worker-domain-map.md`
- `docs/web/shared-entity-validation.md`
- `docs/execplans/active/2026-03-19-web-worker-directory-dashboard-read-model.md`
- `docs/reviews/active/2026-03-19-web-worker-read-model-followups.md`
- `docs/reviews/active/2026-03-19-web-workplace-settings-followups.md`

현재 완료 상태:
- Slice 2 auth/profile foundation 완료
- Slice 3 workplace settings foundation 완료
- `GET /api/employer/workplace-settings`, `PUT /api/employer/workplace-settings` 완료
- Slice 4 foundation 진행 중
- `GET /api/employer/workers` 완료
- `GET /api/employer/workers/{workerId}` foundation 완료
- `GET /api/employer/dashboard/summary` foundation 완료
- `GET /api/employer/dashboard/attendance-board` foundation 완료
- `EmployerAccessScope + EmploymentMembership` 기준 read-model scope 고정 완료
- employer read-model에 `recordStatus/reflectionStatus + attendanceStatus` 조합 반영 완료
- backend employer targeted tests 통과 완료

이번 대화 범위:
- Slice 4 `Worker directory and dashboard read-model` backend 이어서 진행
- worker profile canonical source 빈 칸(`employeeCode`, `team`, `role`, `phone`, `avatarUrl`) 후속 방향 정리
- 가능하면 `late/leave/absent` canonical source 재스코프 초안까지 이어가기

후속 작업 순서:
- 1. worker profile canonical source 결정
- 2. `late/leave/absent` canonical source 재스코프
- 3. attendance-board 확장 규칙 재평가
- 4. Querydsl/read-model 분리 시점 판단
- 5. 기존 app API contract와 worker legacy ownership 범위 재검증

임시 처리된 공통 정책 항목:
- `temporary / shared_policy_pending`: `employeeCode`, `team`, `role`, `phone`, `avatarUrl`는 현재 canonical source가 없어 `null` 허용 유지
- `temporary / shared_policy_pending`: `late/leave/absent`는 현재 canonical source가 없어 summary/board에서 미노출, foundation 상태만 유지

제외 범위:
- 기존 앱 API 변경
- correction request flow 본구현
- mobile 변경
- multi-workplace switcher 구현

작업 시작 전에 현재 코드 기준으로 다음으로 고정해야 할 계약/가정/리스크를 짧게 정리하고 바로 구현해줘.
```
