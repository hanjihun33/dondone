# Android 로컬 설정

이 프로젝트에서 `local.properties` 는 Git에 포함되지 않습니다.

## 1. local.properties 준비

1. `local.properties.example` 파일을 `local.properties` 로 복사합니다.
2. 각자 로컬 환경 값을 입력합니다.

예시:

```properties
KAKAO_NATIVE_APP_KEY=YOUR_KAKAO_NATIVE_APP_KEY
DONDONE_API_BASE_URL=http://10.0.2.2:8080
```

## 2. 카카오 지도 키 설정

앱은 아래 순서로 카카오 키를 읽습니다.

1. `local.properties`
2. `KAKAO_NATIVE_APP_KEY` 환경변수

## 3. 백엔드 기본 주소 설정

모바일 앱의 백엔드 기본 주소는 아래 순서로 읽습니다.

1. `local.properties` 의 `DONDONE_API_BASE_URL`
2. 환경변수 `DONDONE_API_BASE_URL`
3. 기본값 `http://10.0.2.2:8080`

### 자주 쓰는 예시

로컬 백엔드:

```properties
DONDONE_API_BASE_URL=http://10.0.2.2:8080
```

운영 백엔드:

```properties
DONDONE_API_BASE_URL=https://dondone.duckdns.org
```

### 10.0.2.2 의미

Android 에뮬레이터에서 `10.0.2.2` 는 개발자 PC의 `localhost` 를 가리킵니다.
즉 PC에서 백엔드를 `8080` 포트로 띄웠다면 에뮬레이터에서는 `http://10.0.2.2:8080` 으로 접근해야 합니다.

## 참고

- `local.properties` 는 커밋하지 않습니다.
- 팀에서 공유한 카카오 네이티브 앱 키를 사용합니다.
- 카카오 지도는 `x86/x86_64` 에뮬레이터에서 런타임 제약이 있을 수 있습니다.
- 운영 백엔드 연결 시 실제 운영 데이터에 영향을 줄 수 있으니 주의합니다.
