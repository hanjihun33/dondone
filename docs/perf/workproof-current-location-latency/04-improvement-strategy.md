# Improvement Strategy

## 왜 한 번에 다 고치지 않는가
위치 지연은 아래가 섞여 있다.

- provider 응답
- fallback 판단
- action path 호출 구조

한 번에 동시에 바꾸면 어떤 변경이 효과였는지 설명하기 어렵다. 그래서 단계적으로 바꾼다.

## 권장 순서

### 1. 중복 호출/불필요 재조회 먼저 줄이기
가장 먼저 "같은 좌표를 여러 번 찾는 비용"을 줄인다.

이 단계에서 우선 볼 것:
- single-flight(동시 fetch 병합)
- 최근 성공 좌표 짧은 TTL 캐시
- 출퇴근 submit 전 재조회 조건화

### 2. Provider 전략 개선
그다음 provider 대기 구조를 줄인다.

이 단계에서 볼 것:
- timeout 값 조정
- provider 순차 전략 재조정
- lastKnown fast-path 적용

### 3. 정확도/정책 균형 튜닝
마지막으로 정확도와 체감 지연의 균형을 맞춘다.

이 단계에서 볼 것:
- lastKnown age/accuracy threshold
- 반경 검증 실패율 변화

## 실무적으로 좋은 진행 방식
각 단계는 아래 순서로 진행한다.

1. baseline 측정
2. 작은 개선 1개 적용
3. 같은 조건 재측정
4. 결과 기록

## 피해야 할 방식
- 측정 없이 체감만으로 수정
- provider 전략/캐시/정책을 한 번에 동시 수정
- 위치 정확도 리스크 검증 없이 timeout만 공격적으로 축소

