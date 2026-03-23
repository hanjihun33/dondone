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

## Implementation Steps
1. 정책/용어 고정
   - `정정요청`과 `검토 필요`의 UI/도메인 역할 정리
   - 자동 보정, 검토 요청, 승인/반려 상태 전이 명시
2. 회사 공통 출퇴근 정책 설정 추가
   - backend 엔티티/DTO/API
   - `SettingsPage.tsx`는 후속 커밋에서 반영
3. 근무 기록 인정 시간 모델 추가
   - `WorkProof`에 인정 시간 필드 도입
   - 기존 read-model/wage 호출은 우선 호환 상태 유지
4. worker 앱 검토 요청 연동
   - `saveWorkproofEdit()` 로컬 흐름 제거
   - 출근/퇴근 사유/첨부 UI를 backend 요청으로 전환
5. 자동 보정 분기 추가
   - `18:05~18:30` + `늦게 눌렀어요` 자동 인정
   - 자동 보정 audit 저장
   - `18:30` 이전 사용자 실수 보정 경로 허용
6. employer 검토 UI/API 확장
   - `검토 필요` 중심 목록/상세
   - 승인 시 최종 인정 시간 수정
   - 반려 사유 입력
7. worker 결과 조회 추가
   - 반려됨/사유 확인
   - 검토 중 상태 확인
8. 계산/read-model 정리
   - wage/advance/documents가 인정 시간 기준으로 정렬되도록 후속 반영

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
1. `docs: 출퇴근 정책 실행 계획 정리`
2. `feat: 회사 공통 출퇴근 정책 설정 추가`
3. `feat: 근무 기록 인정 시간 모델 추가`
4. `feat: 앱 검토 요청 제출 흐름 연동`
5. `feat: 자동 보정과 검토 요청 분기 추가`
6. `feat: 고용주 검토 승인 시 인정 시간 수정 추가`
7. `feat: 근로자 검토 결과 조회 추가`
8. `refactor: 검토 필요 read-model과 계산 로직 정리`

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
