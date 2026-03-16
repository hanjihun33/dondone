# Source Inputs
- 사용자 요청: `WorkproofUiModel.kt` 코드 리뷰 지적사항을 포함해 리팩토링
- 리뷰 결과:
  - 미래 날짜와 기준월 외 날짜가 `미기록`으로 오표시됨
  - `근무일` 수치가 금융/선지급 흐름의 검증 규칙과 불일치함
  - UI 문자열이 코드에 하드코딩되어 있고 unicode escape 형태로 남아 있음
- 관련 코드:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/calculator/WorkproofCalculator.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/calculator/AdvanceCalculator.kt`
  - `apps/dondone-mobile/android/app/src/main/res/values/strings.xml`
- 테스트 참고:
  - `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/home/presentation/HomeUiModelTest.kt`

# Goal
- WorkProof 화면의 달력/요약/최근 기록 표시를 evidence-first 방향에 맞게 정리한다.
- `근무일` 수치를 공통 검증 계산과 일치시켜 화면 간 계약 불일치를 없앤다.
- WorkProof UI 문자열을 Android 리소스로 이동해 가독성과 유지보수성을 높인다.

# In Scope
- `WorkproofUiModel`의 상태 파생 규칙 정리
- 미래 날짜와 기준월 외 날짜를 `미기록`과 구분하는 달력 표현 계약 추가
- WorkProof 화면에서 새 달력 상태를 반영하도록 렌더링 수정
- WorkProof 관련 하드코딩 문자열의 `strings.xml` 이동
- WorkProof UI 모델 단위 테스트 추가

# Out of Scope
- 백엔드/API/DB/Auth 변경
- WorkProof 외 다른 화면의 카피 전면 수정
- 월별 실제 데이터 소스 확장
- Compose UI instrumentation 테스트 신규 도입

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- `apps/dondone-mobile/android/app/src/main/res/values/strings.xml`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModelTest.kt`

## Docs
- `docs/execplans/active/2026-03-16-workproof-ui-contract-refactor.md`

## Shared
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/calculator/WorkproofCalculator.kt` 참조 규칙

# Contract Changes
- `WorkproofCalendarTone`에 "미래/비활성" 성격의 상태를 추가하거나, 동일 의미를 표현할 수 있도록 `calendarDayTones`/달력 셀 계약을 조정한다.
- `verifiedDaysText`는 `WorkproofCalculator.verify(state).verifiedDays` 기준으로 맞춘다.
- 최근 기록/감사 문구는 문자열 리소스를 통해 조합한다.

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 민감 정보/파일 업로드 동작 자체는 변경하지 않음

# Maintainability Notes
- 근무 검증 규칙은 `WorkproofCalculator`를 단일 소스로 유지하고 UI에서 중복 계산하지 않는다.
- 달력의 "기록 없음"과 "아직 판단 대상 아님"을 같은 enum 값으로 섞지 않는다.
- 문자열은 리소스로 이동하되, 과한 추상화 없이 WorkProof 범위만 정리한다.
- 화면 렌더링과 상태 파생 책임을 분리해, 표시 변경이 계산 규칙을 다시 복제하지 않게 한다.

# Implementation Steps
1. `WorkproofUiModel`과 `WorkproofScreen`의 달력 상태 계약을 재설계한다.
2. 미래 날짜와 기준월 외 날짜가 `미기록`처럼 보이지 않도록 셀 렌더링을 수정한다.
3. `verifiedDaysText`를 공통 계산기 결과로 교체한다.
4. 최근 기록/감사 문구를 `strings.xml`로 이동하고 Compose에서 `stringResource`로 소비한다.
5. `toWorkproofUiModel()` 테스트를 추가해 미래 날짜, 검증 근무일, 수정 기록 문구를 고정한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew --no-daemon testDebugUnitTest`
- 필요 시 `:app:compileDebugKotlin`로 Compose/리소스 참조 오류 확인
- 자동 테스트가 부족하면 다음 수동 확인 항목 기록:
  - 기준월 미래 날짜가 `미기록`으로 보이지 않는지
  - 다른 달 탐색 시 허위 `미기록`이 생기지 않는지
  - WorkProof `근무일` 수치와 Finance 반영 일수가 일관적인지

# Review Focus
- 달력 상태가 증거 없음과 미래/비활성 상태를 혼동하지 않는가
- 검증 근무일 수치가 다른 화면과 일관적인가
- 문자열 리소스화가 화면 의미를 깨지 않았는가
- 새 enum/계약이 WorkProof 화면 외 사용처에 불필요한 결합을 만들지 않는가

# Worktree Split Decision
- Single lane

`WorkproofUiModel`과 `WorkproofScreen`이 같은 표시 계약을 공유하고, 문자열 리소스와 테스트도 함께 움직여야 해서 병렬 분할 이점이 작다.

# Commit Plan
- `refactor: align workproof ui model contracts`

# Open Questions
- 없음

# Assumptions
- 미래 날짜와 기준월 외 날짜는 "미기록"이 아니라 비활성/판단 제외 상태로 보여주는 것이 맞다.
- WorkProof 요약의 `근무일`은 금융/선지급 계산과 같은 `verifiedDays`를 써야 한다.
- WorkProof 범위 문자열 리소스화는 이번 작업 범위에 포함한다.


# Follow-up Scope Extension (2026-03-16)
- 추가 사용자 요청: Android `기록 보기` 이후 하단 홈/루트 탭 이상과 깨진 `TransferUiModelTest`까지 함께 정리
- 추가 In Scope:
  - `DonDoneApp`, `DonDoneNavGraph`, `Route`에 루트 탭 이동 helper 정리
  - `WorkproofScreen`의 상세/수정 임시 상태를 탭 전환 시 reset 가능하게 연결
  - WorkProof 상세 시스템 뒤로가기와 내부 뒤로가기 의미 정렬
  - `TransferUiModel`의 wallet confirmation 금액 문자열 회귀 복구
- 추가 Affected Modules:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/Route.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- 추가 Assumptions:
  - 루트 탭은 내부 CTA와 하단 탭이 동일한 back stack 규칙을 따라야 한다.
  - WorkProof 상세/수정 상태는 루트 탭 전환 시 유지하지 않는 쪽이 현재 UX에 더 가깝다.
