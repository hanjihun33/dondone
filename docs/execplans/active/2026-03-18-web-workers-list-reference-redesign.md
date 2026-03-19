# Source Inputs
- 사용자 요청: 근로자 목록(`/workers`) 페이지를 제공된 레퍼런스 이미지와 유사한 표 기반 레이아웃으로 재구성
- 기존 코드 탐색:
  - `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx`
  - `apps/dondone-web/src/pages/workers/components/WorkerSummaryList.tsx`
  - `apps/dondone-web/src/pages/workers/model/workerSummaryData.ts`
  - `apps/dondone-web/src/shared/styles/global.css`
- 공통 제약:
  - API/백엔드 계약 변경 없이 웹 UI 레이어에서 처리
  - 기존 라우팅(`/workers`) 유지

# Goal
`/workers` 화면을 카드형 요약 UI에서 표형 근로자 목록 UI로 전환하여, 상단 타이틀/필터칩/목록 테이블/페이지네이션 구조를 레퍼런스 이미지 톤에 맞게 구현한다.

# In Scope
- `workers` 페이지의 JSX 구조 교체
- `workers` 모델 데이터를 테이블 행 중심으로 교체
- `workers` 전용 CSS 추가 및 간격/폰트/정렬 조정
- 반응형(태블릿/모바일)에서 깨지지 않도록 최소 대응

# Out of Scope
- 백엔드 API 연동 및 실데이터 페칭
- 인증/인가, 토큰, 보안 규칙 변경
- 다른 페이지(`/`, `/issues`) 레이아웃 재설계
- 다운로드 기능/실제 파일 동작 구현

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- 실행계획 문서 추가(본 문서)

## Shared
- `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx`
- `apps/dondone-web/src/pages/workers/components/WorkerSummaryList.tsx`
- `apps/dondone-web/src/pages/workers/model/workerSummaryData.ts`
- `apps/dondone-web/src/shared/styles/global.css`

# Contract Changes
- 외부 API/DTO/DB 계약 변경 없음
- 프론트 내부 모델 계약 변경:
  - 카드 요약 배열(`title/description/chips`)에서
  - 테이블 중심 데이터(`columns`, `rows`, `salary`, `contact`, `pagination`)로 변경

# Security Notes
- 인증/인가 경로 변경 없음
- 민감정보 처리 없음(모의 데이터만 사용)
- 외부 요청/업로드/다운로드 동작 추가 없음

# Maintainability Notes
- `workers/model`에 화면 데이터 스키마를 집중시켜 페이지/컴포넌트 분리 유지
- CSS를 `workers` 전용 클래스 네임스페이스로 분리해 대시보드 스타일과 결합도 최소화
- 기존 공통 스타일 클래스 재사용은 최소화하고, 이번 화면 요구에 필요한 범위만 추가

# Implementation Steps
1. `workerSummaryData.ts`를 표 중심 타입/모의 데이터 구조로 교체
2. `WorkerSummaryList.tsx`를 카드 리스트에서 테이블 렌더 컴포넌트로 전환
3. `WorkerSummaryPage.tsx`에서 헤더/필터칩/테이블 섹션을 조합해 렌더
4. `global.css`에 workers 전용 스타일 블록 추가
5. `npm run build` 실행 후 Playwright로 `/workers` 시각 검증 및 간격 미세조정

# Test Plan
- 명령 검증:
  - `cd apps/dondone-web`
  - `npm run build`
- 수동 검증:
  - `/workers` 접속 시 테이블 헤더/행 정렬 확인
  - 상단 칩/버튼, 하단 페이지네이션 표시 확인
  - 768px 이하에서 가로 스크롤/줄바꿈 동작 확인

# Review Focus
- 타입 계약과 컴포넌트 렌더 필드 일치 여부
- CSS 범위 누수(다른 페이지 스타일 오염) 여부
- 텍스트 크기/행간/열 간격이 레퍼런스 의도와 유사한지
- 접근성 기본값(버튼 type, 테이블 semantic) 유지 여부

# Worktree Split Decision
Single lane

이번 변경은 `workers` 페이지와 동일한 전역 스타일 파일(`global.css`)을 함께 수정해야 하므로 병렬 lane 분할 시 충돌 위험이 높다. 단일 lane에서 빠르게 구현/검증 후 필요한 미세조정을 반복하는 방식이 안전하다.

# Commit Plan
1. `feat(web): redesign workers page to table layout`
2. `style(web): tune workers spacing and typography`

# Open Questions
- 없음(현재 요구는 레퍼런스 기반 UI 구현으로 범위가 충분히 명확함)

# Assumptions
- 제공된 이미지는 시각 레퍼런스로 사용하며, 실제 기능(정렬/다운로드/검색 API)은 목업 수준으로 둔다.
- 페이지 언어는 서비스 방향에 맞춰 한국어를 기본으로 사용한다.
