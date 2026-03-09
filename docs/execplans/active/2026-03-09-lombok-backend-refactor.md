## Source Inputs

- Repository `AGENTS.md`
- `.agents/skills/execplan-writer/SKILL.md`
- `.agents/skills/implement-checklist/SKILL.md`
- `.agents/skills/test-checklist/SKILL.md`
- Current backend Gradle config and classes with repeated constructor/getter boilerplate
- Existing auth/shared/workproof/wage/demo backend code

## Goal

`apps/dondone-backend`에 Lombok 의존성을 안전하게 도입하고, 생성자 주입과 단순 getter 보일러플레이트가 반복되는 클래스만 선택적으로 리팩토링한다.

## In Scope

- `build.gradle.kts`에 Lombok compileOnly/annotationProcessor 의존성 추가
- 생성자 주입만 담당하는 Spring 컴포넌트를 `@RequiredArgsConstructor`로 정리
- JPA 엔티티와 예외/enum의 반복 getter를 `@Getter`로 정리
- JPA 엔티티 기본 생성자를 Lombok 기반으로 유지 가능한 형태로 정리
- 기존 테스트가 Lombok 도입 이후에도 동일하게 통과하는지 확인

## Out of Scope

- API 응답 계약 변경
- 비즈니스 로직 변경
- DTO record를 일반 클래스로 바꾸는 작업
- 모바일/mockup 코드 변경
- Swagger/OpenAPI 문서 구조 변경
- 전체 백엔드 클래스에 대한 광범위한 Lombok 일괄 적용

## Affected Modules

### Backend

- `apps/dondone-backend/build.gradle.kts`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/demo/**`

### Mobile

- 변경 없음

### Docs

- 이 실행계획 문서

### Shared

- Lombok이 백엔드 공통 의존성으로 추가됨
- 생성자 주입/단순 getter 컨벤션이 일부 공통화됨

## Contract Changes

- 외부 API 요청/응답 계약 변경 없음
- DB 스키마 변경 없음
- 런타임 동작은 동일해야 하며, 코드 생성 방식만 Lombok 기반으로 일부 전환

## Security Notes

- `SecurityConfig`, `JwtAuthenticationFilter`는 생성자 주입 방식만 바뀌고 permitAll 목록이나 인증 흐름은 유지
- JWT 처리, 예외 응답, 필터 동작은 회귀 없이 유지되어야 함

## Maintainability Notes

- Lombok은 보일러플레이트 제거가 확실한 지점에만 적용하고, 생성 규칙이나 도메인 의미를 숨기지 않아야 함
- JPA 엔티티는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`와 명시적 생성 팩토리/생성자를 유지해 영속성 제약을 분명히 해야 함
- 이미 `record`로 충분한 DTO는 건드리지 않아 Lombok 사용 범위를 불필요하게 넓히지 않음

## Implementation Steps

1. Lombok 의존성을 Gradle에 추가한다.
2. 생성자 주입만 쓰는 컨트롤러/서비스/설정/부트스트랩/필터를 `@RequiredArgsConstructor`로 전환한다.
3. `User`, `WorkProof`, `WageDeposit`, `ApiException`, `ErrorCode`의 반복 getter를 `@Getter`로 전환한다.
4. JPA 엔티티 기본 생성자를 Lombok 기반 protected no-args 생성자로 정리한다.
5. import와 어노테이션 충돌이 없는지 정리한다.
6. `./gradlew test --no-daemon`로 회귀를 확인한다.

## Test Plan

- Lombok 전환으로 영향받는 auth/workproof/wage/demo 통합 테스트를 기존 스위트로 회귀 확인
- 보안 필터와 예외 응답이 테스트 경로에서 깨지지 않는지 간접 확인
- 실행 명령: `cd apps/dondone-backend && ./gradlew test --no-daemon`

## Review Focus

- 생성자 주입이 Lombok 전환 후에도 명확한지
- JPA 엔티티 기본 생성자 접근제어가 유지되는지
- enum/exception/entity getter 제거가 빌드와 런타임 동작에 영향을 주지 않는지
- Lombok 적용 범위가 과도하게 넓어지지 않았는지

## Worktree Split Decision

Single lane

이번 작업은 공통 Gradle 의존성과 shared/auth/workproof/wage/demo의 생성자 주입 패턴을 함께 바꾸므로, 병렬로 나누면 import 충돌과 공통 의존성 변경 충돌이 바로 발생한다. DTO 계약과 보안 필터도 같은 범위에 있어 단일 레인으로 처리한다.

## Commit Plan

- Commit 1: Lombok dependency and constructor injection refactor
- Commit 2: entity/exception getter cleanup and verification notes

## Open Questions

- Lombok 적용 범위를 backend 전역으로 확대할지, 현재처럼 선택적 도입으로 둘지는 팀 컨벤션 결정이 필요

## Assumptions

- 현재 실행 환경에서 annotation processing이 정상 동작한다
- 내부 클라이언트나 테스트는 런타임 동작이 아닌 코드 구조 변경만으로는 추가 수정이 거의 필요 없다
