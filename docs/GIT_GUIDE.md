# DawnDone Git Guide

상세 규칙은 루트의 [`GIT_FLOW.md`](../GIT_FLOW.md)를 따릅니다.

## 빠른 요약
- 기준 브랜치: `main`, `develop`
- 기능 브랜치: `feature/*`
- 긴급 수정: `hotfix/*`
- `main` 직접 push 금지, PR/MR로만 병합
- 커밋 형식: `type: subject` (예: `feat: 로그인 API 추가`)

## 권장 습관
1. 기능 1개당 브랜치 1개
2. 기능 단위 커밋
3. PR에서 테스트 결과를 함께 공유
4. 비밀키/산출물 커밋 금지
