# 성능 개선 보고서 템플릿

## 대상

- stage:
- endpoint:
- 고정 시나리오:
- fixture size:
- fixture 설명:
- 측정 환경:
- baseline provenance:
- 실행 명령:
- fallback 또는 blocked reason:

## Baseline

| 항목 | 값 |
| --- | --- |
| 평균 응답시간 |  |
| p95 또는 최대값 |  |
| prepared statement count 또는 query-count proxy |  |
| 비고 |  |

## 병목 원인

- 

## 적용한 수정

- 

## After

| 항목 | 값 |
| --- | --- |
| 평균 응답시간 |  |
| p95 또는 최대값 |  |
| prepared statement count 또는 query-count proxy |  |
| 비고 |  |

## 기능 유지 확인

- 유지한 API contract:
- 유지한 주요 에러 경로:
- 회귀 테스트 또는 기능 테스트:

## 근거

- 관련 controller / service call path:
- repository query:
- SQL log excerpt:
- `EXPLAIN ANALYZE` 요약:

## 잔여 리스크

- 

## 용어/측정 기준 메모

- 평균 응답시간: 전체 요청 시간의 산술 평균
- p95: 전체 요청 중 95%가 이 시간 이하로 끝난다는 의미
- query-count proxy: Hibernate statistics 같은 간접 지표를 뜻하며 실제 DB query 수와 동일하다고 단정하지 않는다
- prepared statement count: DB에 준비 또는 실행된 SQL 수를 나타내는 프록시 지표
- baseline provenance: 개선 전 수치를 어느 브랜치, 커밋, 하네스, 명령으로 수집했는지 기록한 정보
- 측정 환경: DB 종류, 데이터 조건, 실행 횟수, 로컬 또는 컨테이너 여부 같은 비교 전제

## 요약

- 
