## Source Inputs

- Repository `AGENTS.md`
- `.agents/skills/execplan-writer/SKILL.md`
- `.agents/skills/implement-checklist/SKILL.md`
- `.agents/skills/test-checklist/SKILL.md`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/dto/response/WorkProofResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/dto/response/WorkProofMonthlySummaryResponse.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/workproof/WorkProofIntegrationTest.java`

## Goal

`WorkProofService`에 과도하게 몰린 생성, 판정, DTO 조립 책임을 분리해 유지보수성을 높이고, 동일 동작을 유지한 채 서비스 메서드 의도를 더 분명하게 만든다.

## In Scope

- `WorkProof` 생성 규칙 일부를 엔티티 팩토리 메서드와 도메인 메서드로 이동
- `WorkProofResponse`, `WorkProofMonthlySummaryResponse`, `WorkProofMonthlyMetrics`에 정적 팩토리 또는 헬퍼 추가
- `WorkProofService.getMonthlyMetrics()`와 `toResponse()`의 단계 분리
- 매직 넘버를 의미 있는 상수로 정리
- 기존 통합 테스트 재실행 및 필요한 최소 테스트 보강 검토

## Out of Scope

- API 요청/응답 필드 변경
- DB 스키마 변경
- 새로운 WorkProof 비즈니스 규칙 도입
- Wage/Demo 도메인 구조 정리
- 모바일 코드 변경

## Affected Modules

### Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofMonthlyMetrics.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/dto/response/WorkProofResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/dto/response/WorkProofMonthlySummaryResponse.java`
- 필요한 경우 `apps/dondone-backend/src/test/java/com/workproofpay/backend/workproof/WorkProofIntegrationTest.java`

### Mobile

- 변경 없음

### Docs

- 이 실행계획 문서

### Shared

- 변경 없음

## Contract Changes

- 외부 API 계약 변경 없음
- 내부 생성/변환 방식만 정리

## Security Notes

- 인증/인가 규칙 변경 없음
- WorkProof 소유권 검사와 validation 동작은 기존과 동일하게 유지

## Maintainability Notes

- 생성 규칙이 서비스에 남아 있으면 이후 수정 API나 시드 로직에서 중복될 가능성이 높다.
- reflected/edited 판정과 worked minutes 계산은 도메인 메서드 후보이며, 서비스가 계산 orchestration만 담당하도록 줄여야 한다.
- DTO 조립은 계산과 분리해 응답 구조 변경 시 서비스 메서드 수정 범위를 줄여야 한다.
- `getMonthlyMetrics()`는 조회, 필터링, 분류, 요약의 단계가 명확해야 후속 정책 추가 시 영향 범위를 통제할 수 있다.

## Implementation Steps

1. 실행계획을 추가하고 리팩토링 범위를 고정한다.
2. `WorkProof`에 생성/판정/기본 계산용 메서드를 추가한다.
3. `WorkProofResponse`, `WorkProofMonthlySummaryResponse`, `WorkProofMonthlyMetrics`에 정적 팩토리/헬퍼를 추가한다.
4. `WorkProofService`를 단계별 private 메서드와 상수 중심으로 정리한다.
5. 관련 테스트를 검토하고 필요한 최소 수정 후 백엔드 테스트를 실행한다.

## Test Plan

- `apps/dondone-backend/src/test/java/com/workproofpay/backend/workproof/WorkProofIntegrationTest.java`
- 회귀 확인을 위해 `GRADLE_USER_HOME=/tmp/gradle-dondone ./gradlew test --no-daemon`

## Review Focus

- 서비스에서 엔티티/DTO 생성 책임이 적절히 줄었는지
- WorkProof 도메인 메서드가 엔티티 책임을 과도하게 넓히지 않았는지
- `getMonthlyMetrics()` 단계 분리가 동작을 바꾸지 않았는지
- 매직 넘버가 의미 있는 상수로 정리됐는지

## Worktree Split Decision

Single lane

이번 작업은 동일한 `workproof` 도메인 파일들 사이에서 생성 규칙과 계산 책임을 재배치하는 리팩토링이라 병렬 작업 시 충돌 가능성이 높다. 공유 DTO와 서비스, 엔티티가 함께 움직이므로 단일 레인으로 처리한다.

## Commit Plan

- `refactor: WorkProof 서비스 유지보수 구조 정리`
- `docs: WorkProof 리팩토링 실행계획 추가`

## Open Questions

- `workedMinutes` 계산까지 엔티티에 두는 현재 구조를 overtime/night 계산까지 더 확장할지는 추후 판단이 필요하다.

## Assumptions

- 기존 API 응답 값과 테스트 기대값은 유지되어야 한다.
- 생성 규칙을 엔티티로 일부 이동해도 현재 MVP 범위에서는 과도한 도메인 추상화가 아니다.
