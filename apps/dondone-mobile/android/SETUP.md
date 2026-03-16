# Android 로컬 설정

## 카카오 지도 키 설정

이 프로젝트에서 `local.properties` 는 Git에 포함되지 않습니다.

1. `local.properties.example` 파일을 `local.properties` 로 복사합니다.
2. 각자 로컬 환경의 카카오 네이티브 앱 키를 입력합니다.

```properties
KAKAO_NATIVE_APP_KEY=YOUR_KAKAO_NATIVE_APP_KEY
```

앱은 아래 순서로 키를 읽습니다.

1. `local.properties`
2. `KAKAO_NATIVE_APP_KEY` 환경변수

## 참고

- `local.properties` 는 커밋하지 않습니다.
- 팀에서 공유한 카카오 네이티브 앱 키를 사용합니다.
- 카카오 지도는 `x86/x86_64` 에뮬레이터에서 런타임 제약이 있을 수 있습니다.
