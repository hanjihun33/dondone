# Employer Web Direction

## 목적
- 고용주 웹 콘솔의 제품 방향과 기술 경계를 먼저 고정한다.
- 기존 앱 API를 흔들지 않고 웹 전용 API를 추가하는 이유를 문서로 명확히 남긴다.
- 구현 도중 범위가 앱 전면 리팩터링으로 번지는 것을 막는다.

## 현재 관찰 근거
### 웹 화면 근거
- `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx`
- `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx`
- `apps/dondone-web/src/pages/issues/IssuesQueuePage.tsx`
- `apps/dondone-web/src/pages/settings/SettingsPage.tsx`

### 백엔드 근거
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/**`

### 현재 구조에서 읽히는 점
- 백엔드는 근로자 앱 중심 축이 강하다.
- 웹은 사업주 관점 조회, 승인, 운영 console read-model이 필요하다.
- 현재 `User`와 `Workplace`, `WorkProof` 구조만으로는 고용주-회사-근로자 관점이 충분히 드러나지 않는다.

## 이번 단계의 명시적 결정
- 웹은 `고용주 콘솔`로 본다.
- 앱은 `근로자 중심 기능`으로 본다.
- 기존 앱 API contract는 당장 바꾸지 않는다.
- 웹은 별도 namespace의 전용 API로 붙인다.
- 공통 인증 인프라는 재사용하되, 가입/로그인 흐름과 권한 모델은 역할별로 분리한다.
- 웹 구현을 위해 필요한 공유 엔티티 변경은 허용하되, 앱 API 변경은 기본적으로 금지한다.

## 이번 단계에서 하지 않는 것
- 앱 API 전면 리팩터링
- 공통 계정 모델 최종 통합 설계 확정
- 근로자 앱과 고용주 웹의 DTO 통합
- 세부 DTO/DB 컬럼 최종 확정

## 웹 전용으로 먼저 가는 이유
- 현재 백엔드는 근로자 앱 중심 축이 강하다.
- 웹은 사업주 관점 조회/승인/read-model이 핵심이라 성격이 다르다.
- 지금 앱 API까지 같이 바꾸면 인증, DTO, 권한, 테스트 범위가 한 번에 넓어진다.
- 따라서 현재 단계에서는 `기존 앱 영향 최소화`가 가장 현실적인 전략이다.

## 권장 경계
### 웹 전용으로 둬야 하는 것
- endpoint 경계
- request/response DTO
- controller/service read-model
- 가입/로그인 후처리
- role-based access rule

### 공통으로 재사용할 수 있는 것
- JWT 발급/검증 인프라
- 공통 에러 envelope
- 공통 persistence/util
- 기존 근로기록 계산 로직 중 재사용 가능한 도메인 계산기

### 복제하면 안 되는 것
- 기존 앱 service 로직 전체 복붙
- 같은 의미의 DTO를 역할만 바꿔 중복 생성
- UI 분기만 다른데 실제 권한 체크는 없는 구조

## 설계 원칙
- endpoint 경계, DTO, service를 웹 전용으로 둔다.
- 기존 도메인 로직을 무분별하게 복제하지 않는다.
- 공유 엔티티를 건드는 경우 먼저 소유권과 조회 권한을 검증한다.
- 장기적으로는 전체 리팩터링 가능하도록 naming과 경계를 일관되게 유지한다.
- read-model과 command-model을 구분해서 설계한다.

## 리스크와 대응
### 리스크 1. `User` 한 객체에 의미가 계속 섞임
- 대응: `auth-and-role-policy.md` 기준으로 공통 auth와 역할별 profile/API를 분리한다.

### 리스크 2. 회사-사업장-근로자 관계가 불명확한 상태로 API가 먼저 고정됨
- 대응: `employer-worker-domain-map.md`를 먼저 고정하고 API를 설계한다.

### 리스크 3. 정정 요청 승인 결과가 WorkProof/Wage 계산과 불일치
- 대응: `correction-request-flow.md`와 `shared-entity-validation.md`를 같이 갱신한다.

### 리스크 4. settings 변경이 기존 기록 판정에 미치는 영향이 누락됨
- 대응: `workplace-settings-contract.md`에서 효력 시점과 재계산 규칙을 먼저 정한다.

### 리스크 5. 웹 구현이 결국 기존 앱 API 변경을 강제함
- 대응: 이 경우는 구현을 밀어붙이지 말고 범위를 다시 잡는다. 현재 단계의 기본 전제는 `기존 앱 API contract 유지`다.

## 문서 갱신 트리거
- 새 employer endpoint를 추가할 때
- shared entity를 추가/수정할 때
- role 정책이 바뀔 때
- correction request 또는 workplace settings 상태 전이 규칙이 바뀔 때
- 웹 요구사항이 기존 앱 API 변경을 요구하기 시작할 때

## 성공 기준
- 웹 구현 중 문서만 보고도 어느 API를 새로 만들지 판단할 수 있다.
- 공유 도메인 변경이 앱에 미치는 영향을 중간에 놓치지 않는다.
- 앱 API를 변경하지 않고도 웹 MVP가 진행 가능하다.
