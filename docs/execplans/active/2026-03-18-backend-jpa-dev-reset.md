# 백엔드 JPA 개발 모드 복귀

## 목표
- Flyway 기반 스키마 관리 도입분을 되돌린다.
- 백엔드 엔티티/스키마 다이어트 리팩토링은 유지한다.
- 로컬 개발 기준을 다시 JPA `ddl-auto: update`로 맞춘다.

## 범위
- 포함
  - `build.gradle.kts`의 Flyway 의존성 제거
  - `application.yml`, `application-test.yml`의 Flyway 설정 제거 및 JPA 설정 복귀
  - `db/migration` baseline 파일 제거
  - 루트/백엔드 AGENTS의 migration-first 규칙 제거
  - `.agents/skills/db-migration-checklist` 제거
  - Flyway bootstrap execplan 제거
- 제외
  - `WageVerification`, `WageVerificationDraft`, `WageService` 리팩토링
  - `AdvanceRequest`, `ClaimPreparation`의 `updatedAt` 제거
  - `.private` 아래 로컬 참고 문서

## 영향 파일
- `apps/dondone-backend/build.gradle.kts`
- `apps/dondone-backend/src/main/resources/application.yml`
- `apps/dondone-backend/src/test/resources/application-test.yml`
- `apps/dondone-backend/src/main/resources/db/migration/V20260317222243__baseline.sql`
- `AGENTS.md`
- `apps/dondone-backend/AGENTS.md`
- `.agents/skills/db-migration-checklist/SKILL.md`
- `docs/execplans/active/2026-03-17-backend-flyway-bootstrap.md`

## 계약/운영 변경
- DB 생성 기준을 다시 JPA `ddl-auto: update`로 되돌린다.
- 테스트 기본 스키마 전략은 `create-drop`으로 복귀한다.
- 로컬 DB는 삭제 후 재생성 기준으로 본다.

## 검증 계획
- `./gradlew test`
- 가능하면 앱 기동 시 JPA가 빈 로컬 DB에 스키마를 생성하는지 확인

## 리뷰 포인트
- Flyway 관련 흔적이 코드/설정/문서에 남지 않았는지
- 엔티티 리팩토링만 유지되고 있는지
- 테스트 설정이 개발 의도와 맞는지
