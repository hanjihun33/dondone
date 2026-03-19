# Employer Worker Domain Map

## 목적
- 웹에서 다루는 고용주-회사-사업장-근로자 관계를 먼저 고정한다.
- API보다 먼저 `누가 누구를 볼 수 있고, 무엇을 변경할 수 있는지`를 정리한다.

## 현재 코드 기준 관찰
- 현재 `User`는 계정 공통 필드 중심이다.
- 현재 `Workplace`는 개인 사용자 소유 구조에 가깝다.
- 현재 `WorkProof`는 근로자 개인 기록 중심이다.
- 즉, 고용주 관점의 `회사 -> 사업장 -> 근로자` 모델이 명시적으로 드러나지 않는다.

## 현재 코드 근거
### Account
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`
  - `email`, `passwordHash`, `name`, `role`만 존재
  - worker/employer 분리 profile 필드가 없음

### Workplace
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/Workplace.java`
  - `Workplace.user`로 개인 사용자 소유
  - company 또는 employer 조직 축이 없음

### WorkProof
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
  - `WorkProof.user`로 개인 사용자 귀속
  - workplace와 contract는 보조 연결이지만 소속 canonical source로 쓰기에는 부족

### WorkContract
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkContract.java`
  - `WorkContract.workplace` 기준으로 연결
  - 근로자 개별 소속이나 employer 조직 구조를 직접 표현하지 않음

## 핵심 질문
- 고용주는 어떤 단위로 근로자를 본다
- 회사와 사업장은 어떤 관계인가
- 근로자는 어느 사업장 또는 회사에 소속되는가
- 정정 요청은 누구의 기록을 누구에게 올리는가

## 기본 엔티티 후보
- Employer Account
- Employer Profile
- Company
- Workplace
- Worker Account
- Worker Profile
- Employment or Membership
- Correction Request
- WorkProof

## 최소 관계
- Employer Account -> Employer Profile
- Employer Profile -> Company
- Company -> Workplace
- Worker Account -> Worker Profile
- Worker Profile -> Employment or Membership
- Employment or Membership -> Company and Workplace
- Correction Request -> Worker + WorkProof + Employer approver

## 이번 단계 권장 모델
### 공통
- `Account` 또는 기존 `User`는 공통 인증 주체로 유지

### 역할별
- `EmployerProfile`
- `WorkerProfile`

### 조직
- `Company`
- `Workplace`
- `EmploymentMembership`

### 업무 흐름
- `WorkProof`
- `CorrectionRequest`
- `CorrectionDecisionAudit`

## 권장 엔티티 초안
### Account or User
- 목적: 공통 인증 주체
- 최소 필드
  - `id`
  - `email`
  - `passwordHash`
  - `name`
  - `role` or `authorities`

### EmployerProfile
- 목적: employer 전용 프로필과 조직 연결
- 최소 필드
  - `accountId`
  - `companyId`
  - `displayName`
  - `status`

### WorkerProfile
- 목적: worker 목록과 운영 콘솔 표시용 프로필
- 최소 필드
  - `accountId`
  - `employeeCode`
  - `team`
  - `phone`
  - `avatarUrl`
  - `status`

### Company
- 목적: employer 권한의 최상위 조직 축
- 최소 필드
  - `id`
  - `name`
  - `companyCode`
  - `status`

### Workplace
- 목적: 출퇴근 설정과 근무 계약의 물리적 장소 축
- 최소 필드
  - `id`
  - `companyId`
  - `name`
  - `address`
  - `detailAddress`
  - `latitude`
  - `longitude`
  - `allowedRadiusMeters`

### EmploymentMembership
- 목적: worker를 company/workplace에 연결하는 canonical source
- 최소 필드
  - `id`
  - `workerProfileId`
  - `companyId`
  - `workplaceId`
  - `employmentStatus`
  - `effectiveFrom`
  - `effectiveTo`

### CorrectionRequest
- 목적: worker가 제출한 정정 요청과 employer 처리 상태 관리
- 최소 필드
  - `id`
  - `workerProfileId`
  - `workProofId`
  - `status`
  - `originalSnapshot`
  - `requestedSnapshot`
  - `reason`
  - `requestedAt`
  - `decidedBy`
  - `decidedAt`

### CorrectionDecisionAudit
- 목적: 승인/반려 결정의 감사 추적
- 최소 필드
  - `id`
  - `correctionRequestId`
  - `actorAccountId`
  - `action`
  - `beforeState`
  - `afterState`
  - `actedAt`

## 소유권 규칙
- 근로자는 자신의 기록만 생성/수정 요청 가능
- 고용주는 자신이 관리하는 회사/사업장 소속 근로자만 조회 가능
- 고용주는 자신이 관리 범위 안의 정정 요청만 승인/반려 가능

## 접근 범위 규칙
### 근로자
- 자기 `WorkProof`만 조회 가능
- 자기 `CorrectionRequest`만 생성 가능

### 고용주
- 자기 `Company` 또는 위임받은 `Workplace` 범위의 `WorkerProfile`만 조회 가능
- 범위 안의 `CorrectionRequest`만 처리 가능
- `Workplace settings`는 범위 안의 사업장만 수정 가능

### 관리자
- 운영 목적의 전역 접근만 허용
- employer web 기준 요구사항에는 우선 포함하지 않음

## 확인이 필요한 애매한 지점
- 한 고용주가 여러 사업장을 동시에 관리하는지
- 한 근로자가 여러 사업장을 오갈 수 있는지
- 사업장 설정은 회사 공통인지 사업장별인지
- 근로자/고용주 겸용 계정이 필요한지

## 현재 백엔드와의 충돌 포인트
- 현재 데이터 소유가 개인 사용자 중심인지
- 사업주 관점 집계 read-model이 없는지
- 근로자 목록 화면에 필요한 프로필 필드가 현재 모델에 없는지

## 문서화할 결정 포인트
- worker/employer 계정 겸용 허용 여부
- company와 workplace를 둘 다 둘지, workplace만 둘지
- employment 소속의 기준 키를 company로 볼지 workplace로 볼지
- correction request가 snapshot을 갖는지 원본 참조만 갖는지

## 지금 단계의 권장 결론
- 공통 auth 주체는 유지하되 역할별 profile을 분리한다.
- 조직 축은 `Company -> Workplace -> EmploymentMembership`으로 두는 편이 확장성에 유리하다.
- employer web의 모든 조회 권한은 `EmploymentMembership` 또는 그에 준하는 연결 모델을 기준으로 제한한다.
- employer web의 authorization source of truth는 `EmployerProfile.companyId`와 `EmploymentMembership.companyId/workplaceId`다.
- 기존 `Workplace.user`, `WorkProof.user`는 worker legacy ownership과 하위 호환을 위한 값으로만 보고, employer web 권한 판정 기준으로 사용하지 않는다.

## MVP 기본 가정
- employer 계정 1개는 기본적으로 회사 1곳에 소속된다고 가정한다.
- 회사 1곳은 여러 workplace를 가질 수 있다고 가정한다.
- worker 1명은 MVP에서 활성 membership 1건만 가진다고 가정한다.
- worker의 현재 소속 workplace가 dashboard, workers list, correction queue의 범위를 결정한다고 가정한다.
- 다중 소속이나 겸직은 P0 범위 밖으로 둔다.
- employer web의 목록/집계 endpoint는 `인증 사용자 -> EmployerProfile -> defaultWorkplaceId 또는 현재 선택 workplace` 순으로 scope를 해석한다.
- MVP에서는 workplace switcher가 없으면 `defaultWorkplaceId`를 서버가 자동 적용하고, 클라이언트가 임의 `companyId`를 넘겨 scope를 넓히지 못하게 한다.

## 이 가정을 둔 이유
- employer web의 핵심은 운영 콘솔 가시성과 승인 흐름이다.
- 다중 소속, 다중 회사, 겸직까지 같이 열면 권한과 조회 범위가 급격히 복잡해진다.
- 현재 backend도 그 복잡도를 지탱할 조직 모델이 없다.

## 왜 이 결론이 필요한가
- 현재 `Workplace.user`, `WorkProof.user` 구조만으로는 "고용주가 여러 근로자를 본다"는 웹 요구사항을 안정적으로 표현하기 어렵다.
- 즉, 웹 전용 API를 따로 만들더라도 소속 관계를 표현하는 별도 모델 없이 가면 권한과 조회 범위가 쉽게 꼬인다.

## 문서 갱신 시점
- 새 엔티티를 추가하려 할 때
- 권한 규칙이 바뀔 때
- 웹 API 설계가 조회 범위를 넓히려 할 때
- 기존 app API가 공유 엔티티 변경 영향을 받기 시작할 때
