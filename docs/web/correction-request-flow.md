# Correction Request Flow

## 목적
- 웹에서 가장 위험한 공유 도메인인 정정 요청 흐름을 별도 문서로 고정한다.

## 현재 웹 화면 근거
- `apps/dondone-web/src/pages/issues/IssuesQueuePage.tsx`
- `apps/dondone-web/src/pages/issues/components/IssueQueueList.tsx`

## 시나리오
1. 근로자가 출퇴근 정정 요청 생성
2. 요청이 사업주 큐에 노출
3. 사업주가 승인 또는 반려
4. 승인 시 원본 근로기록 반영
5. 처리 이력과 감사로그 저장
6. 필요 시 관련 집계 read-model 재계산 또는 invalidation

## 상태 후보
- `PENDING`
- `APPROVED`
- `REJECTED`
- 필요 시 `CANCELLED`

## 상태 전이 규칙
- `PENDING -> APPROVED`
- `PENDING -> REJECTED`
- `PENDING -> CANCELLED`
- `APPROVED`, `REJECTED`, `CANCELLED` 이후에는 terminal state로 본다.

## 필수 데이터
- 요청자
- 대상 근로기록
- 원본 값
- 요청 값
- 사유
- 첨부 여부
- 처리자
- 처리 시각
- 처리 메모

## 생성 시 검증
- 요청자가 대상 WorkProof의 owner인지
- 같은 대상에 열린 `PENDING` 요청이 이미 있는지
- 요청값이 원본값과 실제로 다른지
- 요청 사유가 최소 길이 또는 validation을 만족하는지

## 승인 시 확인할 것
- 이미 처리된 요청인지
- 사업주가 해당 근로자 소속을 관리하는지
- 승인 후 WorkProof 반영 방식이 단일한지
- Wage/집계에 재계산이 필요한지
- 승인 처리와 WorkProof 반영이 원자적으로 보장되는지

## 반려 시 확인할 것
- 반려 사유 저장 여부
- 근로자에게 노출할 메시지 정책
- 반려 후 재요청 가능 규칙

## 감사로그
- 누가
- 언제
- 무엇을
- 어떤 값에서 어떤 값으로 바꿨는지

## MVP 확정 규칙
### 원칙
- correction request는 원본 WorkProof를 대체하는 독립 기록이 아니다.
- 승인 시 원본 WorkProof를 업데이트하는 command로 본다.
- 승인 결과는 하나의 request당 한 번만 반영된다.

### 승인 처리 단위
- `CorrectionRequest` 상태 변경
- `WorkProof` 값 반영
- `CorrectionDecisionAudit` 기록
- 관련 read-model invalidation 또는 재계산 표시

이 네 가지는 하나의 transaction으로 묶는 것을 원칙으로 한다.

### 승인 결과
- `WorkProof` 시간 값 반영
- `editReason` 또는 변경 사유 연결
- 필요 시 attachment metadata 연결
- 처리자와 처리 시각 audit 저장
- dashboard/workers/issues read-model 갱신 또는 무효화

### 반려 결과
- `WorkProof`는 변경하지 않음
- request 상태와 반려 사유만 저장
- audit는 남긴다

## 집계 반영 규칙
- 승인된 correction request는 이후 조회되는 dashboard, workers attendance status, wage summary에 반영되어야 한다.
- MVP에서는 실시간 비동기 재계산보다 `다음 조회 시 재계산` 또는 `캐시 무효화` 방식이 우선이다.
- 이미 생성된 외부 문서나 PDF는 자동 수정 대상으로 보지 않는다.

## 실패 처리
- 승인 중 WorkProof 반영 실패 시 request 상태를 성공으로 바꾸지 않는다.
- 이미 처리된 요청의 중복 승인/반려는 `409 CONFLICT`로 막는다.
- 소속 범위가 맞지 않으면 `403 FORBIDDEN`으로 막는다.
- 대상 WorkProof가 없으면 `404 NOT_FOUND`로 처리한다.

## 운영상 보수적 가정
- 부분 승인 기능은 넣지 않는다.
- 승인 후 추가 정정이 필요하면 새 request를 생성한다.
- 하나의 WorkProof에 동시에 열린 `PENDING` request는 1건만 허용한다.

## correction request와 checkout 예외 review의 경계
- 반경 밖 `check-out`으로 인해 생성되는 review 대상 기록은 employer web의 이슈 큐에서 함께 다뤄질 수 있다.
- 다만 이것을 현재 문서 단계에서 `correction request`와 완전히 같은 도메인으로 고정하지는 않는다.
- 시간 수정 요청은 worker가 제출하는 `correction request`로 보고, 위치 이탈 퇴근은 `review 대상 예외 기록`으로 구분하는 편이 MVP 문맥에서 더 안전하다.
- worker 앱에서 반경 밖 `check-out` 시 사유를 어떻게 입력받을지, worker-side check-out request 계약을 어떻게 바꿀지는 현재 웹 문서 범위를 넘는다.
- 이 부분은 추후 app/web 공통 리팩토링 단계에서 worker API, employer queue, WorkProof evidence 정책을 함께 보며 일관되게 정리한다.

## 검증해야 할 edge case
- employer가 승인하려는 시점에 원본 WorkProof가 이미 다른 경로로 수정된 경우
- request 생성 후 workplace settings가 바뀐 경우
- 승인 후 wage summary와 dashboard 수치가 불일치하는 경우

## 열어둘 질문
- 승인 후 새 request 생성 시 원본 snapshot 기준을 무엇으로 볼지
- attachment를 request에 둘지 WorkProof 변경 이력에 둘지
