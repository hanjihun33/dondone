---
name: commit-grouping
description: Use when DonDone changes are ready to be committed. Group the current diff into focused semantic commits that match the repository Git flow and commit convention in docs/GIT_FLOW.md, without using broad staging like git add dot.
---

# Commit Grouping

## Goal

Turn a finished change set into reviewable commits.

## Repository Convention

Follow `docs/GIT_FLOW.md` and `docs/GIT_GUIDE.md`.

- Message format: `<type>: <subject>`
- Use lowercase commit types.
- Prefer one intent per commit.
- Match the repository's existing style, which commonly uses concise Korean subjects.

Available commit types:

- `feat`: 기능 추가
- `fix`: 버그 수정
- `refactor`: 구조 개선, 동작 동일
- `docs`: 문서 수정
- `test`: 테스트 코드
- `style`: 포맷팅/스타일 변경
- `chore`: 빌드, 설정, 의존성 작업
- `env`: 환경 변수/실행 환경 관련 변경

## Rules

1. Never use `git add .`.
2. Group files by purpose, not by directory alone.
3. Prefer this commit order when it fits:
   - `feat`
   - `fix`
   - `test`
   - `docs`
   - `refactor`
   - `style`
   - `chore`
   - `env`
4. Keep each commit independently understandable and aligned with one user-visible or maintenance intent.
5. If unrelated changes are present, exclude them and call them out.
6. Do not create a style-only or cleanup-only commit unless it is independently meaningful and safe to review.
7. If the current diff mixes feature work with config, docs, or tests, separate them when the split keeps history clearer.

## Git Flow Notes

- Assume feature work normally happens on `feature/*` branches off `develop`.
- Do not propose merge commits or branch-sync commits unless the user explicitly asks for merge handling.
- If the current branch contains a mix of user work and unrelated carry-over changes, leave the carry-over changes out of the proposed commit groups.

## Subject Guidance

- Prefer concise subjects over long sentences.
- Prefer repository-consistent wording and domain terms from the current codebase and PRD.
- Avoid vague subjects like `수정`, `변경`, or `작업`.
- Name the actual scope when possible, for example:
  - `feat: WorkProof 제출 API 추가`
  - `fix: JWT 인증 예외 응답 형식 수정`
  - `docs: Wage Shield 실행 계획 문서 추가`
  - `chore: 모바일 mockup 정적 자산 ignore 규칙 정리`

## Output

Return:

- proposed commit groups
- files in each group
- commit message for each group
- rationale for each group
- any conflicts or files that should not be committed yet
