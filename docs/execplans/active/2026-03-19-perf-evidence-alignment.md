# Source Inputs
- `AGENTS.md`
- `docs/CODEX_WORKFLOW.md`
- `C:\c202\S14P21C202\.agents\skills\backend-performance-improvement\SKILL.md`
- `C:\c202\S14P21C202\.agents\skills\backend-performance-improvement\references\perf-report-template.md`
- `C:\c202\S14P21C202\docs\execplans\active\2026-03-18-wage-lane1-perf.md`
- `C:\c202\S14P21C202\docs\reviews\active\2026-03-18-wage-lane1-perf.md`
- `C:\c202\S14P21C202\apps\dondone-backend\build.gradle.kts`
- `C:\c202\S14P21C202\apps\dondone-backend\src\test\java\com\workproofpay\backend\wage\WagePerformanceIntegrationTest.java`
- `C:\c202\S14P21C202\apps\dondone-backend\src\test\java\com\workproofpay\backend\wage\WageExternalPostgresPerformanceIntegrationTest.java`
- 내부/외부 페르소나 검토 결과 요약 메모

# Goal
`wage/estimate` 성능 개선 사례의 증빙 경로, 실행 경로, 스킬 기대치가 서로 어긋나지 않도록 정리한다. 다음 endpoint에서도 같은 방식으로 재사용할 수 있게 최소 증빙 기준과 문서 표현 기준을 명확히 남긴다.

# In Scope
- `backend-performance-improvement` 스킬의 증빙/표현/재사용 가이드 보강
- 성능 보고서 템플릿의 baseline 출처, 실행 명령, fallback 기록 칸 보강
- `2026-03-18-wage-lane1-perf.md` 실행 계획/리뷰 문서 정합성 보강
- `build.gradle.kts` 테스트 task 설명 및 dead path 정리

# Out of Scope
- `WageService.java`, `WorkProofLane1Service.java` 기능 로직 재수정
- 새로운 성능 개선 대상 endpoint 추가
- 부하 테스트 도입
- 전용 perf compose 파일 추가

# Affected Modules
## Backend
- `apps/dondone-backend/build.gradle.kts`

## Mobile
- 없음

## Docs
- `docs/execplans/active/2026-03-18-wage-lane1-perf.md`
- `docs/reviews/active/2026-03-18-wage-lane1-perf.md`
- `docs/execplans/active/2026-03-19-perf-evidence-alignment.md`

## Shared
- `.agents/skills/backend-performance-improvement/SKILL.md`
- `.agents/skills/backend-performance-improvement/references/perf-report-template.md`

# Contract Changes
- API contract 변경 없음
- DB schema 변경 없음
- 테스트 실행/문서 계약만 정리

# Security Notes
- 성능 측정용 외부 Postgres는 개발용 공유 DB와 분리해서 써야 한다는 가이드를 강화한다.
- 테스트가 `deleteAll()`와 `create-drop`를 쓰는 만큼, 측정용 DB가 아니면 위험하다는 점을 문서에 명시한다.

# Maintainability Notes
- 사례 문서와 재사용 자산을 분리해 설명해야 한다. 사례 문서는 복제 대상이 아니라 참고 예시라는 점을 스킬에 못 박아야 한다.
- 실행 경로가 실제 소스와 어긋나는 dead path는 남기지 않는다.
- `query count`와 `prepared statement count`를 혼용하지 않도록 표현 기준을 통일한다.

# Implementation Steps
1. 새 정합성 보강 계획 문서를 추가한다.
2. `build.gradle.kts`에서 현재 없는 direct Docker perf 경로를 정리하고 설명을 실제 상태에 맞춘다.
3. 스킬과 템플릿에 baseline 출처, 실행 명령, fallback, 사례 문서 경계, 표현 가이드를 추가한다.
4. 리뷰 문서와 기존 execplan에서 없는 테스트 언급과 과한 표현을 정리한다.
5. 변경 후 파일 기준으로 실행 경로와 문서 내용이 맞는지 다시 점검한다.

# Test Plan
- 문서/스킬 변경 중심이라 별도 앱 기능 테스트는 추가로 돌리지 않는다.
- 변경 후 `git status --short`와 파일 내용 점검으로 dead path가 제거됐는지 확인한다.
- 필요 시 기존 perf 테스트 실행 명령 문구만 사람이 다시 따라갈 수 있는지 점검한다.

# Review Focus
- 문서와 실제 실행 가능한 테스트/task가 일치하는가
- before/after 근거와 after-only 관측치 표현이 분리되었는가
- 사례 문서와 재사용 자산의 경계가 명확한가
- SSAFY 프로젝트 규모에 맞는 현실적인 요구 수준으로 정리되었는가

# Worktree Split Decision
- `Single lane`

이번 작업은 스킬, 템플릿, 리뷰 문서, Gradle task 설명이 서로 연결된 정합성 수정이다. 범위는 넓지 않지만 파일 간 의존성이 강해서 단일 레인으로 정리하는 편이 안전하다.

# Commit Plan
- 1안: `perf docs and skill alignment`
- 2안: 코드 설정(`build.gradle.kts`)과 문서/스킬 정리를 분리

# Open Questions
- 외부 Postgres 성능 측정용 compose를 저장소에 별도로 둘지 여부
- baseline before 측정을 코드 수준에서 다시 재현할지, 문서 provenance로만 남길지 여부

# Assumptions
- 이번 라운드의 목표는 증빙/문서 정합성 보강이며, 새 성능 수치 재측정은 필수 범위가 아니다.
- SSAFY 프로젝트 단계에서는 `query count before/after + external Postgres before/after` 정도가 충분한 품질 기준이다.
