## Source Inputs
- 사용자 확정 정책 메모
  - 회사 공통 출근/퇴근 기준 시간
  - `18:05~18:30` + `늦게 눌렀어요` 자동 `18:00` 인정
- `18:30 이후`, `18:00 이전 퇴근`, `9:00 이후 출근`은 고용주 검토 필요
- 근로자 앱 반려 결과 노출
- `18:00 이전 퇴근`은 검토 요청 필수, 메모 필수, 첨부 선택
- `18:30` 이전은 사용자의 잘못 누름 보정을 위해 재입력/보정 허용
- 기존 구현 탐색
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofCorrectionRequestService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerCorrectionRequestService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerIssueReadModelService.java`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
  - `apps/dondone-web/src/pages/settings/SettingsPage.tsx`
  - `apps/dondone-web/src/pages/issues/IssuesQueuePage.tsx`
  - `apps/dondone-web/src/pages/issues/components/IssueQueueList.tsx`
- explorer 메모
  - Noether: 모바일 수정 바텀시트는 존재하지만 `saveWorkproofEdit()`는 로컬 state만 수정
  - Hypatia: backend correction request는 존재하지만 자동 보정/인정 시간/회사 공통 출퇴근 정책은 없음
  - Carson: web 설정 수정 진입점은 `SettingsPage.tsx`, issues 목록 골격은 재사용 가능

## Goal
- 출퇴근 정책을 회사 공통 기준 시간 중심으로 재정의하고, 근로자 앱의 요청/고용주 웹의 검토/급여 반영 기준을 같은 도메인 규칙으로 정렬한다.
- 고용주 화면에서는 `정정요청`보다 `검토 필요`를 중심 개념으로 보고, 근로자 요청과 위치 이탈 정보를 같은 처리 흐름으로 수용한다.

## 사용자 설명용 요약
- 이 문서는 "출퇴근 정책 변경을 한 번에 끝내지 말고, 안전한 단위로 끊어서 진행하자"는 실행 계획서다.
- 핵심 방향은 `실제로 찍힌 시간`과 `정책상 인정되는 시간`을 분리하는 것이다.
- 지금까지 완료된 범위
  - Slice 1: 회사 공통 출퇴근 정책 backend 설정
  - Slice 2: 인정 시간 필드와 조회 기반 추가
- 아직 남은 범위
  - Slice 3: 자동 보정과 worker 검토 요청 분기
  - Slice 4: employer 승인/반려와 최종 인정 시간 확정
  - Slice 5~7: mobile, web, wage/advance 후속 반영
- 사용자에게 설명할 때는 아래처럼 풀어서 말하면 된다.
  - Slice 1: 회사 전체가 공통으로 쓰는 출근시간, 퇴근시간, 반올림 단위를 저장하는 단계
  - Slice 2: 근무기록에 `실제 찍힌 시간`과 `인정 시간`을 따로 들고 가는 기반을 까는 단계
  - Slice 3: "늦게 눌렀어요", "18:30 이후 퇴근", "9시 이후 출근" 같은 상황을 자동 보정 또는 검토 요청으로 나누는 단계
  - Slice 4: 고용주가 검토 화면에서 최종 인정 시간을 확정하거나 반려하는 단계
  - Slice 5: 모바일에서 로컬 임시 수정 대신 실제 backend 검토 요청을 보내는 단계
  - Slice 6: 웹 설정/검토 화면을 새 정책에 맞게 연결하는 단계
  - Slice 7: 급여/선지급/문서 계산이 인정 시간 기준으로 맞게 흘러가도록 정리하는 단계

## Slice Progress
- 실제 커밋이 만들어진 뒤에만 이 섹션을 갱신한다.
- 기록
  - `Slice 1 -> feat: 회사 공통 출퇴근 정책 설정 추가` (`6dfa1ce`)
  - `Slice 2 -> feat: 근무 기록 인정 시간 조회 모델 추가` (`cc836a9`)
- 슬라이스와 직접 관련 없는 `chore` 커밋은 여기 적지 않고 별도 유지보수 변경으로 본다.
- 아직 커밋되지 않은 작업은 여기 적지 않고, `Slice Plan`과 `Implementation Steps`만 기준으로 관리한다.

## 다음 작업 제안 규칙
- 다음 작업을 제안하기 전에 먼저 현재 브랜치 기준으로 어디까지 구현되었는지 확인한다.
- 다음 작업을 제안하기 전에 현재 슬라이스의 검증이 끝났는지 확인한다.
- 다음 Slice로 넘어가기 직전에는 마지막 체크 용도로 `review-checklist` 스킬을 우선 사용한다.
- `review-checklist`는 구현 계획용이 아니라, 현재 Slice가 합의된 범위까지 끝났는지와 회귀/계약/테스트 리스크가 남았는지를 확인하는 게이트로 사용한다.
- 사용자에게 설명할 때는 아래 순서로 정리한다.
  - `현재 구현 범위`
  - `검증 완료 범위`
  - `남은 리스크 또는 미검증 범위`
  - `다음 작업 제안`
- 검증이 끝나지 않았으면 다음 작업을 바로 밀지 말고, 무엇이 아직 검증되지 않았는지 먼저 적는다.
- 다음 작업 제안은 항상 "지금 단계가 끝났으니 다음 Slice로 넘어간다"는 식으로, 현재 완료 범위와 연결해서 적는다.

## In Scope
- 회사 공통 출퇴근 정책 설정 추가
  - 기준 출근시간
  - 기준 퇴근시간
  - 추가수당 반올림 단위(`15_MINUTES`만 실제 동작, `30_MINUTES/1_HOUR`는 준비중 표시)
- 근무 기록의 실제 기록 시간과 인정 시간 분리
- 앱의 출근/퇴근 후 로컬-only 수정 흐름 제거 및 backend 검토 요청 연동
- 자동 보정 분기
  - `18:05~18:30` + `늦게 눌렀어요`는 자동 `18:00` 인정
- 검토 요청 생성 분기
  - `18:30 이후`
  - `18:00 이전 퇴근`
  - `9:00 이후 출근`
  - 범위 밖 기록
  - 기타 사유
- 고용주 승인 시 최종 인정 시간 수정
- 고용주 반려 시 근로자 앱에서 반려 상태/사유 확인
- 관련 read-model, wage/advance 영향 정리

## Out of Scope
- 사업장별 출퇴근 정책
- 추가수당 `30분`, `1시간` 실제 계산 구현
- 관리자 화면 변경
- 법/노무 판단 로직 고도화
- 위치 이탈 자체를 별도 엔티티나 별도 요청 타입으로 분리

## Affected Modules
### Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/model/Company.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/dto/response/EmployerWorkplaceSettingsResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerSettingsService` 계열
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/CorrectionRequest.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofCorrectionRequestService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerCorrectionRequestService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerIssueReadModelService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageSummaryCalculator.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/advance/...`

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/workproof/BackendWorkproofRepository.kt`
- worker 검토 결과 조회 UI가 붙는 세션/기록 화면

### Docs
- `docs/execplans/active/2026-03-23-attendance-policy-flow.md`
- 정책/계약 변경 시 `docs/web/auth-and-role-policy.md`, `docs/web/implementation-slices.md` 또는 대응 문서

### Shared
- employer/mobile DTO 계약
- issue status, correction request status, workproof reflection status 정의

## Contract Changes
- 회사 설정 응답/저장 DTO에 추가
  - `scheduledClockInTime`
  - `scheduledClockOutTime`
  - `overtimeRoundingUnit`
- `WorkProof` 또는 대응 응답에 실제 기록 시간과 인정 시간 분리 필드 추가
  - 예: `recognizedClockInAt`, `recognizedClockOutAt`
- worker correction/review request 생성 DTO 확장 또는 신규 DTO 추가
  - 사유 타입
  - 자유입력 메모
  - 증빙 첨부(선택)
  - 요청 대상 시간
- employer 승인 DTO 확장
  - 최종 인정 시간 입력
  - 반려 사유 입력
- worker 조회 응답 확장
  - 검토 상태
  - 반려 사유
  - 범위 밖 기록 여부

## Security Notes
- 회사 공통 정책 설정 수정은 employer scope만 허용
- worker 검토 요청 생성은 본인 `WorkProof`에만 허용
- employer 승인/반려는 본인 회사 scope 내 요청만 허용
- 반려 사유/첨부는 요청 당사자(worker)와 스코프 고용주만 볼 수 있게 유지
- 기존 JWT/stateless 구조와 exposed path 정책은 변경하지 않음

## Maintainability Notes
- `WorkProof`의 실제 기록 시간과 인정 시간을 같은 필드에서 처리하면 정책 변경이 계속 누적되므로, 이번 작업에서 역할 분리를 명시해야 한다.
- 모바일 `DemoSessionViewModel`과 `DemoSessionReducer`는 다른 도메인 상태도 함께 관리하므로 첫 단계에서는 접점을 최소화하고, 로컬-only 편집 흐름만 제거 대상으로 본다.
- web의 `IssuesQueuePage.tsx`/`IssueQueueList.tsx`는 목록 골격은 재사용하되 `CORRECTION_REQUEST`와 `REVIEW_REQUIRED_RECORD` 분기를 더 키우지 않는다.
- `NEEDS_REVIEW`는 wage/advance/read-model에 이미 퍼져 있으므로, 상태명만 바꾸지 말고 소비 지점을 같이 정리해야 한다.

## Slice Plan
### Slice 1. 회사 공통 출퇴근 정책 backend 설정
- 상태: 현재 브랜치에 반영됨
- 범위: `Company`, employer settings DTO/response, settings service, 관련 backend 테스트
- 종료 기준: employer 설정 API에서 기준 출근/퇴근 시간과 반올림 단위를 조회/수정할 수 있고 validation/auth가 보장된다.
- 비고: 이미 반영된 범위이므로 이후 슬라이스에서 계약 드리프트가 생길 때만 재진입한다.

### Slice 2. 인정 시간 도메인 기반 추가
- 상태: 기반 반영 완료
- 범위: `WorkProof`와 관련 응답에서 실제 기록 시간과 인정 시간을 분리하고, 기존 reflection/status 계산이 호환 상태를 유지하도록 정리한다.
- 주요 파일: `workproof/model/WorkProof.java`, `workproof/api/dto/response/*`, `EmployerWorkerReadModelService`, `EmployerIssueReadModelService`
- 종료 기준: 인정 시간 필드가 저장/조회 가능하고, 기존 목록/상세/요약이 컴파일 및 테스트 기준으로 깨지지 않는다.
- 현재 결과: `recognizedClockInAt`, `recognizedClockOutAt`가 backend 모델과 lane1/employer 조회 응답에 추가되었고, 좁은 범위 integration test로 검증했다.

### Slice 3. worker 검토 요청 규칙과 자동 보정 분기
- 범위: worker correction request 흐름을 정책 기반 검토 요청으로 재해석하고, 자동 보정/검토 요청 생성 조건을 backend에서 강제한다.
- 주요 파일: `WorkProofCorrectionRequestService`, `CorrectionRequest`, `CreateWorkProofCorrectionRequest`, 관련 controller/test
- 종료 기준: `18:05~18:30 + 늦게 눌렀어요`, `18:30 이후`, `18:00 이전 퇴근`, `9:00 이후 출근`, 범위 밖 기록`에 대한 분기가 테스트로 고정된다.

### Slice 4. employer 검토 API와 read-model 정렬
- 범위: employer correction/review 조회, 승인, 반려가 인정 시간 모델과 맞물리도록 정리한다.
- 주요 파일: `EmployerCorrectionRequestService`, `EmployerIssueReadModelService`, employer review DTO, 관련 integration test
- 종료 기준: 고용주가 최종 인정 시간을 입력해 승인할 수 있고, 반려 사유와 검토 상태가 read-model에 일관되게 반영된다.

### Slice 5. worker/mobile 연동
- 범위: 앱의 로컬-only 편집 흐름을 제거하고 backend 검토 요청 제출/결과 조회로 전환한다.
- 주요 파일: `DemoSessionViewModel.kt`, `DemoSessionReducer.kt`, `WorkproofScreen.kt`, `BackendWorkproofRepository.kt`
- 종료 기준: worker가 앱에서 검토 요청을 제출할 수 있고, 검토 중/반려 결과를 확인할 수 있다.

### Slice 6. web 설정/검토 화면 연결
- 범위: web settings와 issues 화면을 새 정책/검토 흐름에 연결한다.
- 주요 파일: `SettingsPage.tsx`, `IssuesQueuePage.tsx`, `IssueQueueList.tsx`
- 종료 기준: 회사 정책 설정 저장, 검토 필요 목록 조회, 승인/반려 액션이 동작한다.

### Slice 7. wage/advance/read-model 후속 정리
- 범위: 인정 시간 기준으로 downstream 계산과 snapshot/read-model 소비 지점을 정렬한다.
- 주요 파일: `WageSummaryCalculator`, `advance/...`, `documents/...`, 관련 회귀 테스트
- 종료 기준: 인정 시간 도입 이후에도 wage/advance/documents 결과가 정책과 충돌하지 않는다.

## Implementation Steps
1. Slice 1을 기준선으로 고정
   - 이미 반영된 회사 공통 정책 설정 backend 범위를 재작업하지 않는다.
   - 이후 슬라이스는 이 계약을 기준으로만 확장한다.
2. Slice 2 진행
   - `WorkProof` 인정 시간 모델 추가
   - 기존 조회/요약/상태 계산은 호환 상태 유지
3. Slice 3 진행
   - worker 요청 DTO와 backend 분기 규칙 추가
   - 자동 보정과 검토 요청 생성 조건을 service 레이어에 고정
4. Slice 4 진행
   - employer 승인/반려 API와 issue read-model 정렬
   - 최종 인정 시간 확정 흐름 반영
5. Slice 5 진행
   - mobile의 로컬-only 편집 제거
   - 검토 요청 제출/결과 조회 연결
6. Slice 6 진행
   - web settings/issues 화면 계약 반영
7. Slice 7 진행
   - wage/advance/documents/read-model 소비 지점 회귀 정리

## Test Plan
- Backend
  - 회사 공통 출퇴근 설정 validation/auth integration test
  - 자동 보정 분기 test
  - `9:00 이후 출근`, `18:00 이전 퇴근`, `18:30 이후 퇴근`, 범위 밖 기록 request 생성 test
  - employer 승인 시 최종 인정 시간 수정/반려 test
  - worker 스코프/타사 스코프 authz test
  - wage/advance 영향 regression test
- Mobile
  - `:app:compileDebugKotlin`
  - 사유 선택/첨부/반려 표시 상태 수동 확인
- Web
  - `tsc -b`
  - 설정 저장, 검토 목록, 승인/반려 UI 수동 확인

## Review Focus
- 인정 시간 도입 이후 기존 `workedMinutes()`/summary 계산이 깨지지 않는지
- 자동 보정과 employer 수동 승인 로직이 중복 충돌하지 않는지
- 위치 이탈 정보가 별도 상태 난립 없이 검토 정보에만 추가되는지
- worker 앱의 로컬-only 수정 흐름이 완전히 제거되었는지
- 반려 사유가 worker에게만 적절히 노출되는지

## Worktree Split Decision
- Single lane

공유 엔티티(`WorkProof`, `CorrectionRequest`, `Company`), shared DTO, employer/mobile/web 계약, wage/advance 영향이 동시에 움직인다. 병렬 작업으로 나누면 merge 충돌보다 계약 드리프트 위험이 더 크므로 단일 레인으로 진행한다.

## Commit Plan
1. `docs: 출퇴근 정책 실행 계획 슬라이스 정리`
2. `feat: 근무 기록 인정 시간 모델 추가`
3. `feat: 검토 요청 규칙과 자동 보정 분기 추가`
4. `feat: 고용주 검토 승인/반려 흐름 정렬`
5. `feat: 모바일 검토 요청 및 결과 조회 연동`
6. `feat: 웹 설정 및 검토 화면 반영`
7. `refactor: 인정 시간 기준 read-model과 계산 로직 정리`

## Commit Checkpoint
- 현재 시점은 커밋을 한 번 끊어도 되는 상태다.
- 이유
  - Slice 1은 이미 끝났고, Slice 2도 "인정 시간 필드 추가 + 조회 응답 반영 + 관련 테스트 보강"까지 하나의 묶음으로 정리되었다.
  - 다음 단계인 Slice 3은 자동 보정/검토 요청 규칙이라는 별도 정책 로직이어서 같은 커밋에 섞지 않는 편이 검토와 롤백이 쉽다.
- 문서에는 상세 커밋 묶음을 미리 적지 않고, 커밋해도 되는 시점인지 사용자에게 제안하는 용도로만 쓴다.
- 실제 커밋이 만들어지면 그때 `Slice Progress`에 `Slice n -> <commit message>` 형태로 남긴다.
- 사용자에게 설명할 때는 "지금은 정책 기반을 깔아둔 단계까지 끝났고, 다음 커밋부터 실제 자동 보정 규칙을 붙인다"고 정리하면 된다.

## Open Questions
- 없음. 현 시점에서는 정책 방향이 구현 가능한 수준으로 확정됐다고 본다.

## Assumptions
- 출근/퇴근 기준 시간과 반올림 단위는 회사 공통 설정으로 관리한다.
- `18:00~18:04` 퇴근은 사유 없이 `18:00` 인정으로 본다.
- 범위 밖 기록은 별도 요청 타입이 아니라 검토 정보의 보조 플래그로 저장/노출한다.
- 자동 보정 이력은 별도 목록 UI 없이 내부 audit 또는 기록 상세 수준에서만 확인 가능하다.
- `18:00` 이전 퇴근은 검토 요청이 필요하며, 메모는 필수이고 첨부는 선택이다.
- 출근시간 이전 출근은 기본적으로 기준 출근시간으로 인정하고, 더 이른 시간 인정이 필요하면 검토 요청으로 처리한다.
- 퇴근은 완전 1회 고정이 아니라 `18:30` 이전까지 사용자 보정 구간을 허용하고, 이후는 검토 요청으로 전환한다.
- 다음 구현 시작점은 Slice 3이며, Slice 3이 끝나기 전에는 mobile/web 화면 연동을 먼저 열지 않는다.
