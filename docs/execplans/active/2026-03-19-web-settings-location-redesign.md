# Source Inputs
- 사용자 요청: 설정 페이지를 첨부 이미지(지도 + 반경 설정 + 상세 주소 입력)와 유사하게 재구성
- 기존 구현 확인:
  - `apps/dondone-web/src/pages/settings/SettingsPage.tsx`
  - `apps/dondone-web/src/pages/settings/components/SettingsSectionGrid.tsx`
  - `apps/dondone-web/src/pages/settings/model/settingsData.ts`
  - `apps/dondone-web/src/shared/styles/global.css`
- 제약:
  - 웹 UI 중심 작업
  - 서버 연동/지도 SDK 연동 없음

# Goal
설정 페이지를 기존 정책 섹션 그리드 UI에서 위치 설정 중심 UI로 교체하여, 사용자가 지도 영역에서 위치를 확인하고 반경과 상세 주소를 입력하는 흐름을 제공한다.

# In Scope
- `SettingsPage` 레이아웃 전면 교체
- 지도 미리보기(정적 모형), 핀/반경 표현, 지도 컨트롤 모형 추가
- `반경 설정` 드롭다운 및 `상세 주소` 입력 필드 구성
- 저장 상태 버튼 유지(초기화/저장)
- 페이지 반응형 조정

# Out of Scope
- 실제 지도 API(Google/TMap/Kakao/Naver) 연동
- 좌표 저장/주소 검색 자동완성
- 백엔드 저장 API 연동

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- 본 실행계획 문서 추가

## Shared
- `apps/dondone-web/src/pages/settings/SettingsPage.tsx`
- `apps/dondone-web/src/pages/settings/components/SettingsSectionGrid.tsx` (정리 대상)
- `apps/dondone-web/src/pages/settings/model/settingsData.ts` (정리 대상)
- `apps/dondone-web/src/shared/styles/global.css`

# Contract Changes
- 외부 API/DTO/DB 계약 변경 없음
- 프론트 내부 컴포넌트 계약 변경:
  - `SettingsSectionGrid` + `settingsData` 기반 렌더 제거
  - `SettingsPage` 내부 상태(`radius`, `detailAddress`) 중심으로 단순화

# Security Notes
- 인증/인가 영향 없음
- 민감정보 처리 경로 추가 없음
- 네트워크 호출 없음

# Maintainability Notes
- 복잡한 폼 메타데이터 구조를 제거해 현재 요구(UI 단일 흐름)에 맞는 단순 상태 구조로 축소
- 클래스 네임을 `location-settings-*`로 분리해 기존 설정 스타일과 충돌 최소화
- 추후 지도 SDK 연동 시 지도 영역을 컴포넌트 단위로 교체하기 쉽게 블록 경계 유지

# Implementation Steps
1. `SettingsPage.tsx`를 위치 설정 중심 구조로 교체
2. 불필요해진 `SettingsSectionGrid.tsx`, `settingsData.ts` 파일 제거
3. `global.css`에 위치 설정 전용 스타일 추가
4. `npm run build`로 타입/번들 검증
5. Playwright로 `/settings` 화면 비교 확인

# Test Plan
- `cd apps/dondone-web && npm run build`
- 수동 확인:
  - `/settings` 진입 시 지도 상단 블록, 반경 선택, 상세주소 입력 렌더 확인
  - `초기화` 동작으로 값 원복 확인
  - `설정 저장` 상태 전환 확인
  - 모바일 폭에서 레이아웃 깨짐 여부 확인

# Review Focus
- 삭제 파일 참조 잔존 여부(컴파일 에러)
- 상태 변경 흐름(반경/주소/저장 상태) 정확성
- 첨부 이미지와의 시각적 유사성(간격, 타이포, 입력 박스 톤)
- 기존 라우팅/메뉴 동작 유지 여부

# Worktree Split Decision
Single lane

단일 페이지와 전역 CSS를 함께 수정하며, 삭제 파일 정리까지 한 번에 끝내는 작업이라 병렬 분할 이점이 낮다.

# Commit Plan
1. `feat(web): redesign settings page to location-first layout`
2. `style(web): add location setting map-like UI styles`

# Open Questions
- 없음

# Assumptions
- 지도는 데모용 정적 시각 모형으로 구현해도 요구 충족으로 본다.
- 첨부 이미지의 핵심은 구조/간격/입력 UX이며 지도 데이터 정확도는 요구 범위 밖이다.
