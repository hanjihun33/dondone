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

# Deferred Work Order
## 1. `GET /api/employer/workers/{workerId}` 계약 고정 및 구현
- 현재는 list/board/summary까지만 열었다.
- 다음 작업에서는 detail endpoint가 아래를 어떻게 조합할지 고정해야 한다.
  - worker 기본 식별 정보
  - latest scoped record snapshot
  - 최근 7일 또는 이번 달 집계 범위
  - correction flow가 붙기 전까지 어떤 review context를 내려줄지
- bare `workerId`를 받는 만큼 `EmploymentMembership` 기준 target 재검증을 endpoint/service/test에서 닫아야 한다.

## 2. worker profile canonical source 결정
- 현재 `employeeCode`, `team`, `role`, `phone`, `avatarUrl`는 canonical source가 없어서 `null` 허용으로 뒀다.
- 다음 단계에서 결정할 것:
  - 기존 `User` 확장으로 갈지
  - `WorkerProfile` 별도 엔티티를 열지
  - employer web 전용 projection으로 버틸지
- 이 결정 전에는 프론트 mockup의 풍부한 worker card 필드를 backend 계약으로 과장하지 않는다.

## 3. 지각/휴가/결근 canonical source 재스코프
- 현재 employer dashboard mockup에는 `late/leave/absent`가 있지만 backend source-of-truth는 아직 없다.
- 다음 단계에서 정리할 것:
  - lateness를 schedule/contract/start-time로 계산할지
  - leave를 별도 leave domain으로 둘지
  - absent를 “해당일 no record”와 구분할 기준이 필요한지
- 이 축이 고정되기 전까지 summary/board의 foundation 상태(`WORKING/COMPLETED/NEEDS_REVIEW/NO_RECORD`)를 유지한다.

## 4. attendance-board 확장 규칙
- 현재 board는 `weekStart` 기준 7일 범위, row filter는 “주간 중 하루라도 status 일치” 규칙으로 시작했다.
- 다음 단계에서 재평가할 것:
  - page/hasNext만으로 충분한지
  - 날짜축 정렬/locale 노출을 API에서 더 가져갈지
  - week-based query를 dedicated read-model이나 Querydsl projection으로 옮길지

## 5. Querydsl/read-model 분리 시점 판단
- foundation 단계는 service 조합으로 구현했다.
- 아래 신호가 나오면 분리 후보로 올린다.
  - scoped worker 수 증가로 성능 저하가 보일 때
  - search/status/week filter 조합이 더 복잡해질 때
  - worker detail/correction queue와 동일 worker snapshot을 여러 endpoint가 반복 조합하기 시작할 때

## 6. 기존 app API contract와 worker legacy ownership 범위 재검증
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
- `GET /api/employer/dashboard/summary` foundation 완료
- `GET /api/employer/dashboard/attendance-board` foundation 완료
- `EmployerAccessScope + EmploymentMembership` 기준 read-model scope 고정 완료
- employer read-model에 `recordStatus/reflectionStatus + attendanceStatus` 조합 반영 완료
- backend employer targeted tests 통과 완료

이번 대화 범위:
- Slice 4 `Worker directory and dashboard read-model` backend 이어서 진행
- `GET /api/employer/workers/{workerId}` 계약/DTO/API/service/test 뼈대 구현
- 가능하면 worker profile canonical source 빈 칸(`employeeCode`, `team`, `role`, `phone`, `avatarUrl`) 후속 방향까지 정리

후속 작업 순서:
- 1. `GET /api/employer/workers/{workerId}` 계약 고정 및 구현
- 2. worker profile canonical source 결정
- 3. `late/leave/absent` canonical source 재스코프
- 4. attendance-board 확장 규칙 재평가
- 5. Querydsl/read-model 분리 시점 판단
- 6. 기존 app API contract와 worker legacy ownership 범위 재검증

제외 범위:
- 기존 앱 API 변경
- correction request flow 본구현
- mobile 변경
- multi-workplace switcher 구현

작업 시작 전에 현재 코드 기준으로 다음으로 고정해야 할 계약/가정/리스크를 짧게 정리하고 바로 구현해줘.
```
