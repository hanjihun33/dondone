# Git Flow & Commit Convention (DawnDone)

이 문서는 `S14P11C205/GIT_FLOW.md`를 참고해 DawnDone(`S14P21C202`) 프로젝트에 맞게 정리한 Git 운영 가이드입니다.

## 1. 브랜치 전략

### 브랜치 구조

```text
main            : 배포 기준 브랜치
develop         : 통합 개발 브랜치
feature/*       : 기능 개발 브랜치
hotfix/*        : 긴급 수정 브랜치
```

### 브랜치 규칙

- `main`
  - 직접 push 금지
  - PR/MR로만 병합
- `develop`
  - 기능 브랜치 통합 지점
  - 항상 빌드 가능한 상태 유지
- `feature/*`
  - `develop`에서 분기
  - 하나의 사용자 스토리(또는 작업 단위) 중심으로 작업
- `hotfix/*`
  - `main`에서 분기 후 긴급 수정
  - 수정 후 `main`, `develop` 모두 반영

### 브랜치 네이밍

- `feature/auth-login`
- `feature/workproof-record`
- `feature/wage-anomaly-check`
- `hotfix/jwt-expiration-bug`

## 2. 기본 개발 플로우

### 2.1 기능 시작

```bash
git checkout develop
git pull origin develop
git checkout -b feature/<feature-name>
```

### 2.2 작업/커밋

```bash
git add .
git commit -m "feat: 로그인 API 추가"
git push origin feature/<feature-name>
```

### 2.3 통합

```bash
git checkout develop
git pull origin develop
git merge --no-ff feature/<feature-name>
git push origin develop
```

### 2.4 배포 반영

- `develop -> main` PR/MR 생성
- 코드 리뷰 + 빌드 확인 후 병합

## 3. 커밋 컨벤션

### 커밋 원칙

- 하나의 커밋은 하나의 의도를 가진다.
- 기능/리팩터/문서 변경을 가능한 분리한다.
- “수정 범위가 큰 커밋”보다 “의미 단위 커밋”을 우선한다.

### 메시지 형식

```text
<type>: <subject>
```

### type 목록

- `feat`: 기능 추가
- `fix`: 버그 수정
- `refactor`: 구조 개선(동작 동일)
- `docs`: 문서 수정
- `test`: 테스트 코드
- `style`: 포맷팅/스타일 변경
- `chore`: 빌드/설정/의존성 작업
- `env`: 환경 변수/실행 환경 관련 변경

### 예시

- `feat: 로그인 API 추가`
- `fix: Bearer 토큰 검증 오류 수정`
- `docs: 백엔드 실행 가이드 문서 추가`
- `chore: 백엔드 gitignore 정리`

## 4. PR 체크리스트

PR 본문에 아래를 포함합니다.

```markdown
## 변경 사항
- 주요 변경 1
- 주요 변경 2

## 테스트
- [ ] 로컬 실행 확인
- [ ] API 동작 확인
- [ ] 회귀 영향 확인

## 관련 문서
- PRD / API 문서 링크

## 비고
- 환경 변수/마이그레이션 필요 여부
```

## 5. 충돌 해결 규칙

- 충돌 해결 후 반드시 빌드/테스트 재실행
- 충돌 해결 커밋 메시지를 명확히 작성
  - 예: `fix: develop 브랜치 병합 충돌 해결`
- 임시 코드(TODO, 디버그 로그) 제거 후 push

## 6. 금지 사항

- `main` 직접 push
- 무의미한 대량 포맷 변경 단독 커밋
- 비밀키/인증정보 커밋
- 생성 산출물(`node_modules`, `build`, `dist`) 커밋
