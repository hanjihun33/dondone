# Findings

## 성능 단계
- `기본기 최적화 단계 (baseline optimization)`

## 대상
- Primary endpoint: `GET /api/wage/estimate`
- Shared-path follow-up target: `GET /api/wage/monthly-summary`, `POST /api/wage/verifications`
- Branch: `codex/backend-perf-skill-bootstrap`

## 고정 시나리오
- user 1명
- workplace 1개
- active contract 1개
- reflected workproof 10건
- 같은 월 범위 조회
- H2 회귀 테스트는 동일 fixture로 1회 실행
- 외부 PostgreSQL 실측은 warm-up 1회 후 5회 반복

## Baseline
- 측정 날짜: `2026-03-18`
- baseline harness: H2 in-memory DB + Hibernate statistics
- baseline provenance:
  - snapshot loader 리팩터링 전 동일 fixture에서 수집한 prepared statement 기준값
  - 현재 브랜치에는 after 회귀 테스트만 남아 있으므로, baseline `6`은 사례 기록으로 보존한 수치다
- baseline result:
  - prepared statement count: `6`

## 병목 원인
- `WageService.loadLane1Context(...)`가 기존에는 아래 세 경로를 각각 호출했다.
  - `getMonthlySummary(...)`
  - `getCurrentContract(...)`
  - `getRecords(...)`
- 이 구조 때문에 같은 `userId + month + workplaceId` 조합에 대해
  - workplace ownership 조회가 반복되고
  - 월별 workproof 조회가 중복됐다

## 적용한 수정
- `WorkProofLane1Service.getWageLane1Snapshot(...)`를 추가해 아래 lane1 입력을 한 번에 조합하도록 바꿨다.
  - workplace ownership 검증 1회
  - active contract 조회 1회
  - month-scoped workproof 조회 1회
- `WageService.loadLane1Context(...)`는 이 snapshot을 사용하도록 정리했다.
- 응답 DTO, 권한 경로, active contract 에러 경로는 유지했다.

## After
- H2 회귀 테스트 기준:
  - prepared statement count: `3`
  - improvement: `6 -> 3`
  - reduction rate: `50%`
- 외부 Docker PostgreSQL 기준 개선 후 관측치:
  - measurement date: `2026-03-19`
  - datasource: local compose PostgreSQL on `localhost:5433`
  - average prepared statement count: `3`
  - average latency: `27 ms`
  - max latency: `67 ms`

## 실행 및 검증 경로
- H2 회귀 방지:
  - `integrationTest --tests com.workproofpay.backend.wage.WagePerformanceIntegrationTest`
- 기능 회귀 확인:
  - `integrationTest --tests com.workproofpay.backend.wage.WageDemoIntegrationTest`
- 외부 PostgreSQL 실측:
  - `externalDockerPerfTest --tests com.workproofpay.backend.wage.WageExternalPostgresPerformanceIntegrationTest`
- 전제 환경 변수:
  - `PERF_PG_URL`
  - `PERF_PG_USERNAME`
  - `PERF_PG_PASSWORD`
  - `PERF_PG_ALLOW_RESET=true`

## 측정 환경 메모
- 외부 PostgreSQL 실측은 공유 개발 DB가 아니라 버려도 되는 측정용 DB 또는 측정용 컨테이너에 붙여야 한다.
- 현재 테스트는 `create-drop`과 `deleteAll()`를 사용하므로, 잘못된 DB를 물리면 위험하다.
- 현재 외부 실측 테스트는 `PERF_PG_ALLOW_RESET=true`가 없으면 바로 실패하도록 되어 있다.
- direct Testcontainers 경로는 이 브랜치에서 제거했다. 현재 저장소 기준의 PostgreSQL 실측 경로는 `WageExternalPostgresPerformanceIntegrationTest` 하나다.

## 근거
- controller path: `GET /api/wage/estimate`
- changed services:
  - `com.workproofpay.backend.wage.service.WageService`
  - `com.workproofpay.backend.workproof.service.WorkProofLane1Service`
- regression tests:
  - `com.workproofpay.backend.wage.WagePerformanceIntegrationTest`
  - `com.workproofpay.backend.wage.WageDemoIntegrationTest`
  - `com.workproofpay.backend.wage.WageExternalPostgresPerformanceIntegrationTest`
- raw measured result:
  - `apps/dondone-backend/build/test-results/externalDockerPerfTest/TEST-com.workproofpay.backend.wage.WageExternalPostgresPerformanceIntegrationTest.xml`

## 표현 가이드
- 이 문서의 `prepared statement count`는 Hibernate statistics 기반 프록시 지표다.
- 이 문서는 `prepared statement 6 -> 3 감소`는 강하게 말할 수 있다.
- PostgreSQL latency는 현재 before/after가 모두 확보된 수치가 아니므로 `개선 후 관측치`로만 적는다.
- shared path를 타는 `monthly-summary`, `verifications`는 구조상 영향 가능성은 있지만, 별도 수치가 없으므로 이 문서에서는 성과로 단정하지 않는다.
- 외부 PostgreSQL 실측 테스트는 평균값만 보지 않고 각 run이 `prepared statement <= 3`인지도 함께 검증한다.

## 잔여 리스크
- PostgreSQL before latency가 없어 응답시간 개선폭 자체를 강하게 주장하긴 어렵다.
- 현재 fixture는 작고 단순하다. 대용량 데이터나 동시성 상황을 대표하지 않는다.
- `WorkProofMonthlySummaryContractResponse` 계산은 여전히 여러 stream pass를 사용하므로 다음 최적화 여지는 남아 있다.
- `GET /api/wage/monthly-summary`, `POST /api/wage/verifications`는 별도 측정이 아직 없다.

## 후속 권장
- 같은 외부 PostgreSQL 경로로 before/after latency를 모두 수집하면 포트폴리오 방어력이 더 올라간다.
- 다음 endpoint 사례에서는 baseline provenance, 실행 명령, fixture 설명, pending 사유를 템플릿 기준으로 처음부터 같이 남긴다.
