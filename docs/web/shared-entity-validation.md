# Shared Entity Validation

## 목적
- 웹을 만들면서 앱에도 영향을 줄 수 있는 엔티티/DB 변경을 중간에 검증한다.
- 구현 중 가장 많이 사고나는 공유 도메인 변경을 체크리스트로 관리한다.

## 반드시 먼저 확인할 것
- 이 변경이 기존 앱 API contract를 깨는가
- 이 변경이 기존 WorkProof/Wage 계산에 영향을 주는가
- 이 변경이 조회 권한 범위를 넓히는가
- 이 변경이 기존 데이터 마이그레이션을 요구하는가

## 검증이 꼭 필요한 변경 유형
- 새 profile/entity 추가
- 기존 `User`, `Workplace`, `WorkProof` 관계 변경
- role enum 또는 auth filter 변경
- correction request 상태 전이 규칙 추가
- workplace settings가 출퇴근 판정 규칙에 영향 주는 변경

## 검증 대상 엔티티
- User or Account
- Employer Profile
- Worker Profile
- Company
- Workplace
- Membership or Employment
- WorkProof
- Correction Request
- Audit Log

## 체크리스트
### 소유권
- 엔티티 owner가 누구인지 명확한가
- 사업주와 근로자의 접근 경계가 분리되어 있는가

### 관계
- 회사-사업장-근로자 연결 키가 일관적인가
- 여러 사업장/여러 고용주 케이스에서 모순이 없는가

### 권한
- 조회 가능 범위가 DB 질의 수준에서 제한되는가
- 승인/반려 같은 쓰기 작업이 role 기반으로 막히는가
- bare ID를 받는 employer endpoint가 `EmploymentMembership` 또는 그에 준하는 조직 연결 기준으로 target 재검증을 수행하는가

### 상태 전이
- 정정 요청 처리 후 WorkProof 상태가 어떻게 바뀌는지 명확한가
- 반경/위치 설정 변경 후 기존 기록 처리 규칙이 정의되어 있는가

### 계약 영향
- 앱 DTO를 같이 바꾸지 않아도 되는가
- 웹 전용 DTO로 격리 가능한가

### 운영 영향
- 마이그레이션이 필요한가
- 시드 데이터나 테스트 픽스처를 바꿔야 하는가

## MVP 기준 마이그레이션 메모
- `EmployerProfile`, `WorkerProfile`, `Company`, `EmploymentMembership`, `CorrectionRequest`, `CorrectionDecisionAudit`를 추가하면 신규 테이블 마이그레이션이 필요할 가능성이 높다.
- 기존 `Workplace.user`, `WorkProof.user`는 당장 제거하지 않고 공존시키는 방향이 안전하다.
- 즉, MVP 단계에서는 파괴적 스키마 변경보다 `추가 테이블 + 보조 연결` 접근을 우선 검토한다.
- 기존 seed account와 테스트 픽스처는 worker 기준으로 유지하고, employer용 seed는 별도 추가하는 편이 안전하다.
- 단, employer web authorization source of truth는 `EmployerProfile + EmploymentMembership`으로 고정하고, 레거시 `Workplace.user`, `WorkProof.user`는 권한 판정 기준으로 사용하지 않는다.

## 엔티티별 확인 포인트
### User or Account
- 공통 auth용인지 역할 정보까지 포함하는지
- worker/employer 겸용을 허용할지

### Employer or Worker Profile
- 화면 표시용 필드와 권한용 필드를 분리할지
- soft delete 또는 비활성 처리 기준이 필요한지

### Company and Workplace
- 어떤 엔티티가 권한 기준 축인지
- workplace settings가 어느 엔티티에 저장되는지

### EmploymentMembership
- 근로자와 회사/사업장을 연결하는 canonical source인지
- 대시보드, workers list, correction queue 조회 범위를 이 모델로 제한 가능한지

### CorrectionRequest
- 원본 WorkProof snapshot을 저장할지
- 처리 후 재처리 방지 규칙이 있는지

### AuditLog
- 누가/언제/무엇을 바꿨는지 재현 가능한지

## 중간 검증 게이트
- Gate 1: 엔티티 추가 전
- Gate 2: API 계약 고정 전
- Gate 3: 구현 후 테스트 추가 전
- Gate 4: 리뷰 전

## 게이트별 해야 할 일
### Gate 1. 엔티티 추가 전
- `employer-worker-domain-map.md` 갱신
- owner, relation, scope를 서술
- 기존 app API 영향 여부를 문장으로 남김

### Gate 2. API 계약 고정 전
- `employer-web-api-map.md`와 대조
- company/workplace scope가 request에 필요한지 결정
- read-model/command 구분 확인

### Gate 3. 구현 후 테스트 추가 전
- 변경 엔티티가 WorkProof/Wage 계산에 영향 주는지 다시 확인
- 데이터 마이그레이션 또는 seed 보강 필요 여부 정리
- 기존 integration test가 깨질 가능성이 있는지 먼저 확인

### Gate 4. 리뷰 전
- 권한 누락, 범위 누락, 상태 전이 누락 항목을 재점검
- 결과를 `docs/reviews/active/`에 남길지 판단

## 즉시 중단하고 재검토해야 하는 신호
- worker가 employer scope 데이터를 직접 참조하기 시작함
- employer endpoint가 기존 worker DTO를 그대로 재사용하려 함
- company/workplace scope 없이 worker 목록을 전역 조회하려 함
- correction request 승인 후 WorkProof 반영 규칙이 문서에 없음
- 웹 요구사항을 맞추기 위해 기존 앱 API contract 변경이 필수처럼 보이기 시작함
- 신규 엔티티를 넣으려는데 기존 `Workplace.user`, `WorkProof.user`를 동시에 제거해야만 성립하는 구조가 나옴

## 결과 기록 위치
- 실제 검증 결과와 리스크는 `docs/reviews/active/`에 기록한다.

## Follow-up 메모
- 반경 밖 `check-out` 예외를 employer web review queue와 어떻게 연결할지는 shared domain follow-up 대상이다.
- 현재 단계에서는 `웹에서 review 대상이 될 수 있다`는 정책 방향만 유지하고, worker-side 사유 입력 방식과 check-out request 계약 변경은 추후 app/web 공통 리팩토링 범위에서 정리한다.
