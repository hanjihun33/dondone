# Source Inputs
- 사용자 요청: 설정 페이지에서 주소 검색 가능하도록 구현
- 현재 상태:
  - `KakaoWorkplaceMap`으로 카카오맵 렌더 및 마커/반경 원 표시
  - 지도 중심 변경은 마커 드래그/클릭 기반
  - 주소 검색 UI/로직 부재
- 참고 파일:
  - `apps/dondone-web/src/pages/settings/SettingsPage.tsx`
  - `apps/dondone-web/src/pages/settings/components/KakaoWorkplaceMap.tsx`
  - `apps/dondone-web/src/shared/styles/global.css`

# Goal
설정 페이지에 주소 검색 입력/실행 UX를 추가하고, 검색 결과 좌표로 지도 중심, 마커, 반경 원을 동기화한다.

# In Scope
- 주소 검색 입력/버튼 UI 추가
- 카카오 지도 SDK `services` 라이브러리 사용(Geocoder)
- 검색 성공/실패 피드백 메시지 추가
- 검색 결과 반영 시 지도 fit 및 중심 좌표 업데이트
- 스타일 보강

# Out of Scope
- 백엔드 저장/조회 API 연동
- 검색 결과 다건 리스트 UI(우선 1건 반영)
- 자동완성/최근 검색어

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- 본 실행계획 문서 추가

## Shared
- `apps/dondone-web/src/pages/settings/SettingsPage.tsx`
- `apps/dondone-web/src/pages/settings/components/KakaoWorkplaceMap.tsx`
- `apps/dondone-web/src/shared/styles/global.css`

# Contract Changes
- 외부 API/DTO/DB 계약 변경 없음
- 프론트 내부 컴포넌트 props 계약 확장:
  - 검색 요청 전달
  - 검색 성공/실패 콜백

# Security Notes
- 카카오 JavaScript 키는 공개키 전제
- 도메인 화이트리스트 미등록 시 검색 포함 지도 기능 실패 가능
- 비밀키/서버키 사용 없음

# Maintainability Notes
- 지도 SDK 연동 책임은 `KakaoWorkplaceMap`에 유지
- 페이지는 검색 입력/상태 UX만 담당
- 검색 요청 식별자(`requestId`) 기반으로 중복 처리 방지

# Implementation Steps
1. 카카오 SDK 로더 URL에 `libraries=services` 추가
2. `KakaoWorkplaceMap`에 geocoder 초기화 및 검색 요청 처리 effect 구현
3. `SettingsPage`에 주소 검색 입력/버튼/피드백 상태 추가
4. 지도 컴포넌트 콜백으로 검색 성공/실패 처리
5. 검색 UI 스타일 추가
6. 빌드 및 화면 검증

# Test Plan
- `cd apps/dondone-web && npm run build`
- 수동 검증
  - 주소 검색 성공 시 지도 중심/마커/반경 원 이동
  - 검색 실패 시 오류 메시지 노출
  - 키 누락 상태에서 검색 시 안내 문구 유지

# Review Focus
- 지도 로드/검색 비동기 레이스 조건
- 반경 fit 로직과 검색 이동 충돌 여부
- 검색 상태 메시지 정확성

# Worktree Split Decision
Single lane

지도 컴포넌트/페이지/CSS가 강하게 결합된 UI 동작 변경이므로 단일 lane에서 구현-검증을 연속 처리한다.

# Commit Plan
1. `feat(web): add address search to settings kakao map`
2. `style(web): add address search controls and feedback styles`

# Open Questions
- 없음

# Assumptions
- 최초 버전은 단일 검색 결과(첫 번째 주소)로 지도 이동하는 UX로 충분하다.
