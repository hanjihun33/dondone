# Source Inputs
- 사용자 요구: 네이버 지도 대신 카카오 지도 사용
- 기존 지도 작업:
  - `docs/execplans/active/2026-03-16-mobile-workproof-map-card.md`
  - `docs/execplans/active/2026-03-16-mobile-workproof-naver-map.md`
- 공식 문서:
  - Kakao Developers 시작하기: Android SDK는 네이티브 앱 키 사용
  - KakaoMaps SDK v2 for Android 시작하기: Maven 저장소 `https://devrepo.kakao.com/nexus/repository/kakaomap-releases/`, 의존성 `com.kakao.maps.open:android:2.13.1`
  - `MapView.start()`, `resume()`, `pause()` 필요
- 현재 Android 근무 탭 화면: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`

# Goal
- 근무 탭 지도를 Kakao Maps Android SDK v2로 전환한다.
- 네이티브 앱 키는 코드에 하드코딩하지 않고 `local.properties` 또는 환경변수로 주입한다.
- demo 좌표 기준으로 근무지/현재 위치 표시와 카메라 이동 버튼을 유지한다.

# In Scope
- Kakao Maven 저장소/의존성 추가
- `KAKAO_NATIVE_APP_KEY` 주입 및 `Application` 초기화
- `MapView.start()/resume()/pause()` 기반 Compose 래핑
- Naver 전용 코드 제거

# Out of Scope
- 실제 GPS 수집
- 백엔드/API 계약 변경
- 주소 검색/역지오코딩

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/settings.gradle.kts`
- `apps/dondone-mobile/android/app/build.gradle.kts`
- `apps/dondone-mobile/android/app/src/main/AndroidManifest.xml`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/DonDoneApplication.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`

## Docs
- `docs/execplans/active/2026-03-16-mobile-workproof-kakao-map.md`

## Shared
- 없음

# Contract Changes
- 외부 API/DB schema 변경 없음
- Android 빌드 입력값이 `NAVER_MAP_CLIENT_ID` 에서 `KAKAO_NATIVE_APP_KEY` 로 바뀜

# Security Notes
- 앱 키는 소스에 하드코딩하지 않음
- `Application` 초기화 한 곳에서만 키를 사용
- 실제 위치 권한은 이번 범위에서 요청하지 않음

# Maintainability Notes
- SDK 종속 초기화는 `Application` 으로 한정한다.
- 화면 모델은 SDK 타입이 아닌 좌표 `Double` 만 유지한다.
- Compose 화면은 지도 SDK 뷰를 래핑하되, lifecycle 호출을 helper로 고립한다.

# Implementation Steps
1. Gradle/Manifest/App init 을 Kakao 기준으로 바꾼다.
2. `WorkproofScreen.kt` 의 Naver SDK 타입과 카메라 API를 Kakao 타입으로 교체한다.
3. `MapView.start()` 와 `resume()/pause()` 를 Compose lifecycle 에 연결한다.
4. Kakao label API 로 근무지/현재 위치 표시를 추가한다.
5. 빌드 후 남는 SDK 시그니처 오류를 정리한다.

# Test Plan
- `:app:assembleDebug`
- `KAKAO_NATIVE_APP_KEY` 미입력 시 fallback UI 확인
- 키 입력 시 지도 렌더링 및 버튼 카메라 이동 확인

# Review Focus
- Kakao 앱 키가 하드코딩되지 않았는가
- `MapView.start/resume/pause` lifecycle 이 누락되지 않았는가
- Naver SDK 잔재가 남지 않았는가

# Worktree Split Decision
- Single lane

SDK 전환은 설정, 초기화, 화면 코드가 동시에 바뀌므로 단일 레인으로 처리한다.

# Commit Plan
- 1개 커밋 기본
  - `feat: 근무 탭 카카오 지도 연동`

# Open Questions
- 없음

# Assumptions
- 사용자는 KakaoMaps Android SDK v2의 무료 쿼터 범위 사용을 원한다.
- 로컬 빌드 환경에 `KAKAO_NATIVE_APP_KEY` 를 추가할 수 있다.
