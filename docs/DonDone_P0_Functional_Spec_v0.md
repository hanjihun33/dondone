# DonDone P0 기능 명세 초안 v0

## 문서 정보
- 작성일: 2026-03-10
- 기준 문서:
  - `docs/DonDone_PRD_v1.5.md`
  - `docs/DonDone_P0_API_Contract_v0.md`
- 상태: Draft v0
- 목적: PRD v1.5와 P0 API 계약 초안 v0의 공통 범위를 기준으로, 구현 착수 전에 팀이 기능 범위와 흐름을 빠르게 합의할 수 있는 상위 기능 명세를 제공한다.

## 요구사항 확인

| 항목 | 이번 문서 기준 |
| --- | --- |
| expected behavior | API 초안을 반복해서 읽지 않아도 P0 기능의 목표, 핵심 흐름, 포함/제외 범위, 구현 우선 논점을 이해할 수 있어야 한다. |
| exact scope | `auth`, `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault`를 다룬다. |
| contract changes | 이번 문서는 기능 명세 초안이며 API/DTO/DB 스키마를 새로 변경하지 않는다. 계약은 `docs/DonDone_P0_API_Contract_v0.md`를 기준으로 유지한다. |
| security impact | 기존 JWT 보호 원칙, 타인 리소스 은닉, `testnet/demo only` 제약, evidence-first 문구를 그대로 반영한다. |
| non-functional impact | 문서 생성과 송금은 비동기 처리 전제를 유지하고, Wage는 anomaly detection 중심, Vault/Advance/Remittance는 시뮬레이션 또는 testnet 범위만 다룬다. |

## 범위

### 포함
- Auth
- WorkProof
- Advance
- Wage Shield
- Documents
- Instant Claim
- Remittance
- SafePay
- Vault

### 제외
- Money Home
- Copilot
- Demo Time Travel
- P1 범위 전체

### 범위 판단 원칙
- 이 문서는 PRD P0 전체가 아니라 `PRD P0`와 `P0 API 계약 초안 v0`의 공통 범위를 정리한다.
- PRD에는 있으나 이번 API 초안에서 의도적으로 제외된 항목은 각 기능의 `범위 제외` 또는 문서 하단 메모에 명시한다.
- 문서 표현은 아래 세 가지 경계를 드러내는 것을 목표로 한다.
  - 확정: 현재 공유 문서 기준으로 바로 구현 기준으로 삼을 수 있는 항목
  - v0 가정: 초안 기준 기본값 또는 임시 정책이지만 추후 조정 가능성이 있는 항목
  - 열린 질문: 구현 직전 또는 구현 중 다시 좁혀야 하는 항목

## 공통 원칙
- 제품명은 `DonDone`를 사용한다.
- P0는 `demo/testnet only` 범위다. 실거래, 실정산, 실수익 보장을 의미하지 않는다.
- Wage 결과는 법률·재무 최종 판단이 아니라 `anomaly detection + evidence-first` 보조 기능으로 위치시킨다.
- `/api/auth/signup`, `/api/auth/login`, `/health`, Swagger 외 나머지 기능은 JWT 보호 대상이다.
- 타인 리소스 접근은 허용하지 않으며, 필요 시 `404`로 숨기는 현재 API 초안 원칙을 유지한다.
- 문서 생성과 송금은 비동기 처리 전제를 유지한다.
- 이 문서는 기능 설명을 우선하므로, API 상세 필드 표를 반복 복제하지 않고 `핵심 흐름 / 주요 규칙 / API 매핑` 중심으로 정리한다.

## 도메인 간 연결
- WorkProof는 Advance와 Wage Shield의 근거 데이터다.
- Wage Shield의 verification 결과는 Documents와 Instant Claim의 입력이 된다.
- Remittance는 SafePay 사전 점검과 Documents 영수증 생성 흐름을 함께 가진다.
- Vault는 Remittance와 잔액 해석을 공유하지만, P0에서는 독립적인 시뮬레이션 기능으로 취급한다.

## 1. Auth

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

## 2. WorkProof

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

### 범위 제외
- 지오펜스 자동 알림
- 동시 다중 근무지 활성 계약 운영
- 동일 날짜 다중 근무 허용
- WorkProof Integrity 전용 판정 흐름(W7)
- 문서 자동 반영 세부 규칙의 완전한 고정
- 누락 기록/수정 기록의 승인 워크플로 상세화

### 핵심 흐름
1. 사용자가 근무지와 급여 계약을 등록한다.
2. 출근 시 `workplaceId`와 GPS 좌표를 보내면 서버가 활성 계약을 선택해 기록을 만든다.
3. 퇴근 시 서버가 활성 `CHECKED_IN` 기록 1건을 찾아 같은 기록을 마감한다.
4. 누락되었거나 잘못된 기록이 있으면 provisional 누락 생성 또는 기존 기록 수정으로 보완한다.
5. 사용자는 월별 목록과 상세를 확인하고, WorkProof 월간 요약은 Advance/Wage 입력으로 재사용된다.

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
- 누락 기록 필드와 승인 정책은 구현 중 조정 가능하다.

### 열린 질문
- W7 Integrity를 별도 API/판정 모델로 분리할지 여부
- 누락 기록과 수정 기록의 승인 정책을 어느 수준까지 v0에서 고정할지 여부
- 자정 경계가 걸리는 근무를 월 집계에 어떻게 드러낼지 여부

## 3. Advance

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

## 4. Wage Shield

### 목표
- 월간 근무 요약과 실제 입금액 비교를 통해 `정답 계산`이 아니라 `이상 징후와 근거`를 보여준다.

### 범위
- 월간 근무 요약 조회
- 참고용 예상 급여 조회
- 실제 입금액 기반 verification 생성
- verification 상세 조회
- Documents/Claim로 이어질 수 있는 다음 액션 상태 제공

### 범위 제외
- 급여명세서 자동 파싱
- 법률·재무 최종 판단
- 기관 자동 제출
- P1 고도화 규칙

### 핵심 흐름
1. 사용자가 `month + workplaceId` 기준으로 월간 요약과 예상 급여를 조회한다.
2. 사용자가 실제 입금액을 입력해 verification을 생성한다.
3. 시스템이 차액과 이상 징후를 계산한다.
4. 사용자는 근거 항목, 포함 기록, 수정 기록 개수, 다음 액션 가능 여부를 확인한다.

### 주요 규칙
- Wage 조회는 `month`와 `workplaceId`를 함께 받아야 한다.
- Wage 엔진은 WorkProof 월간 요약 결과를 입력으로 사용한다.
- 급여 계산은 시급 기준으로 정규화해 수행한다.
- 계산 단위는 분 기준이며, 기본급/연장/야간 항목별로 floor 처리한다.
- `CHECKED_IN` 상태 기록은 집계에서 제외하고 pending count로만 관리한다.
- 연장은 일별 `480분` 초과분, 야간은 `22:00~06:00` 겹침 분으로 계산한다.
- 휴일 가산은 P0 범위에서 제외한다.
- 차액 기본 임계값은 `30,000원 또는 2%`, 공제 미반영 추정은 `50,000원 또는 3%`를 사용한다.
- 결과는 `최종 판정`이 아니라 `이상 징후 + 근거`다.

### API 매핑
- `GET /api/wage/monthly-summary?month=YYYY-MM&workplaceId={id}`
- `GET /api/wage/estimate?month=YYYY-MM&workplaceId={id}`
- `POST /api/wage/verifications`
- `GET /api/wage/verifications/{verificationId}`

### v0 가정
- 월간 집계 결과는 조회 시 계산하고 별도 snapshot 저장은 하지 않는다.
- 휴일/법정 예외/더 정교한 근로 규칙은 후속 범위로 남긴다.
- payslip parsing 없이 사용자가 실제 입금액을 직접 입력한다.

### 열린 질문
- 자정 경계 근무를 Wage 계산 근거에 어떻게 표시할지 여부
- 수정 기록을 verification 상세에서 개수 중심으로 볼지, 개별 근거 목록으로 더 강조할지 여부

## 5. Documents

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
1. 사용자가 Wage verification, Claim, Remittance 문맥에서 문서 생성을 요청한다.
2. 시스템은 비동기 요청으로 접수하고 `requestId`, `status`, `pollUrl`을 반환한다.
3. 생성된 문서는 Documents 목록에서 다시 조회할 수 있다.
4. 상세 화면에서 요약과 관련 링크를 보고, 별도 다운로드 URL을 발급받는다.

### 주요 규칙
- 문서 생성은 비동기 처리(`202 Accepted`)를 전제로 한다.
- 문서 생성 계열 API는 `Idempotency-Key`를 필수로 둔다.
- Proof Pack은 월간 요약, WorkProof 상세표, 급여 추정 근거, 수정 이력, 첨부 목록을 기본 포함 대상으로 본다.
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

### 열린 질문
- 제출용 문서 톤을 `공식 제출용`에 더 맞출지, `읽기 쉬운 설명형`에 더 맞출지 여부
- `summary`, `relatedLinks[]`를 기능 명세 수준에서 어디까지 고정할지 여부

## 6. Instant Claim

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
2. 사용자가 wage verification과 선택적 claim kit를 기반으로 preparation을 생성한다.
3. 시스템은 summary text, checklist, suggested routes, related documents를 반환한다.
4. 사용자는 복사, 공유, 바로가기 같은 반자동 지원만 사용하고 직접 제출한다.

### 주요 규칙
- Instant Claim v0는 `반자동 지원`이다.
- route 안내는 locale에 따라 달라질 수 있다.
- claim kit 없이도 preparation을 만들 수 있다.
- `Idempotency-Key`는 v0에서 권장 수준으로 둔다.
- Documents와 Claim은 verification 결과를 재사용한다.

### API 매핑
- `GET /api/claim/routes?locale=ko-KR`
- `POST /api/claim/preparations`
- `GET /api/claim/preparations/{preparationId}`

### v0 가정
- 요약 문구는 facts 기반 템플릿/생성 결과로 다룬다.
- 생성 직후 `READY` 상태를 반환하는 동기형 흐름을 우선 사용한다.
- 비용/캐시 전략이 바뀌기 전까지는 idempotency를 권장으로 유지한다.

### 열린 질문
- claim preparation 생성 비용과 캐시 전략이 정해지면 `Idempotency-Key`를 필수로 전환할지 여부

## 7. Remittance

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

## 8. SafePay

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

## 9. Vault

### 목표
- 남는 돈을 `보관해본다`는 경험과 예상 이자 미리보기를 제공하되, 실제 수익 상품처럼 보이지 않게 한다.

### 범위
- Vault 요약 조회
- 보관 금액 설정
- 보관 금액 해제

### 범위 제외
- 목표 저축(P1)
- 실제 메인넷 연동(P1)
- 실제 예치/락업/수익 실현

### 핵심 흐름
1. 사용자가 Vault summary에서 stored amount, 사용 가능 금액, 이자 미리보기를 본다.
2. 사용자가 일부 금액을 `보관 중` 상태로 이동시킨다.
3. 필요 시 금액을 `SPENDABLE` 또는 `TRANSFERABLE`로 되돌린다.

### 주요 규칙
- Vault는 P0에서 순수 시뮬레이션이다.
- 실제 온체인 예치나 락업은 수행하지 않는다.
- 이자 미리보기는 예시값이며 수익 보장을 의미하지 않는다.
- release의 `target`은 화면 해석과 잔액 분기를 위한 값이다.

### API 매핑
- `GET /api/vault/summary`
- `POST /api/vault/allocations`
- `POST /api/vault/releases`

### v0 가정
- `interestPreview.daily`, `monthly`, `apr`는 시뮬레이션 값이다.
- allocation/release는 시뮬레이션 잔액만 갱신한다.
- Remittance와 잔액 해석은 연결되지만 실제 자금 이동은 아니다.

### 열린 질문
- Vault의 기본 preview yield 수치를 어떤 값으로 둘지 여부

## PRD 대비 의도적 제외 메모
- PRD P0에 포함된 Money Home은 이번 API 초안 범위에 없으므로 본 문서에서도 제외한다.
- Copilot과 Demo Time Travel은 P0 PRD에는 존재하지만 별도 문서 후보로 분리되어 이번 기능 명세에는 포함하지 않는다.
- WorkProof의 Integrity 판정(W7)은 기능 개념은 남기되, 전용 계약과 판정 기준은 이번 공통 범위에서 제외한다.
- Wage의 문서 자동 파싱(WS5), Advance의 상용화(A5), Vault의 목표 저축/실제 연동(V3, V4)은 P1 또는 후속 범위로 남긴다.

## 다음 보강 후보
- `summary`, `relatedDocuments[]`, `checklist[]`, `relatedLinks[]` 같은 중첩 object의 최소 의미 정의 보강
- 구현 우선순위용 최소 DTO/엔드포인트 집합 재절단
- backend DTO / validation / error code 구체화로 연결되는 다음 문서 초안 작성