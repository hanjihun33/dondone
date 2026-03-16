# Findings
- `showDate` root-tab 분기는 현재 route 집합 기준 near-dead 상태다. `ScreenChrome` root branch에서 날짜를 켜는 경우가 실질적으로 없어 셸이 불필요한 파생 계산을 하고 있다.
- 헤더 숨김을 `""` sentinel로 표현하고 `DonDoneApp`에서 다시 `null`로 바꾸는 우회 표현이 남아 있다. 의미가 `타이틀 없음`이라면 nullable state가 더 직접적이다.
- transfer back-step 계산 helper가 `DonDoneApp` 하단에 붙어 있어 app shell 파일이 feature-specific 규칙까지 알고 있다.
- workproof detail/reset 상태는 제거 대상이 아니라 현재 `WorkproofScreen` 계약을 유지하기 위한 활성 연결부다. 이번 범위에서는 ownership 이동 대신 캡슐화 수준 정리가 적절하다.

# Open Questions
- 없음

# Testing Gaps
- Compose UI 계층에 대한 instrumentation 검증은 이번 범위에 포함하지 않는다.
- workproof detail reset 계약은 단위 테스트 대신 기존 화면 계약 유지 전제로 둔다.

# Residual Risks
- dirty worktree 상태라 `DonDoneApp.kt` 인접 auth/advance 변경과의 merge 충돌 가능성은 남아 있다.
- transfer back helper를 분리해도 실제 `BackHandler` 동작은 수동 경로 확인이 가장 확실하다.

# Change Summary
- `DonDoneApp` 셸은 chrome 의미 명시화와 transfer back helper 분리에 집중해 정리한다.
- auth gate와 workproof route 계약은 유지하면서, 제거 가능한 sentinel/dead branch만 우선 정리한다.
