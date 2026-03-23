# DonDone P0 기능 명세 초안 v0

## 문서 정보
- 작성일: 2026-03-10
- 기준 문서:
  - `docs/DonDone_PRD_v1.5.md`
  - `docs/DonDone_P0_API_Contract_v0.md`
- 상태: Draft v0
- 목적: PRD v1.5 기준 P0 전체를 한 문서 안에서 읽을 수 있게 정리하고, 구현 계약이 이미 비교적 안정적인 항목과 아직 최소 스케치만 있는 항목의 차이를 함께 드러낸다.

## 요구사항 확인

| 항목 | 이번 문서 기준 |
| --- | --- |
| expected behavior | API 초안을 반복해서 읽지 않아도 P0 기능의 목표, 핵심 흐름, 포함/제외 범위, 구현 우선 논점을 이해할 수 있어야 한다. |
| exact scope | `home`, `auth`, `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault`, `copilot`, `demo time travel`를 다룬다. |
| contract changes | 이번 문서는 기능 명세 초안이며 구현 코드를 바꾸지 않는다. 다만 PRD P0 전체를 맞추기 위해 `Money Home`, `Copilot`, `Demo Time Travel`의 최소 기능 스케치를 포함한다. |
| security impact | 기존 JWT 보호 원칙, 타인 리소스 은닉, `testnet/demo only` 제약, facts-only Copilot 원칙, demo account 한정 Time Travel 조건을 그대로 반영한다. |
| non-functional impact | 문서 생성과 송금은 비동기 처리 전제를 유지하고, Wage는 anomaly detection 중심, Vault/Advance/Remittance는 시뮬레이션 또는 testnet 범위만 다룬다. Demo Time Travel은 `asOf` 재현성을 우선한다. |

## 범위

### 포함
- Money Home
- Auth
- WorkProof
- Advance
- Wage Shield
- Documents
- Instant Claim
- Remittance
- SafePay
- Vault
- Copilot
- Demo Time Travel

### 제외
- P1 범위 전체
- 실거래/실정산/실제 수익 보장
- Copilot 자유 대화형 확장
- Demo Time Travel의 릴리스 빌드 노출

## 기능 상태 표기
- `P0-확정`: 현재 공유 문서 기준으로 바로 구현 기준으로 삼기 쉬운 항목
- `P0-초안`: P0에는 포함하지만 세부 계약/DTO는 아직 최소 스케치 수준인 항목
- `P0-후속 세부화`: P0 핵심 규칙이지만 판정 기준이나 노출 방식은 구현 직전에 더 좁혀야 하는 항목

## 공통 원칙
- 제품명은 `DonDone`를 사용한다.
- P0는 `demo/testnet only` 범위다. 실거래, 실정산, 실수익 보장을 의미하지 않는다.
- Wage 결과는 법률·재무 최종 판단이 아니라 `anomaly detection + evidence-first` 보조 기능으로 위치시킨다.
- `/api/auth/signup`, `/api/auth/login`, `/health`, Swagger 외 나머지 기능은 JWT 보호 대상이다.
- 타인 리소스 접근은 허용하지 않으며, 필요 시 `404`로 숨기는 현재 API 초안 원칙을 유지한다.
- 문서 생성과 송금은 비동기 처리 전제를 유지한다.
- Copilot은 `facts-only`, `숫자 재계산 금지`, `추정 금지`를 기본 원칙으로 둔다.
- Demo Time Travel은 demo account / demo mode 한정 기능으로 두고, 일반 사용자 흐름과 릴리스 빌드에서는 비활성화한다.
- 이 문서는 기능 설명을 우선하므로, API 상세 필드 표를 반복 복제하지 않고 `상태 / 핵심 흐름 / 주요 규칙 / API 매핑` 중심으로 정리한다.

## 도메인 간 연결
- Money Home은 Advance, Wage Shield, Remittance, Vault의 상태를 한 화면에서 재조합한다.
- WorkProof는 Advance와 Wage Shield의 근거 데이터이며, W7 Integrity는 이 연결의 신뢰 기준이다.
- Wage Shield의 급여 확인 결과(계약상 verification)는 Documents와 Instant Claim의 입력이 된다.
- Remittance는 SafePay 사전 점검과 Documents 영수증 생성 흐름을 함께 가진다.
- Vault는 Remittance와 잔액 해석을 공유하지만, P0에서는 독립적인 시뮬레이션 기능으로 취급한다.
- Copilot은 독립 판단 엔진이 아니라 각 화면 facts를 다시 설명하는 보조 계층이다.
- Demo Time Travel은 Home, WorkProof, Advance, Wage, Remittance, Vault 같은 읽기 흐름을 `asOf` 기준으로 다시 렌더하는 가로축 기능이다.

## 1. Money Home

### 상태
- `P0-초안`

### 목표
- 앱 첫 화면이 `근무 기록 앱`보다 `이번 달 내 돈 상태를 보여주는 핀테크 홈`으로 읽히게 한다.

### 범위
- 메인 hero 카드
- 상태 기반 다음 행동 CTA 1개
- 빠른 금융 액션 묶음
- 오늘 근무 보조 카드
- loading / empty / error / success 상태 분리

### 범위 제외
- 개인화 추천 고도화
- 푸시/리마인드 엔진
- 카드 단위 독립 API 분리 확정
- P1용 장기 분석/리포트 홈

### 핵심 흐름
1. 사용자가 홈에 진입하면 이번 달 기준 돈 상태를 한 번에 조회한다.
2. 시스템은 예상 급여, 실제 입금 상태, 미리받기 가능 금액, 지금 보낼 수 있는 금액을 hero 카드에 조합한다.
3. 시스템은 현재 상태에 맞는 `다음 행동 CTA` 1개를 우선 노출한다.
4. 사용자는 빠른 금융 액션을 통해 Advance, Wage, Remittance, Vault로 이동한다.
5. 오늘 근무 카드는 보조 정보로 남기되, 근무 기록 그 자체보다 금융 연결 메시지를 강조한다.

### 주요 규칙
- 홈의 첫 인상은 `금융 중심`이어야 한다.
- 금액이 비어 있거나 잠겨 있어도 `왜 없는지`와 `다음 행동`을 함께 설명해야 한다.
- 오늘 근무 카드는 핵심 카드가 아니라 보조 카드다.
- 홈 값은 독립 정책 계산이 아니라 다른 도메인 결과를 재조합한 표현이다.
- Demo Time Travel이 활성화되면 홈도 `asOf` 기준 상태를 반영해야 한다.

### API 매핑
- `GET /api/home/summary`

### v0 가정
- 현재 월 기준 단일 조합 응답을 우선 사용한다.
- 추천 행동은 개인화 모델이 아니라 현재 도메인 상태 규칙으로 정한다.
- 빠른 액션과 today work 카드는 하나의 summary 응답 안에 함께 포함한다.

### 열린 질문
- Home 조합 응답을 이후 카드 단위로 분리할지 여부

## 2. Auth

### 상태
- `P0-확정`

### 목표
- 기존 backend auth baseline을 그대로 사용해 P0 보호 기능 진입에 필요한 최소 인증 컨텍스트를 제공한다.

### 범위
- 회원가입
- 로그인과 JWT 발급
- 현재 사용자 조회

### 범위 제외
- 소셜 로그인
- 비밀번호 재설정
- 프로필 수정
- refresh token 재설계

### 핵심 흐름
1. 사용자가 회원가입을 완료한다.
2. 로그인 시 access token을 발급받는다.
3. 보호 화면 진입 시 `me` 조회로 현재 사용자 컨텍스트를 복구한다.

### 주요 규칙
- 현재 backend 구현을 기준으로 문서화한다.
- 공개 경로는 `signup`, `login`, `/health`, Swagger만 유지한다.
- 응답 envelope은 기존 `data` 기반 형식을 유지한다.
- 이 문서에서 auth 계약은 새로 확장하지 않는다.

### API 매핑
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/auth/me`

### v0 가정
- access token 기반 단일 세션 흐름을 우선 사용한다.
- 현재 auth baseline과 API 계약을 그대로 유지한다.

### 열린 질문
- 없음

## 3. WorkProof

### 상태
- `P0-확정`

### 목표
- Wage Shield, Advance, Documents로 이어질 수 있는 신뢰 가능한 근무 증거를 남긴다.

### 범위
- 근무지 등록
- 급여 계약 등록과 현재 활성 계약 조회
- 수정 증빙용 첨부 업로드
- 출근 기록 생성
- 퇴근 기록 확정
- 누락 기록 provisional 생성
- 기존 기록 수정
- 월별 목록 조회
- 기록 상세 조회
- 월간 요약 조회
- WorkProof Integrity 판정에 필요한 상태 구분과 근거 해석

### 범위 제외
- 지오펜스 자동 알림
- 동시 다중 근무지 활성 계약 운영
- 동일 날짜 다중 근무 허용
- 복잡한 부정 탐지 모델
- 문서 자동 반영 세부 규칙의 완전한 고정
- 누락 기록/수정 기록의 승인 워크플로 상세화

### 핵심 흐름
1. 사용자가 근무지와 급여 계약을 등록한다.
2. 출근 시 `workplaceId`와 GPS 좌표를 보내면 서버가 활성 계약을 선택해 기록을 만든다.
3. 퇴근 시 서버가 활성 `CHECKED_IN` 기록 1건을 찾아 같은 기록을 마감한다.
4. 누락되었거나 잘못된 기록이 있으면 provisional 누락 생성 또는 기존 기록 수정으로 보완한다.
5. 사용자는 월별 목록과 상세를 확인하고, 월간 요약은 `기록된 근무 / 반영 근무 / 검증 완료 근무 / 선지급 가능 근무` 구분까지 포함해 Advance/Wage/Home 입력으로 재사용된다.

### 주요 규칙
- contract는 user 단독이 아니라 workplace 종속 모델로 관리한다.
- workproof record에는 `contract_id`를 저장해 과거 계약 기준을 추적한다.
- 출근/퇴근 시 device time과 server time을 함께 저장한다.
- `workDate`는 `checkIn.deviceAt` 로컬 날짜 기준으로 해석한다.
- 출근/퇴근 API는 GPS 좌표를 필수로 받고 `locationLabel`만 선택으로 둔다.
- 1주차 기준으로 동일 날짜 다중 근무는 허용하지 않는다.
- 출근 API는 `workplaceId`만 받고 서버가 활성 계약을 해석한다.
- 퇴근 API는 `recordId`를 받지 않고 서버가 활성 `CHECKED_IN` 기록을 찾는다.
- 본인 기록만 조회/수정할 수 있다.
- 누락 기록은 기존 modification API와 분리된 provisional 생성 API로 다룬다.

### W7. WorkProof Integrity 판정
- 상태: `P0-후속 세부화`
- WorkProof는 단순 기록 저장이 아니라 `이 근무를 금융 판단에 써도 되는가`를 가르는 근거다.
- P0에서는 아래 상태를 구분한다.
  - 기록된 근무
  - 반영 근무
  - 검증 완료 근무
  - 선지급 가능 근무
- 판정 근거 예시는 아래를 포함한다.
  - 출근/퇴근 위치 스냅샷
  - 기기 시간 / 서버 수신 시간
  - 수정 횟수 / 수정 사유 / 첨부 유무
  - 기기 일관성 및 demo용 위험 플래그
- Home, Advance, Wage는 W7 결과를 직접 점수화된 모델보다 `설명 가능한 규칙 + 감사 로그` 기준으로 사용한다.
- `verified_hours`, `pending_hours`, `workproofRiskFlags` 같은 결과값은 WorkProof 내부 계산 결과로 유지하되, 별도 독립 기능처럼 보이기보다 WorkProof 하위 규칙으로 다룬다.

### API 매핑
- `POST /api/workproof/workplaces`
- `GET /api/workproof/workplaces`
- `POST /api/workproof/contracts`
- `GET /api/workproof/contracts/current?workplaceId={id}`
- `POST /api/workproof/attachments`
- `POST /api/workproof/records/check-in`
- `POST /api/workproof/records/check-out`
- `POST /api/workproof/records/missing`
- `POST /api/workproof/records/{recordId}/modifications`
- `GET /api/workproof/records?month=YYYY-MM&workplaceId={id}`
- `GET /api/workproof/records/{recordId}`
- `GET /api/workproof/monthly-summary?month=YYYY-MM&workplaceId={id}`

### v0 가정
- `DAILY` 계약은 `dailyWorkMinutes` 미지정 시 `480`을 사용한다.
- `MONTHLY` 계약은 `monthlyWorkMinutes` 미지정 시 시스템 기본값을 사용한다.
- 월간 요약은 snapshot 저장 없이 조회 시 계산한다.
- 첨부는 `attachmentId` 참조 방식으로 사용한다.
- W7 결과는 별도 독립 API보다 record detail / monthly summary에 내장하는 방향을 우선 사용한다.
- 누락 기록 필드와 승인 정책은 구현 중 조정 가능하다.

### 열린 질문
- W7 결과를 월간 요약 안에만 둘지, 별도 세부 조회가 필요한지 여부
- 누락 기록과 수정 기록의 승인 정책을 어느 수준까지 v0에서 고정할지 여부
- 자정 경계가 걸리는 근무를 월 집계와 W7 노출에 어떻게 드러낼지 여부

## 4. Advance

### 상태
- `P0-확정`

### 목표
- 검증 가능한 근무 반영 데이터를 바탕으로 `얼마까지 왜 가능한지`를 먼저 설명하고, 그 다음에 데모 신청을 받는다.

### 범위
- 적격성/가능 금액 조회
- Advance 신청 생성
- 월 단위 신청 이력 조회
- 신청 상세 조회

### 범위 제외
- 실대출 집행
- 실회수/실상환 처리
- 외부 파트너 연동
- P1 상용화 정책

### 핵심 흐름
1. 사용자가 근무지 기준으로 eligibility를 조회한다.
2. 시스템이 가능 금액, 상한, 사유, 차단 사유를 설명한다.
3. 사용자가 `Idempotency-Key`와 함께 신청을 생성한다.
4. 시스템은 데모 시뮬레이션 결과를 반환하고, 사용자는 월별 이력과 상세를 다시 볼 수 있다.

### 주요 규칙
- Advance는 P0에서 `demo simulation`이다.
- 설명 없는 신청보다 `근거 설명 -> 신청` 순서를 우선한다.
- 신청 계열 API는 중복 호출 위험이 커서 `Idempotency-Key`를 필수로 둔다.
- 적격성은 workplace 컨텍스트와 근무 반영 데이터를 기준으로 판단한다.
- Repayment Confidence와 Hard Stop Rules는 사용자에게 결과와 이유를 설명하는 수준으로 노출한다.
- WorkProof W7 결과 중 `verified_hours`, `pending_hours`, 위험 플래그가 Advance 근거 설명의 핵심 입력이다.

### API 매핑
- `GET /api/advance/eligibility?workplaceId={id}`
- `POST /api/advance/requests`
- `GET /api/advance/requests?month=YYYY-MM`
- `GET /api/advance/requests/{requestId}`

### v0 가정
- `maxCap` 기본값은 `500,000 KRW`를 사용한다.
- 실제 대출 집행이 아니라 승인/거절 시뮬레이션 결과만 돌려준다.
- 적격성 정책의 세부 수식은 내부 정책 결과로 취급하고 별도 정책 문서화는 후속으로 미룬다.

### 열린 질문
- eligibility 화면에서 `verified_hours`, `pending_hours`, `block_reason_codes`를 어느 정도까지 직접 노출할지 여부

## 5. Wage Shield

### 상태
- `P0-확정`

### 목표
- 월간 반영 근무와 실제 받은 돈 확인을 통해 `정답 계산`이 아니라 `확인 필요 상태와 근거`를 보여준다.

### 범위
- 월간 근무 요약 조회
- 참고용 예상 급여 조회
- 실제 지급 결과 확인 기반 verification 생성
- verification 상세 조회
- Documents/Claim로 이어질 수 있는 다음 액션 상태 제공

### 범위 제외
- 급여명세서 자동 파싱
- 법률·재무 최종 판단
- 기관 자동 제출
- P1 고도화 규칙

### 핵심 흐름
1. 사용자가 `month + workplaceId` 기준으로 월간 요약과 예상 급여를 조회한다.
2. 사용자가 급여일 이후 실제 받은 돈을 확인해 verification을 생성한다.
3. 시스템이 참고용 예상 금액 대비 `확인 필요 상태`와 근거를 계산한다.
4. 사용자는 근거 항목, 포함 기록, 수정 기록 개수, 다음 액션 가능 여부를 확인하고, 필요 시 회사와 1차 확인으로 이어간다.

### 주요 규칙
- Wage 조회는 `month`와 `workplaceId`를 함께 받아야 한다.
- Wage 엔진은 WorkProof 월간 요약 결과를 입력으로 사용한다.
- 급여 계산은 시급 기준으로 정규화해 수행한다.
- 계산 단위는 분 기준이며, 기본급/연장/야간 항목별로 floor 처리한다.
- `CHECKED_IN` 상태 기록은 집계에서 제외하고 pending count로만 관리한다.
- 연장은 일별 `480분` 초과분, 야간은 `22:00~06:00` 겹침 분으로 계산한다.
- 휴일 가산은 P0 범위에서 제외한다.
- 확인 필요 상태 기본 임계값은 `30,000원 또는 2%`, 공제 미반영 추정은 `50,000원 또는 3%`를 사용한다.
- 결과는 `최종 판정`이 아니라 `확인 필요 상태 + 근거`다.
- 고용주는 연결된 경우에만, 근로자 확인 이후 설명/정정이 필요한 단계에서 보조 참여자로 등장한다.

### API 매핑
- `GET /api/wage/monthly-summary?month=YYYY-MM&workplaceId={id}`
- `GET /api/wage/estimate?month=YYYY-MM&workplaceId={id}`
- `POST /api/wage/verifications`
- `GET /api/wage/verifications/{verificationId}`

### v0 가정
- 월간 집계 결과는 조회 시 계산하고 별도 snapshot 저장은 하지 않는다.
- 휴일/법정 예외/더 정교한 근로 규칙은 후속 범위로 남긴다.
- payslip parsing 없이 사용자가 실제 받은 돈을 직접 확인해 입력한다.

### 열린 질문
- 자정 경계 근무를 Wage 계산 근거에 어떻게 표시할지 여부
- 수정 기록을 verification 상세에서 개수 중심으로 볼지, 개별 근거 목록으로 더 강조할지 여부
- 연결된 회사가 있을 때 `확인 요청` 상태를 Wage 상세 응답에 어디까지 포함할지 여부

## 6. Documents

### 상태
- `P0-확정`

### 목표
- WorkProof, Wage, Remittance 결과를 제출·공유 가능한 문서로 재조합해 사용자가 근거를 다시 정리하지 않게 한다.

### 범위
- 문서 목록 조회
- 문서 상세 조회
- 다운로드 URL 발급
- Proof Pack 생성 요청
- Claim Kit 생성 요청
- Transfer Receipt 생성 요청

### 범위 제외
- 기관 제출용 최종 템플릿 다변화
- 문서 편집기
- 자동 제출 기능
- P1용 고급 템플릿 확장

### 핵심 흐름
1. 사용자가 Wage 확인 결과(verification), Claim, Remittance 문맥에서 문서 생성을 요청한다.
2. 시스템은 비동기 요청으로 접수하고 `requestId`, `status`, `pollUrl`을 반환한다.
3. 생성된 문서는 Documents 목록에서 다시 조회할 수 있다.
4. 상세 화면에서 요약과 관련 링크를 보고, 별도 다운로드 URL을 발급받는다.

### 주요 규칙
- 문서 생성은 비동기 처리(`202 Accepted`)를 전제로 한다.
- 문서 생성 계열 API는 `Idempotency-Key`를 필수로 둔다.
- Proof Pack의 request anchor는 `wageVerificationId` 하나로 두고, `month`/`workplaceId`는 verification snapshot에서 파생한다.
- Proof Pack은 월간 요약, WorkProof 상세표, 급여 추정 근거, 수정 이력, 첨부 목록을 기본 포함 대상으로 본다.
- Proof Pack의 급여 설명은 verification snapshot을 재사용하고, WorkProof 상세 행은 verification에 저장된 `recordIds`로 보조 조회한다.
- Claim Kit는 Proof Pack + 제출용 요약 + 체크리스트를 묶고, 첨부가 많으면 `ZIP`을 허용한다.
- Transfer Receipt는 tx hash와 송금 상태 요약을 포함한다.
- 실제 파일 접근은 만료 가능한 download URL로 분리한다.

### API 매핑
- `GET /api/documents`
- `GET /api/documents/{documentId}`
- `GET /api/documents/{documentId}/download-url`
- `POST /api/documents/proof-packs`
- `POST /api/documents/claim-kits`
- `POST /api/documents/transfer-receipts`

### v0 가정
- 문서 타입은 `PROOF_PACK`, `CLAIM_KIT`, `TRANSFER_RECEIPT`를 우선 사용한다.
- 문서 상태는 `QUEUED`, `RUNNING`, `READY`, `FAILED` 수준으로 관리한다.
- 템플릿은 v1 수준의 초안으로 두고 시각 스타일 고도화는 후속 범위로 남긴다.
- legacy `POST /api/wage/deposits`, `GET /api/wage/summary`는 기존 화면 호환용으로 남기되, Documents 입력으로 직접 재사용하지 않는다.

### 열린 질문
- 제출용 문서 톤을 `공식 제출용`에 더 맞출지, `읽기 쉬운 설명형`에 더 맞출지 여부
- `summary`, `relatedLinks[]`를 기능 명세 수준에서 어디까지 고정할지 여부

## 7. Instant Claim

### 상태
- `P0-확정`

### 목표
- 자동 제출이 아니라 `준비를 반자동으로 줄여주는` 방식으로 신고/상담 진입 장벽을 낮춘다.

### 범위
- locale 기준 route 안내 조회
- claim preparation 생성
- claim preparation 상세 조회

### 범위 제외
- 기관 자동 제출
- 법률 자문
- Copilot식 자유 대화형 지원

### 핵심 흐름
1. 사용자가 locale 기준 신고/상담 route를 조회한다.
2. 사용자가 Wage 확인 결과와 선택적 claim kit를 기반으로 preparation을 생성한다.
3. 시스템은 summary text, checklist, suggested routes, related documents를 반환한다.
4. 사용자는 복사, 공유, 바로가기 같은 반자동 지원만 사용하고 직접 제출한다.

### 주요 규칙
- Instant Claim v0는 `반자동 지원`이다.
- route 안내는 locale에 따라 달라질 수 있다.
- claim kit 없이도 preparation을 만들 수 있다.
- `Idempotency-Key`는 v0에서 권장 수준으로 둔다.
- Documents와 Claim은 Wage 확인 결과(verification)를 재사용한다.
- Claim preparation은 verification snapshot을 1차 facts source로 사용하고, 선택적 `claimKitDocumentId`는 연결 문서 보강 용도로만 사용한다.
- 현재 `WageVerification`이 가진 contract/pay snapshot이면 P0 summary/checklist를 시작하기에 충분하다고 본다.

### API 매핑
- `GET /api/claim/routes?locale=ko-KR`
- `POST /api/claim/preparations`
- `GET /api/claim/preparations/{preparationId}`

### v0 가정
- 요약 문구는 facts 기반 템플릿/생성 결과로 다룬다.
- 생성 직후 `READY` 상태를 반환하는 동기형 흐름을 우선 사용한다.
- 비용/캐시 전략이 바뀌기 전까지는 idempotency를 권장으로 유지한다.
- legacy wage deposit/summary와 verification은 공존하지만, Claim의 downstream 입력축은 `wageVerificationId`로 고정한다.

### 열린 질문
- claim preparation 생성 비용과 캐시 전략이 정해지면 `Idempotency-Key`를 필수로 전환할지 여부

## 8. Remittance

### 상태
- `P0-확정`

### 목표
- 허용된 수신자에게만 testnet 송금을 수행하고, 상태 추적과 영수증까지 이어지는 흐름을 제공한다.

### 범위
- 수신자 allowlist 조회
- 수신자 등록
- 송금 목록 조회
- 송금 요청
- 송금 상세 조회

### 범위 제외
- 정기 송금
- 자동 송금
- mainnet 또는 실거래 송금
- allowlist 밖 주소로 직접 송금

### 핵심 흐름
1. 사용자가 수신자 allowlist를 조회하거나 새 수신자를 등록한다.
2. 사용자가 `Idempotency-Key`와 함께 송금을 요청한다.
3. 시스템은 SafePay 점검과 비동기 송금 처리 흐름을 시작한다.
4. 사용자는 목록과 상세에서 상태, tx hash, 영수증 연결을 확인한다.

### 주요 규칙
- 송금은 allowlist 기반으로만 수행한다.
- P0 송금은 testnet 범위다.
- 송금 요청은 비동기 처리이며 `transferId`를 즉시 부여한다.
- 송금 요청 API는 `Idempotency-Key`를 필수로 둔다.
- 송금 상세에는 SafePay 결정, tx hash, 영수증 문서 연결이 포함된다.

### API 매핑
- `GET /api/remittance/recipients`
- `POST /api/remittance/recipients`
- `GET /api/remittance/transfers`
- `POST /api/remittance/transfers`
- `GET /api/remittance/transfers/{transferId}`

### v0 가정
- 송금 진행은 `SUBMITTED -> CONFIRMED / FAILED / BLOCKED` 수준으로 추적한다.
- 상세 조회와 `pollUrl`을 통해 진행 상태를 확인한다.
- 정기/자동 송금은 P1에서 별도 흐름으로 분리한다.

### 열린 질문
- 없음

## 9. SafePay

### 상태
- `P0-확정`

### 목표
- 송금을 막는 것에서 끝내지 않고, `왜 막혔는지`를 사용자 언어로 먼저 설명하는 보호 계층이 된다.

### 범위
- 송금 전 transfer check
- decision, reason codes, cooldown, 추가 확인 여부 반환

### 범위 제외
- 별도 리스크 플랫폼
- 실시간 외부 사기 탐지 연동
- 도메인 밖 범용 점수화 시스템

### 핵심 흐름
1. 사용자가 송금 직전 transfer check를 요청한다.
2. 시스템이 `ALLOW`, `WARN`, `BLOCK` 중 하나를 판단한다.
3. 사용자에게 reason codes, user message, cooldown, 추가 확인 필요 여부를 돌려준다.
4. Remittance는 이 결과를 반영해 실제 송금 요청 가능 여부를 결정한다.

### 주요 규칙
- SafePay는 설명 가능한 보호 계층이어야 한다.
- decision은 `ALLOW`, `WARN`, `BLOCK`으로 단순화한다.
- reason codes는 사용자 메시지와 함께 제공한다.
- cooldown과 추가 확인 여부는 P0에서 함께 반환한다.
- 절대/상대 금액 혼합 기준은 사용하되 수치는 후속 정책 문서에서 조정 가능하게 둔다.

### API 매핑
- `POST /api/safepay/transfer-checks`

### v0 가정
- `RECIPIENT_IN_COOLDOWN`, `AMOUNT_TOO_HIGH`, `RECIPIENT_NOT_ALLOWLISTED`, `DUPLICATE_TRANSFER_SUSPECTED` 수준의 reason code 세트를 우선 사용한다.
- 정책 수치 자체는 문서 외부에서 조정 가능하게 둔다.

### 열린 질문
- SafePay 고액 기준을 절대값과 상대값 중 어떤 조합으로 고정할지 여부

## 10. Vault

### 상태
- `P0-확정`

### 목표
- testnet vault에 예치하고 출금하는 경험과 예상 이자 미리보기를 제공하되, 실제 수익 상품처럼 보이지 않게 한다.

### 범위
- Vault 요약 조회
- 예치 요청 생성
- 출금 요청 생성
- 거래 상태 조회

### 범위 제외
- 목표 저축(P1)
- 실제 메인넷 연동(P1)
- 실제 수익 보장 또는 실정산

### 핵심 흐름
1. 사용자가 Vault summary에서 현재 principal, 지갑 잔액, 예상 이자 미리보기를 본다.
2. 사용자가 `deposit` 요청을 생성한다.
3. 백엔드는 testnet 트랜잭션을 비동기로 제출하고 상태를 갱신한다.
4. 사용자는 거래 목록/상세에서 `REQUESTED`, `BROADCASTED`, `CONFIRMED` 상태를 확인한다.
5. 필요 시 `withdraw` 요청으로 일부 금액을 지갑으로 되돌린다.

### 주요 규칙
- Vault는 testnet/demo 범위에서만 동작한다.
- 서버는 저장된 사용자 지갑 키를 사용해 testnet 예치/출금 트랜잭션을 처리한다.
- 동일 사용자 기준 active vault transaction은 한 번에 하나만 허용한다.
- `Idempotency-Key` 헤더로 중복 요청을 제어한다.
- 이자 미리보기는 예시값이며 수익 보장을 의미하지 않는다.

### API 매핑
- `GET /api/vault/summary`
- `POST /api/vault/deposits`
- `POST /api/vault/withdrawals`
- `GET /api/vault/transactions`
- `GET /api/vault/transactions/{requestId}`

### v0 가정
- `interestPreview`는 예시 APY 기반 추정값이다.
- deposit/withdraw는 testnet 환경에서만 수행한다.
- Remittance와 Vault는 같은 사용자 서버 지갑을 재사용한다.
- wallet token balance가 곧 송금 가능 토큰 잔액이다.

### 열린 질문
- Vault의 기본 preview yield 수치를 어떤 값으로 둘지 여부

## 11. Copilot

### 상태
- `P0-초안`

### 목표
- 자유 대화형 챗봇이 아니라, 현재 화면을 더 쉽게 이해하고 설명 가능한 다음 행동을 고르게 돕는 화면 보조 계층이 된다.

### 범위
- 화면 설명 생성
- 추천 질문 칩 제공
- 제출용/공유용 요약 문장 생성
- facts 기반 번역과 쉬운 표현 변환

### 범위 제외
- 자유 주제 대화형 챗봇
- 새로운 숫자 계산 또는 정책 판정
- 법률·재무 조언
- 대규모 RAG 지식봇

### 핵심 흐름
1. 사용자가 홈, Wage, SafePay, Instant Claim 같은 핵심 화면에서 `이 화면 설명해줘` 또는 추천 질문 칩을 선택한다.
2. 화면은 서버 facts와 현재 화면 종류를 Copilot에 전달한다.
3. Copilot은 facts를 재구성해 설명, 제출용 문장, 번역 중 하나를 생성한다.
4. 사용자는 답변과 근거 facts, 추가 질문 칩, 공통 디스클레이머를 함께 본다.

### 주요 규칙
- Copilot은 `설명 / 번역 / 문장 생성`만 수행한다.
- 서버가 내려준 facts 밖의 내용을 추정하거나 숫자를 새로 계산하지 않는다.
- 필요한 facts가 없으면 `확인할 수 없음` 또는 `추정 불가`로 답해야 한다.
- 모든 응답에는 `이 안내는 화면에 표시된 사실을 바탕으로 작성된 참고 문장입니다.` 문구를 함께 보여준다.
- Copilot은 서버 룰 결과를 덮어쓰지 않고, 기존 화면 판단을 더 읽기 쉽게 다시 쓰는 역할만 한다.

### API 매핑
- `POST /api/copilot/answers`

### v0 가정
- 하나의 intent 기반 endpoint가 `설명 / 제출용 요약 / 번역` 세 가지를 처리하는 방향을 우선 사용한다.
- 추천 질문 칩은 화면 유형과 locale 기준으로 응답 안에 같이 반환할 수 있다.
- RAG는 필수 범위가 아니며, 화면 facts만으로 해결되지 않는 짧은 용어 설명 정도만 후속 후보로 남긴다.

### 열린 질문
- Copilot을 intent 기반 단일 endpoint로 둘지, explain / claim-summary / translate 분리 endpoint로 둘지 여부
- 추천 질문 칩을 서버가 완전히 내려줄지, 화면별 고정 세트를 프론트에서 일부 들고갈지 여부

## 12. Demo Time Travel

### 상태
- `P0-초안`

### 목표
- 실제 한 달 사용자 여정을 `15~30초` 안에 보여주기 위해, 고정 시드와 `asOf` 날짜만 바꿔도 핵심 화면 상태가 자연스럽게 이어지는 데모 장치를 제공한다.

### 범위
- demo seed 주입
- demo reset
- `asOf` 기준 상태 재렌더
- demo mode meta/state 조회
- 읽기 API의 `X-Demo-AsOf` 반영 규칙

### 범위 제외
- 일반 사용자용 기능 노출
- 릴리스 빌드 노출
- 실제 운영 데이터 되감기 기능
- 발표 스크립트 자체 자동화

### 핵심 흐름
1. 데모 시작 전 고정 시드 데이터를 주입한다.
2. 사용자는 슬라이더나 재생 버튼으로 `asOf` 날짜를 바꾼다.
3. 시스템은 기준일 이전 데이터만 반영해 Home, WorkProof, Advance, Wage, Remittance, Vault 같은 읽기 화면을 다시 계산한다.
4. 데모 상태 조회는 현재 seed, 허용 날짜 범위, 현재 `asOf`를 돌려주고, 일반 읽기 API는 `X-Demo-AsOf`를 통해 같은 시점을 해석한다.

### 주요 규칙
- 시드 데이터는 고정하되, 집계/계산/판정 로직은 실제 로직을 사용한다.
- Demo Time Travel은 demo account / demo mode에서만 활성화한다.
- `X-Demo-AsOf`는 demo mode에서만 유효하고, 일반 계정이나 릴리스 빌드에서는 무시하거나 차단한다.
- `asOf`가 바뀌면 화면 일부가 아니라 전체 사용자 여정이 자연스럽게 이어져야 한다.
- 데모용 기능이더라도 Home, Advance, Wage 같은 기존 도메인 결과를 우회해서 임의 값만 보여주면 안 된다.

### API 매핑
- `POST /api/demo/seed`
- `POST /api/demo/reset`
- `GET /api/demo/state?asOf=YYYY-MM-DD`
- 공통 헤더: `X-Demo-AsOf: YYYY-MM-DD`

### v0 가정
- `asOf`는 날짜 단위로만 다루고 시간 단위 이동은 후속 범위로 남긴다.
- 데모 시나리오는 고정된 seed 세트 중심으로 운용한다.
- 개별 도메인 화면은 별도 demo 전용 API보다 기존 읽기 API + `X-Demo-AsOf`를 우선 사용한다.

### 열린 질문
- `GET /api/demo/state`가 메타만 돌려줄지, 조합 상태까지 같이 줄지 여부
- `X-Demo-AsOf`를 어떤 읽기 API까지 공통 지원 대상으로 둘지 여부

## P0 상태 메모
- `Money Home`, `Copilot`, `Demo Time Travel`은 PRD P0에 맞춰 문서에 포함하되, 현재는 `P0-초안` 상태의 최소 스케치로 남긴다.
- `W7 WorkProof Integrity`는 별도 top-level 기능으로 분리하지 않고 WorkProof 하위 `P0-후속 세부화` 규칙으로 포함한다.
- `WS5`, `A5`, `Vault V3/V4`처럼 PRD상 P1 또는 후속 범위는 여전히 제외한다.

## 다음 보강 후보
- `summary`, `relatedDocuments[]`, `checklist[]`, `relatedLinks[]` 같은 중첩 object의 최소 의미 정의 보강
- Home / Copilot / Demo Time Travel의 최소 DTO와 상태 필드 우선순위 좁히기
- backend DTO / validation / error code 구체화로 연결되는 다음 문서 초안 작성
