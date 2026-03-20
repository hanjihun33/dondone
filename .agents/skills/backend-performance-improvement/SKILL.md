---
name: backend-performance-improvement
description: Use when improving DonDone backend performance with measurable before/after numbers that can be defended in review. Focus on Spring Boot, JPA, Querydsl, and PostgreSQL bottlenecks in workproof, advance, wage, documents, or claim flows.
---

# Backend Performance Improvement

DonDone 백엔드에서 감으로 최적화하지 않고, 같은 조건의 before/after 수치와 근거를 남기며 성능을 개선할 때 쓴다.

## 언제 쓰는가

- `workproof`, `advance`, `wage`, `documents`, `claim` API의 응답 경로를 줄이거나 읽기 비용을 낮출 때
- JPA, Querydsl, PostgreSQL 병목을 추적하고 작은 구조 개선으로 수치를 만들고 싶을 때
- 코드 변경을 리뷰, 포트폴리오, 후속 endpoint 작업까지 재사용 가능한 형태로 남기고 싶을 때

다음 경우에는 이 스킬보다 다른 흐름이 더 맞다.

- 단순 코드 정리만 하는 경우
- 측정 없이 체감만으로 최적화하는 경우
- auth, security, contract 변경이 핵심인 경우

## DonDone 우선 후보

보통 아래 순서로 먼저 본다.

1. `GET /api/workproof/monthly-summary`
2. `GET /api/advance/eligibility`
3. `GET /api/wage/summary`
4. `GET /api/wage/estimate`
5. `POST /api/wage/verifications`
6. `GET /api/documents/{documentId}`

처음 작업은 보통 endpoint 1개만 고른다. 여러 API를 한 번에 묶지 않는다.

## 사례 문서와 재사용 자산 구분

- `docs/execplans/active/*-perf.md`
- `docs/reviews/active/*-perf.md`

위 문서는 특정 endpoint 한 건을 기록하는 사례 문서다. 다음 작업에서 본문을 그대로 복제하지 않는다. 구조와 체크 항목만 참고한다.

재사용 자산은 아래다.

- 이 `SKILL.md`
- `references/perf-report-template.md`

## Readiness Gate

endpoint가 지금 최적화할 가치가 있는 상태인지 먼저 확인한다.

- 백엔드 endpoint가 실제로 구현되어 있고 호출 가능한가
- 응답 contract가 곧 바뀔 상태는 아닌가
- 최소한의 validation 또는 기능 검증이 이미 있는가
- mobile이나 downstream이 이미 사용 중이거나 곧 연결될 예정인가
- 담당자와 범위가 명확해서 잘못된 영역을 건드리는 상황은 아닌가

여기서 `아직 아님`이 나오면 바로 최적화하지 말고 readiness 메모만 남긴다.

## Pilot First

처음에는 큰 최적화 작업으로 바로 가지 않는다.

1. 우선순위가 높은 API 1개를 고른다.
2. baseline 계획만 먼저 잡는다.
3. 1차 병목 가설을 코드 기준으로 정리한다.
4. 짧게 교차검토한다.
5. 그 다음 실제 수정을 시작한다.

보통 첫 pilot은 아래 중 하나가 적당하다.

- `GET /api/advance/eligibility`
- `GET /api/wage/summary`
- `GET /api/workproof/monthly-summary`

## Required Workflow

### 0. Preflight Readiness Check

- 현재 구현 완성도를 확인한다.
- mobile/downstream 연결 여부를 확인한다.
- 곧 다시 뒤집힐 contract인지 확인한다.
- 지금 내가 건드려도 되는 lane인지 확인한다.

이 gate를 통과한 endpoint만 다음 단계로 넘긴다.

### 0.5. 성능 단계 구분

이번 작업이 어떤 단계인지 먼저 정한다.

- `기본기 최적화 단계 (baseline optimization)`
  - 중복 조회, 반복 read, 비효율적인 join, 요청 1건 기준 낭비를 줄일 때 쓴다.
  - 같은 조건에서 요청 1건이 더 싸졌는지를 증명하는 단계다.
- `부하 검증 단계 (load validation)`
  - 동시성, connection pool 압박, p95/p99, 대용량 데이터, lock, sustained throughput을 볼 때 쓴다.
  - 실제 부하 테스트를 하지 않았으면 이 단계라고 쓰지 않는다.

현재 DonDone 레포의 첫 성능 개선은 대부분 `기본기 최적화 단계`에 해당한다.

### 1. Scope One Endpoint

- endpoint 하나만 고른다.
- 입력 시나리오를 고정한다.
- 데이터 규모 가정을 적는다.
- 성공 기준을 숫자로 적는다.

예시:

- 대상: `GET /api/advance/eligibility?workplaceId=1`
- 조건: 근무 기록 1000건, 계약 1건, 사용자 1명
- 목표: prepared statement 12개 -> 4개 이하

### 2. Capture Baseline

최소한 아래를 남긴다.

- 측정 날짜
- 브랜치 또는 커밋
- 측정 환경
- 입력 데이터 조건
- 평균 응답시간
- p95 또는 반복 측정 최대값
- prepared statement count 또는 query-count proxy
- 주요 로그/프로파일 근거

기본 fallback 순서는 아래와 같다.

- Docker/Testcontainers가 막히면 H2 + Hibernate statistics로 query-count 기준선을 먼저 고정한다.
- PostgreSQL 실측은 기본 회귀 테스트와 분리된 전용 task 또는 전용 tag로 둔다.
- Testcontainers가 끝까지 막히면 외부 Docker PostgreSQL을 띄우고 env 기반 datasource perf test로 실측을 이어간다.

같은 시나리오는 최소 5회 이상 반복해 평균을 남긴다.

### 2.5. 성능 측정 전용 DB 사용

성능 측정 테스트를 공유 로컬 개발 DB에 바로 붙이지 않는다.

- 성능 측정 전용 PostgreSQL DB 또는 전용 컨테이너를 우선 사용한다.
- 테스트 시작 전에 `deleteAll()`로 테이블을 비우는 구조라면, 그 DB는 버려도 되는 측정용 DB로 취급한다.
- 이 레포에서는 local compose로 띄운 외부 Docker PostgreSQL이 가장 안전한 기본 경로다.
- direct Testcontainers는 현재 머신에서 Docker discovery가 안정적일 때만 선택한다.

### 2.6. 최소 증빙 세트 수집

기본기 최적화 단계에서는 환경이 허용하는 범위에서 아래를 모은다.

- before prepared statement count 또는 query-count proxy
- after prepared statement count 또는 query-count proxy
- 같은 시나리오 기준 before PostgreSQL latency
- 같은 시나리오 기준 after PostgreSQL latency
- API contract와 주요 에러 경로 유지 여부

`fixture`는 테스트 전에 고정해두는 입력 데이터 묶음을 뜻한다.

- 예: 사용자 1명, 사업장 1개, 계약 1개, 근무기록 10개

`baseline provenance`도 반드시 남긴다.

- baseline branch or commit
- baseline harness(H2 or external PostgreSQL)
- baseline collection command

PostgreSQL before/after latency 확보가 막히면 pending 사유를 문서에 명시하고, query-count before/after 증빙은 반드시 남긴다.

### 3. Find The Bottleneck

병목 원인은 아래 분류로 본다.

- N+1 또는 lazy loading
- 과도한 aggregate 집계
- DTO projection 부재
- 인덱스 부족
- 조건식 비효율
- 중복 query
- 과한 payload
- 서비스 계층 중복 호출

추정만 하지 말고 근거를 남긴다.

예시 근거:

- Hibernate SQL 로그
- `EXPLAIN ANALYZE`
- prepared statement count
- 서비스 호출 체인

### 4. Apply The Smallest Defensible Fix

보통 아래 순서로 본다.

1. 중복 호출 제거
2. fetch 전략 정리
3. DTO projection 또는 전용 query 추가
4. 인덱스 보강
5. 계산 경로 단순화

처음부터 캐시를 넣지 않는다. 캐시는 마지막 단계다.

### 5. Re-Measure With The Same Scenario

반드시 baseline과 같은 조건으로 재측정한다.

- 같은 endpoint
- 같은 fixture 규모
- 같은 반복 횟수
- 같은 로그 기준

다른 조건의 수치를 섞지 않는다.

### 6. Summarize The Result

결과는 아래 질문에 답하게 쓴다.

1. 무엇이 느렸는가
2. 왜 느렸는가
3. 무엇을 바꿨는가
4. 수치가 어떻게 좋아졌는가

표현 가이드:

- `prepared statement count`와 `query count`를 같은 의미처럼 섞어 쓰지 않는다.
- Hibernate `prepareStatementCount`를 썼다면 그 사실을 명시한다.
- PostgreSQL after-only 수치만 있으면 `개선됐다`고 단정하지 말고 `개선 후 관측치`라고 적는다.
- shared path를 타는 다른 endpoint는 별도 수치가 없으면 `영향 가능성` 또는 `후속 측정 대상`으로만 적는다.
- 로컬 측정이면 로컬이라고 쓴다.
- 추정값과 실측값을 섞지 않는다.

## Short Cross-Review

실제 수정 전에 짧게 아래 3개 관점으로 점검한다.

- `implementer`
  - 이 계획으로 바로 손을 움직일 수 있는가
  - 범위가 너무 넓지 않은가
- `reviewer`
  - 병목 추정에 근거가 있는가
  - 기능 변경과 성능 변경이 섞이지 않았는가
- `tester`
  - 같은 조건에서 before/after 비교가 가능한가
  - 반복 측정 기준이 분명한가

길게 하지 않는다. 핵심 의문만 잡는 용도다.

## Feed Learnings Back

pilot이나 실제 개선 작업에서 아래가 드러나면 스킬을 갱신한다.

- 불필요한 단계가 있었다
- DonDone 코드 구조와 안 맞는 일반론이 있었다
- baseline 기준이 애매했다
- 자주 막히는 환경 요소가 있었다
- 어떤 API부터 봐야 하는지 더 명확해졌다
- 로컬 환경 제약 때문에 대체 측정 경로가 필요했다
- Docker Desktop/Windows named pipe 같은 환경 이슈를 실행 가이드에 명시할 필요가 있었다
- Testcontainers fallback만으로 부족하면 외부 Docker 컨테이너 기반 측정 루트도 준비할 필요가 있었다

스킬은 한 번 만들고 끝내는 문서가 아니라, 실제 작업을 태우면서 다듬는 작업 기준서로 유지한다.

## DonDone-Specific Guardrails

- 공개 경로나 JWT 보호 규칙을 깨지 않는다.
- 사용자 소유권 조건을 우회하는 조회 최적화는 하지 않는다.
- 급여/정산 로직의 결과값을 바꾸면 성능 개선이 아니라 동작 변경으로 본다.
- `documents`, `claim`은 비동기 작업과 상태 흐름을 유지한다.
- 성능 개선 과정에서도 evidence-first 메시지와 테스트 범위를 섞지 않는다.

## Recommended Evidence

- 관련 controller
- service call path
- repository query
- SQL log excerpt
- `EXPLAIN ANALYZE` 요약
- before/after 수치 표

필요 시 참고 템플릿:

- `references/perf-report-template.md`

## Output Expectations

최종 산출물에는 아래가 있어야 한다.

- 성능 단계 구분
- 대상 endpoint
- 고정 시나리오
- fixture size
- fixture 설명
- 측정 환경
- baseline provenance
- 실행 명령
- before query-count proxy
- after query-count proxy
- before PostgreSQL latency 또는 pending 사유
- after PostgreSQL latency 또는 pending 사유
- 기능 유지 여부
- 남은 리스크
- 용어/측정 기준 메모

사례 문서를 쓸 때는 `references/perf-report-template.md` 구조를 기본으로 쓴다.
