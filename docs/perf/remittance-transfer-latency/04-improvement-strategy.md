# Improvement Strategy

## 왜 한 번에 다 고치지 않는가
송금 지연은 여러 레이어가 섞여 있다.

- backend request path
- async worker path
- mobile polling path

이걸 한 번에 다 고치면 무엇이 실제로 효과가 있었는지 설명하기 어려워진다. 그래서 한 레이어씩 고친다.

## 권장 순서

### 1. Backend Request Path
가장 먼저 `POST /api/remittance/transfers` 자체를 더 싸게 만든다.

이 단계에서 우선 볼 것:
- wallet lock 획득 시점
- active transfer fast-fail 순서
- 중복 policy evaluation 비용

이 단계는 before/after 를 만들기 가장 쉽다.

### 2. Async Worker Path
그다음 `REQUESTED -> CONFIRMED` 총 시간을 줄인다.

이 단계에서 볼 것:
- poll interval
- receipt poll delay
- scheduling 방식

### 3. Mobile Polling Path
마지막으로 사용자가 실제로 보는 완료 시간을 줄인다.

이 단계에서 볼 것:
- create 직후 전체 reload 필요성
- polling 시작 시점

## 실무적으로 좋은 진행 방식
각 단계는 아래 순서로 진행한다.

1. baseline 측정
2. 병목 진단
3. 작은 구조 개선 1개 적용
4. 같은 조건 재측정
5. 결과 기록

## 피해야 할 방식
- 측정 없이 체감만으로 수정
- backend, worker, mobile 을 한 번에 동시 수정
- `demo` 와 `sepolia` 결과를 섞어서 성과로 설명
- 기능 변경과 성능 변경을 한 커밋에 섞기
