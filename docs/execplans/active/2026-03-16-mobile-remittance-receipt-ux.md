# Source Inputs
- 사용자 요청: `docs/DonDone_PRD_v1.5.md` 기준 차지훈 2주차 역할을 실제 구현 순서대로 진행
- PRD 근거:
  - `13.5 2주차: 차액 감지 -> Proof Pack -> 송금/영수증 연결 + 근무 수정(W4)`
  - 차지훈 역할: `영수증 화면(Explorer 링크/tx hash/상태) + 공유 버튼`
- 기존 코드 탐색:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/demo/DemoSeedFactory.kt`

# Goal
- 송금 완료 화면에서는 영수증을 제거하고, 메뉴의 서비스 섹션에서 영수증 세션으로 접근하게 만든다.
- `tx hash`, Explorer 이동, 공유 버튼, 상태 요약을 메뉴 기반 영수증 세션에서 데모 데이터로 확인 가능하게 만든다.

# In Scope
- 메뉴 서비스 섹션에 영수증 진입 액션 추가
- 메뉴 영수증 세션용 UI 모델 추가
- 메뉴에서 영수증 상세 sheet/CTA 추가
- 송금 완료 tracker 화면에서는 영수증 카드 제거
- Sepolia 기준 Explorer URL 생성
- 공유/Explorer CTA 기본 동작 연결

# Out of Scope
- Proof Pack 생성 UX
- 백엔드 API/DTO 변경
- 실제 PDF 영수증 생성
- 영수증 문서 저장소 연동

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/menu/presentation/MenuUiModelTest.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModelTest.kt`

## Docs
- `docs/execplans/active/2026-03-16-mobile-remittance-receipt-ux.md`

## Shared
- 없음

# Contract Changes
- 외부 API/DB schema 변경 없음
- 메뉴 화면 모델에 영수증 세션 표시용 필드가 추가된다
- 송금 화면 모델에서 영수증 전용 표시 필드는 제거될 수 있다

# Security Notes
- 공유/Explorer CTA 는 데모용 공개 정보(tx hash)만 사용한다
- 민감 정보나 토큰은 다루지 않는다
- 외부 링크는 Sepolia Explorer 의 tx URL만 연다

# Maintainability Notes
- 영수증 세션은 `menu` feature 안에서 소유해 `remittance` presentation 모델에 대한 역참조를 만들지 않는다
- tracker 화면은 완료 요약과 종료 동작만 유지해 책임을 줄인다
- Explorer URL 포맷과 tx hash 축약 로직은 한 화면 모델에서만 소유되도록 정리해 중복 책임을 키우지 않는다

# Implementation Steps
1. `MenuUiModel.kt` 에 영수증 세션용 모델과 `DemoState` 매핑을 추가한다
2. `MenuScreen.kt` 서비스 섹션에 영수증 액션과 상세 sheet 를 추가한다
3. 메뉴 영수증 sheet 에 상태, tx hash, Explorer, 공유 CTA 를 연결한다
4. `TransferScreen.kt` tracker 화면에서 영수증 카드 노출을 제거한다
5. 관련 테스트를 메뉴 모델 기준으로 추가하고, 송금 화면 테스트는 새 동작에 맞게 정리한다

# Test Plan
- `:app:assembleDebug`
- 메뉴 서비스 섹션에서 영수증 액션이 보이는지 확인
- 영수증 sheet 가 `SUBMITTED`/`CONFIRMED` 상태에 맞는 문구와 tx hash 를 보여주는지 확인
- tracker 화면에서 영수증 카드가 더 이상 보이지 않는지 확인
- Explorer 버튼이 Sepolia tx 링크를 여는지 확인
- 공유 버튼이 tx hash 포함 텍스트를 공유하는지 확인

# Review Focus
- 영수증 접근 위치가 메뉴 서비스 섹션으로 일관되게 이동했는가
- tracker 화면이 영수증 없이도 완료 흐름을 정상 종료하는가
- `tx hash`, 상태, Explorer, 공유가 메뉴 영수증 세션에서 PRD 요구와 맞는가
- 메뉴 문서/서비스 섹션 기존 동작이 회귀하지 않는가

# Worktree Split Decision
- Single lane

공유 상태 모델과 tracker 화면이 함께 바뀌므로 단일 레인으로 처리한다.

# Commit Plan
- `feat: 송금 영수증 상세 UX 추가`

# Open Questions
- 없음

# Assumptions
- 테스트넷 Explorer 는 `Ethereum Sepolia` 기준 URL 을 사용한다
- 영수증 CTA 는 데모 단계에서 Intent 기반 기본 동작만 제공해도 충분하다
