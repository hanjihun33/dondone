## Scope

- `apps/dondone-backend`에 Lombok 의존성 추가
- 생성자 주입 기반 Spring 컴포넌트와 단순 getter 보일러플레이트가 있는 클래스 일부를 Lombok으로 정리

## What Changed

- `build.gradle.kts`에 `compileOnly`, `annotationProcessor`, `testCompileOnly`, `testAnnotationProcessor`로 Lombok 추가
- 컨트롤러/서비스/설정/필터/부트스트랩은 `@RequiredArgsConstructor`로 생성자 주입 정리
- `User`, `WorkProof`, `WageDeposit`, `ApiException`, `ErrorCode`는 `@Getter` 기반으로 반복 getter 제거
- JPA 엔티티 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 유지

## Verification

- 실행 명령: `GRADLE_USER_HOME=/tmp/gradle-dondone ./gradlew test --no-daemon`
- 결과: 성공

## Findings

- 기본 `~/.gradle` 캐시에서 lock file 관련 `java.io.IOException: Input/output error`가 발생해, 임시 `GRADLE_USER_HOME`으로 우회 실행했다.
- Lombok 도입으로 API 계약, 보안 설정, 비즈니스 로직은 변경되지 않았다.

## Residual Risks

- 팀에서 Lombok 사용 범위를 더 넓히면 스타일 일관성을 다시 논의해야 한다.
- 로컬 기본 Gradle 캐시 상태가 계속 불안정하면 이후 빌드도 동일한 우회가 필요할 수 있다.
