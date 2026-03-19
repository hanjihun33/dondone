# Source Inputs
- 사용자 요청: 설정 페이지 지도 영역을 카카오맵 API로 교체
- 참고 맥락:
  - 모바일 앱이 카카오맵 사용 중
  - 현재 웹 설정 페이지는 정적 지도 모형 UI
- 코드 기준점:
  - `apps/dondone-web/src/pages/settings/SettingsPage.tsx`
  - `apps/dondone-web/src/shared/styles/global.css`
  - `apps/dondone-mobile/android/SETUP.md` (카카오 키 운영 방식 참고)

# Goal
웹 설정 페이지의 지도 섹션을 카카오맵 JavaScript SDK 기반 실제 지도(마커, 반경 원)로 전환하고, 키 누락 시 사용자 안내 fallback을 제공한다.

# In Scope
- 카카오맵 SDK 로더 컴포넌트 추가
- 설정 페이지에 지도/반경 상태 연동
- 키 누락/SDK 로드 실패 fallback 메시지 추가
- 웹 환경변수(`VITE_KAKAO_MAP_APP_KEY`) 타입 및 예시 파일 추가
- 지도 관련 스타일 조정

# Out of Scope
- 백엔드 저장 API 연동
- 주소 검색 API/지오코딩 자동완성
- 지도 공급자 다중 지원
- 모바일 앱 키 관리 방식 변경

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- 본 실행계획 문서 추가

## Shared
- `apps/dondone-web/src/pages/settings/SettingsPage.tsx`
- `apps/dondone-web/src/pages/settings/components/KakaoWorkplaceMap.tsx` (신규)
- `apps/dondone-web/src/shared/styles/global.css`
- `apps/dondone-web/src/vite-env.d.ts` (신규)
- `apps/dondone-web/.env.example` (신규)

# Contract Changes
- 외부 API/DTO/DB 계약 변경 없음
- 프론트 내부 환경계약 추가:
  - `import.meta.env.VITE_KAKAO_MAP_APP_KEY` 사용

# Security Notes
- 카카오맵 JavaScript 키는 클라이언트에 노출되는 공개 키 전제
- 실제 운영 시 카카오 개발자 콘솔에서 웹 도메인 화이트리스트 등록 필수
- 비밀키/네이티브키를 웹 코드에 하드코딩하지 않음

# Maintainability Notes
- SDK 로딩 로직을 `KakaoWorkplaceMap` 컴포넌트로 분리해 페이지 단순성 유지
- 지도 로딩 실패/키 누락 상태를 명시적으로 분기하여 디버깅 가능성 개선
- 지도 표현 변경(다른 공급자) 시 컴포넌트 단위 교체 가능

# Implementation Steps
1. SDK 동적 로더 + 지도 초기화 컴포넌트 구현
2. `SettingsPage`에 반경/좌표 상태 및 지도 컴포넌트 연결
3. 정적 지도 모형 DOM 제거
4. 지도 overlay/fallback 스타일 적용
5. 환경 변수 타입/예시 파일 추가
6. 빌드 및 화면 검증

# Test Plan
- `cd apps/dondone-web && npm run build`
- `/settings` 수동 검증
  - 키 누락 시 안내 overlay 렌더
  - 키 존재 시 지도 렌더 및 마커 이동 시 좌표 업데이트
  - 반경 옵션 변경 시 원 반경 업데이트

# Review Focus
- SDK 중복 로드/초기화 누수 여부
- 키 누락 fallback 동작 정확성
- 반경/좌표 상태가 지도 오버레이와 일치하는지
- 기존 설정 페이지 액션(초기화/저장 상태) 회귀 여부

# Worktree Split Decision
Single lane

지도 SDK 연동은 설정 페이지와 스타일, 환경변수 타입 파일을 함께 수정해야 하며 순차 검증이 필요하므로 단일 lane이 적합하다.

# Commit Plan
1. `feat(web): integrate kakao map in settings workplace map`
2. `chore(web): add kakao map env typing and example`

# Open Questions
- 웹용 카카오 JavaScript 키 발급/도메인 등록 정보 확인 필요

# Assumptions
- 웹에서는 `VITE_KAKAO_MAP_APP_KEY`를 사용하고, 값이 없으면 fallback 안내를 보여주는 것이 현재 요구에 부합한다.
