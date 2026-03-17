# 2026-03-17 dondone-web bootstrap

## Source Inputs
- `docs/DonDone_PRD_v1.5.md`
  - `6.8 고용주: 운영 콘솔과 요약 카드 확인`
  - `7I. 고용주 기능(Employer Support, P0 지원 기능)`
  - `12.7 고용주 기능 안내 문구(필수)`
- 기존 데모 자산
  - `apps/dondone-mobile/mockup/mockup.html`
  - `apps/dondone-mobile/mockup/employer-console.html`
- 저장소 탐색 결과
  - 루트 `package.json`/workspace 설정 없음
  - 기존 웹 앱 골격 없음

## Goal
- `apps/dondone-web`에 독립 실행 가능한 React + TypeScript 웹 앱을 추가한다.
- DonDone 브랜드 톤을 유지한 회사용 운영 콘솔 첫 화면을 구현한다.
- 데모용 정적 데이터와 실행 가이드를 함께 제공한다.

## In Scope
- `apps/dondone-web` 디렉토리 생성
- Vite 기반 React/TypeScript 최소 구성 추가
- 회사용 운영 콘솔 대시보드 1개 페이지 구현
- 공용 스타일, 데모 데이터, 간단한 라우팅/앱 셸 구성
- 실행 방법 문서화

## Out of Scope
- 실제 API 연동
- 인증/인가 구현
- 회사용 다중 페이지 완성
- 모바일/백엔드 코드 변경

## Affected Modules
### Backend
- 없음

### Mobile
- 없음

### Docs
- `docs/execplans/active/2026-03-17-dondone-web-bootstrap.md`

### Shared
- `apps/dondone-web/package.json`
- `apps/dondone-web/tsconfig*.json`
- `apps/dondone-web/vite.config.ts`
- `apps/dondone-web/src/**`

## Contract Changes
- 없음
- 데모 데이터는 웹 앱 내부 mock 데이터로만 관리

## Security Notes
- 실제 회사 데이터나 인증 토큰을 다루지 않는다.
- 고용주 기능은 PRD 원칙대로 운영 리스크 설명용 화면으로만 표현한다.

## Maintainability Notes
- `apps/dondone-web`은 독립 앱으로 두어 모바일 mockup 정적 자산과 분리한다.
- 첫 구현에서는 `app / pages / shared / mocks` 정도의 얕은 구조로 시작하고, 기능이 늘 때 `features` 분리를 고려한다.
- PRD 설명 문구와 데모 표시 문구는 화면 곳곳에 하드코딩하지 말고 상수 또는 mock 데이터 근처로 모은다.

## Implementation Steps
1. `apps/dondone-web` 앱 골격과 실행 스크립트를 추가한다.
2. 공용 글로벌 스타일과 앱 셸을 만든다.
3. PRD 기반 회사용 운영 콘솔 대시보드 페이지를 React 컴포넌트로 옮긴다.
4. 데모 데이터와 공용 카드/배지 UI를 정리한다.
5. 실행 안내를 최종 응답에 포함한다.

## Test Plan
- `npm install`
- `npm run build`
- 필요 시 `npm run dev`로 수동 확인
- 자동 테스트는 이번 범위에서 추가하지 않는다.

## Review Focus
- PRD의 고용주 지원 범위를 넘지 않았는지
- 모바일 DonDone 톤과 브랜드 일관성이 유지되는지
- 독립 앱 구조가 이후 확장에 무리가 없는지
- 실행에 필요한 설정 파일 누락이 없는지

## Worktree Split Decision
- Single lane

이 작업은 새 웹 앱 골격, 스타일, 페이지, 실행 설정이 서로 강하게 연결되어 있어 한 레인에서 일관되게 만드는 편이 안전하다.

## Commit Plan
- `feat: 회사용 웹 콘솔 앱 초기 구조 추가`
- 필요 시 후속 스타일/문서 커밋 분리

## Open Questions
- 패키지 매니저를 `npm`으로 고정할지 팀 합의 필요
- 이후 페이지가 늘면 `features` 분리 시점을 언제로 볼지 결정 필요

## Assumptions
- 현재 저장소에는 웹 workspace가 없으므로 독립 앱이 가장 낮은 충돌 해법이다.
- 첫 화면은 PRD의 회사용 운영 콘솔 데모를 설명하는 수준이면 충분하다.
