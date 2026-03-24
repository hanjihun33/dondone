# Source Inputs
- 사용자 요청: 메뉴 탭 내 `내 정보 수정` 화면을 제공된 레퍼런스 이미지 스타일로 개선
- 코드 탐색:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/auth/AuthSession.kt`
- AGENTS 가이드: 모바일 상태 명시성 유지, 계약 일관성 유지, 비파괴 최소 변경

# Goal
메뉴 탭의 `내 정보 수정` 시트를 레퍼런스 이미지에 가까운 리스트형 정보 화면으로 재구성하되, DonDone에서 실제 보유하는 프로필 데이터만 반영하고 기존 프로필 업데이트(이름/휴대폰) 동작은 유지한다.

# In Scope
- `MenuProfileSheet` UI 레이아웃 재구성
- 프로필 표시 필드 확장(회사명/사업장명 읽기 전용 표시)
- 기존 이름/휴대폰 수정 및 저장 플로우 유지
- 관련 UI 모델 매핑 및 단위 테스트 보강

# Out of Scope
- 신규 API/DTO/DB 스키마 변경
- 생년월일/영문이름/주소 등 현재 세션 계약에 없는 필드 영속화
- 인증/권한 정책 변경
- iOS 스타일 완전 복제(픽셀 단위 동일성)

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuUiModel.kt`
- (필요 시) `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/menu/presentation/MenuUiModelTest.kt`

## Docs
- 본 실행계획 문서

## Shared
- 없음

# Contract Changes
- 서버/네트워크 계약 변경 없음
- 모바일 내부 UI 모델에 표시용 필드(`companyName`, `workplaceName`) 추가 가능

# Security Notes
- 인증 토큰/권한 경로 변경 없음
- 프로필 표시 데이터는 기존 세션 정보 범위 내에서만 사용

# Maintainability Notes
- 기존 `onUpdateProfile(name, phoneNumber)` 계약을 유지해 ViewModel/Repository 영향 최소화
- 메뉴 시트 내 UI 재사용 가능한 행 컴포넌트를 도입해 하드코딩 중복 최소화
- 도메인 미보유 필드는 임의 저장 없이 읽기 전용 placeholder(`없음`)로만 노출

# Implementation Steps
1. `MenuSessionUiModel`에 읽기 전용 표시 필드(회사명/사업장명) 추가
2. `toMenuUiModel`에서 `AuthSession`의 회사/사업장 값 매핑
3. `MenuProfileSheet`를 리스트형 프로필 화면으로 재구성
4. 이름/휴대폰 입력 컨트롤은 유지하되 화면 하단 저장 액션으로 연결
5. `MenuUiModelTest`에 신규 필드 매핑 테스트 추가

# Test Plan
- 단위 테스트:
  - `MenuUiModelTest`에 회사/사업장 매핑 검증 추가
- 가능 시 실행:
  - `:app:testDebugUnitTest --tests "com.dondone.mobile.feature.menu.presentation.MenuUiModelTest"`
- 환경 제약 시 차단 사유 명시

# Review Focus
- 기존 저장 동작(이름/휴대폰) 회귀 여부
- 읽기 전용 필드가 null/blank에서 `없음` 처리되는지
- 메뉴/프로필 시트 상태 전환(닫기/저장/등록코드 열기) 안정성

# Worktree Split Decision
- `Single lane`
- 사유: 동일 파일(`MenuScreen.kt`) 내 상태/UI 로직 결합이 높고 충돌 위험이 있어 단일 lane이 안전함.

# Commit Plan
1. `feat(mobile): redesign menu profile sheet to list-style info layout`
2. `test(mobile): extend menu ui model test for company/workplace mapping`
3. `docs(execplan): add profile sheet redesign plan`

# Open Questions
- 없음 (현재 요청 기준으로 필요한 필드 범위는 이름/휴대폰/이메일/회사/사업장으로 고정)

# Assumptions
- 사용자 요청의 “필요한 부분만 참고”는 레이아웃/표현을 참고하되, 데이터 계약이 없는 항목은 제외한다는 의미로 해석한다.
- 내 정보 수정 저장 대상은 기존과 동일하게 이름/휴대폰만 허용한다.
