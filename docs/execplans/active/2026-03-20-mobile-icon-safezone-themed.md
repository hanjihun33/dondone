# Source Inputs
- 사용자 요청: 모바일 런처 아이콘 safe zone 재구성 및 Android 13 themed icon 대응
- 기존 Android 리소스: `apps/dondone-mobile/android/app/src/main/res/mipmap-*`, `mipmap-anydpi-v26`, `drawable/ic_launcher_foreground.png`
- 원본 아이콘: `apps/dondone-mobile/assets/branding/Dondone_icon.png`

# Goal
기존 런처 아이콘을 adaptive icon safe zone에 맞게 재구성하고, Android 13 이상에서 사용할 monochrome themed icon 리소스를 추가한다.

# In Scope
- `ic_launcher_foreground` 재생성
- `ic_launcher_monochrome` 추가
- `ic_launcher.xml`, `ic_launcher_round.xml`에 monochrome 연결
- Android 리소스 빌드 검증

# Out of Scope
- 앱 브랜딩 전면 변경
- 웹/백엔드 변경
- 아이콘 외 다른 모바일 UI 수정

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/res/drawable/`
- `apps/dondone-mobile/android/app/src/main/res/mipmap-anydpi-v26/`
- `apps/dondone-mobile/assets/branding/`

## Docs
- `docs/execplans/active/2026-03-20-mobile-icon-safezone-themed.md`

## Shared
- 없음

# Contract Changes
- 없음

# Security Notes
- 인증, 토큰, 권한, 외부 노출 경로 영향 없음

# Maintainability Notes
- 런처 아이콘 원본은 `assets/branding`에 유지하고, 생성 산출물은 Android `res/` 아래에만 둔다.
- foreground/background/monochrome 역할을 분리해 다음 교체 때 safe zone 재검토 범위를 줄인다.

# Implementation Steps
1. 현재 원본 아이콘에서 심볼 영역을 분리해 safe zone 여백을 가진 foreground 자산을 생성한다.
2. 단색 themed icon용 monochrome 자산을 생성한다.
3. adaptive icon XML 두 개에 monochrome drawable을 연결한다.
4. `:app:assembleDebug`로 리소스 링크와 패키징을 확인한다.

# Test Plan
- `.\gradlew.bat :app:assembleDebug`
- 필요 시 생성된 drawable/mipmap 파일 존재 여부 확인

# Review Focus
- foreground가 배경과 분리되어 adaptive mask에서 잘리지 않는지
- Android 13 themed icon용 monochrome 리소스가 실제 XML에 연결됐는지
- 기존 레거시 `mipmap-*` 아이콘이 유지되는지

# Worktree Split Decision
- Single lane

공유 Android 리소스와 동일 아이콘 세트를 함께 조정하는 작업이라 병렬 분리가 이득보다 충돌 위험이 크다.

# Commit Plan
- `fix: 모바일 런처 아이콘 safe zone 보정`

# Open Questions
- 없음

# Assumptions
- 현재 제공된 `Dondone_icon.png`를 공식 원본으로 사용한다.
- 데모 품질 기준에서 PNG 기반 monochrome drawable 사용은 허용 가능하다.
