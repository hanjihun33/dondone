# Review Map

이 문서는 WorkProof 현재 위치 지연 경로를 다시 훑을 때 쓰는 안내서다.

핵심은 네 가지다.

- 실제로 어디까지 읽었는가
- 지금 코드상 무엇이 먼저 의심되는가
- 다음에 어떤 함수와 파일을 보면 좋은가
- 무엇이 아직 측정되지 않았는가

## 한눈에 보기

| 구간 | 이번 라운드 상태 | 지금 보이는 핵심 | 다음 초점 |
|---|---|---|---|
| Provider | 검토 완료 | provider 순차 timeout 누적이 크다 | provider별 실제 시간 분해 |
| Action Path | 검토 완료 | 출퇴근 직전 강제 재조회가 체감 지연을 키운다 | 캐시/조건부 재조회 적용 여지 |
| UI Trigger | 검토 완료 | 진입 자동조회 + 수동조회 + 제출전조회가 겹칠 수 있다 | single-flight 필요성 확인 |
| Measurement | 아직 시작 전 | 현재는 코드 기반 추정이 많다 | 액션별 baseline 수치 확보 |

## Provider

### 실제로 읽은 파일
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/location/CurrentLocationProvider.kt`

### 지금 보이는 핵심
- `GPS -> NETWORK -> PASSIVE` 순서로 순차 조회한다.
- provider당 `3500ms` timeout이 걸려 최악 지연이 길다.
- current 실패 시 `lastKnown` fallback을 시도한다.
- fine 권한이 없으면 coarse-only 상태에서도 조회를 중단한다.

### 다음에 보면 좋은 함수
- `AndroidCurrentLocationProvider.fetch()`
- `requestCurrentLocation(...)`
- `bestLastKnownLocation(...)`
- `isUsableCurrentLocation(...)`
- `isUsableLastKnownLocation(...)`

### 아직 측정되지 않은 것
- provider별 timeout 발생률
- provider별 성공률
- fallback 비율

## Action Path

### 실제로 읽은 파일
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionWorkproofHandlers.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/workproof/BackendWorkproofRepository.kt`

### 지금 보이는 핵심
- 출근/퇴근 submit 전에 항상 위치를 다시 조회한다.
- 조회 실패 시 submit은 즉시 중단된다.
- 좌표는 submit body에 그대로 포함돼 backend 반경 검증에 사용된다.

### 다음에 보면 좋은 함수
- `submitWorkproofAction(...)`
- `refreshWorkproofCurrentLocationInternal()`
- `BackendWorkproofRepository.clockIn(...)`
- `BackendWorkproofRepository.clockOut(...)`

### 아직 측정되지 않은 것
- submit flow에서 위치조회가 차지하는 평균/최대 시간
- 최근 위치 재사용(캐시) 도입 시 정확도 영향

## UI Trigger

### 실제로 읽은 파일
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofMapSection.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`

### 지금 보이는 핵심
- WorkProof 지도 컴포저블 진입 시 자동 refresh를 호출한다.
- 사용자 버튼 탭으로도 refresh를 호출한다.
- `LOADING` 가드가 있지만 근접 호출 레이스 가능성은 남아 있다.

### 다음에 보면 좋은 함수
- `KakaoWorkplaceMapView(...)` 내부 `LaunchedEffect(Unit)`
- refresh 버튼 `onClick`
- `DemoSessionViewModel.refreshWorkproofCurrentLocation()`

### 아직 측정되지 않은 것
- 중복 fetch 실발생 빈도
- 중복 fetch가 체감 지연에 주는 비중

## Backend Validation 경계

### 실제로 읽은 파일
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/dto/request/CheckInWorkProofRequest.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/dto/request/CheckOutWorkProofRequest.java`

### 지금 보이는 핵심
- backend는 좌표를 필수 입력으로 받고 반경 검증을 수행한다.
- check-in은 반경 밖이면 차단, check-out은 review 상태로 전환한다.
- 그래서 모바일 최적화 시에도 좌표 품질 요구를 완화할 때 정책 영향 점검이 필요하다.

## 갱신 규칙
- 새로 읽은 파일만 `실제로 읽은 파일`에 추가한다.
- 코드만 보고 의심되는 내용은 `지금 보이는 핵심`에 둔다.
- 실제 수치가 나오면 `02-baseline-and-measurement-plan.md` 와 `03-current-findings.md` 를 먼저 갱신한다.

