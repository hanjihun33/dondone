# Source Inputs
- 사용자 요구: 근무 탭 지도를 실제 `Naver Map API`로 연결
- 기존 지도 UI 작업물: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- 현재 Android 설정:
  - 저장소 설정 `apps/dondone-mobile/android/settings.gradle.kts`
  - 앱 Gradle `apps/dondone-mobile/android/app/build.gradle.kts`
  - Manifest `apps/dondone-mobile/android/app/src/main/AndroidManifest.xml`
  - 데모 데이터 `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
  - 데모 시드 `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/demo/DemoSeedFactory.kt`
- 공식 문서:
  - NAVER Map Android SDK Get Started: 저장소 `https://repository.map.naver.com/archive/maven`, 의존성 `com.naver.maps:map-sdk:3.23.1`, `com.naver.maps.map.NCP_KEY_ID` 설정
  - NAVER Map Android SDK Map Object / MapView: `MapView` 사용 시 lifecycle 호출 필요

# Goal
- 근무 탭 `위치` 섹션을 정적 모사 UI에서 실제 NAVER Map SDK 기반 지도로 교체한다.
- 키는 하드코딩하지 않고 Gradle/Manifest placeholder로 주입한다.
- 데모 앱 범위에서 근무지/현재 위치 마커와 재센터링 버튼이 동작하도록 만든다.

# In Scope
- Android Gradle 저장소/의존성 추가
- Manifest의 NAVER Map 키 placeholder 추가
- 데모용 좌표 필드 추가
- `WorkproofScreen.kt`의 지도 섹션을 실제 `MapView` + `AndroidView` 기반으로 교체
- 키 누락/지도 준비 전 상태를 최소한으로 표시

# Out of Scope
- 실제 GPS 권한 요청 및 기기 현재 위치 추적
- 백엔드 API/DB/DTO 변경
- 실시간 지오코딩
- 체크인/체크아웃 시 위치 스냅샷 저장 로직 추가

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/settings.gradle.kts`
- `apps/dondone-mobile/android/app/build.gradle.kts`
- `apps/dondone-mobile/android/app/src/main/AndroidManifest.xml`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/demo/DemoSeedFactory.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`

## Docs
- `docs/execplans/active/2026-03-16-mobile-workproof-naver-map.md`

## Shared
- 없음

# Contract Changes
- 외부 API/DB schema 변경 없음
- Android 내부 `WorkproofData` 또는 `WorkproofUiModel`에 데모 지도 좌표 필드가 추가됨
- `BuildConfig` 또는 Manifest placeholder에 NAVER Map client id 전달 값이 추가됨

# Security Notes
- NAVER Map client id는 코드에 하드코딩하지 않고 `local.properties` 또는 환경변수에서 읽어 Manifest placeholder/BuildConfig로만 주입한다.
- `AndroidManifest.xml`에 노출되는 값은 배포 앱의 일반적인 SDK 키 설정 범위이며, 저장소에는 실제 키를 커밋하지 않는다.
- 이번 범위에서는 위치 권한을 요청하지 않아 민감 권한 surface를 늘리지 않는다.

# Maintainability Notes
- Compose 전용 래퍼 라이브러리를 새로 들이지 않고 공식 SDK `MapView`를 `AndroidView`로 감싸 최소한의 브리지로 유지한다.
- `MapView` lifecycle 관리는 전용 컴포저블로 감싸 `WorkproofScreen.kt` 본문과 분리한다.
- 데모 좌표는 도메인 모델에 명시적으로 추가해 화면 코드에 매직 넘버를 흩뿌리지 않는다.

# Implementation Steps
1. 저장소/의존성/Manifest 키 주입 구조를 추가한다.
2. 데모 데이터에 근무지 좌표와 데모 현재 위치 좌표를 추가한다.
3. `WorkproofUiModel.kt`에 지도 표시용 좌표/라벨 상태를 만든다.
4. `WorkproofScreen.kt`에 `MapView` 기반 컴포저블과 lifecycle 처리 로직을 추가한다.
5. 지도 로딩 전/키 누락 시 fallback UI를 제공한다.
6. 버튼으로 카메라를 데모 현재 위치로 이동시키고, 근무지/현재 위치 마커를 구분한다.

# Test Plan
- `:app:assembleDebug`
- 키 미설정 상태에서 fallback UI 노출 확인
- 키 설정 상태에서 지도 렌더링, 근무지/현재 위치 마커, 버튼 재센터링 확인

# Review Focus
- 키 주입 경로가 안전하고 저장소에 실제 키가 남지 않는가
- `MapView` lifecycle 누락으로 메모리/크래시 위험이 없는가
- 데모 좌표 추가가 다른 화면 계약을 불필요하게 깨지 않는가

# Worktree Split Decision
- Single lane

Gradle, Manifest, 데모 모델, Compose 화면이 함께 바뀌며 지도 SDK 연동 실패 시 원인 분리가 어렵다. 단일 레인에서 설정과 UI를 함께 맞추는 편이 안전하다.

# Commit Plan
- 1개 커밋 기본
  - `feat: 근무 탭에 naver map 연동`

# Open Questions
- 없음

# Assumptions
- 이번 요청의 `실제 연동`은 Android 데모 앱에서 NAVER Map SDK가 실제 렌더링되는 수준을 의미한다.
- `현재 내 위치`는 실제 GPS가 아니라 데모 좌표를 사용해 카메라를 이동시키는 범위로 제한한다.
- 사용자 로컬 환경에 `NAVER_MAP_CLIENT_ID` 또는 `local.properties` 설정을 추가할 수 있다.
