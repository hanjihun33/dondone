# 2026-03-23 Workproof Clock-out Review Alignment

## Scope
- 모바일 앱에서 `반경 밖 퇴근`을 허용한다.
- `반경 밖 출근 차단`은 유지한다.
- 백엔드의 `clock-out outside allowed radius -> NEEDS_REVIEW` 정책과 모바일 UI를 정렬한다.

## In Scope
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/*`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/*`

## Out of Scope
- 백엔드 WorkProof 정책 변경
- NEEDS_REVIEW 상세/처리 플로우 변경
- 반경 밖 퇴근 전용 안내 문구/배지 추가

## Contract / Behavior
- 출근 버튼: 기존처럼 반경 밖이면 비활성화
- 퇴근 버튼: 반경 밖이어도 활성화 가능
- 퇴근 요청은 기존 API 그대로 사용하고, 서버가 `NEEDS_REVIEW`를 결정한다

## Files Expected
- `HomeScreen.kt`
- `HomeUiModel.kt`
- `WorkproofScreen.kt`
- `WorkproofUiModel.kt`

## Verification
- 모바일 compile check
- 수동 확인
  - 반경 밖 출근 버튼 비활성화 유지
  - 반경 밖 퇴근 버튼 활성화
  - 퇴근 후 백엔드 기록이 검토 필요로 반영되는지 별도 수동 확인

## Review Focus
- 홈/근무 화면 정책 일관성
- 출근 차단 조건이 의도치 않게 완화되지 않았는지
- 퇴근 버튼만 선택적으로 완화되었는지
