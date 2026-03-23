# 모바일 송금 트래커 정체 재적용 메모

## 증상
- 첫 송금은 정상 완료됨
- 두 번째 송금부터는 실제 전송은 되었는데 모바일 화면이
  `네트워크에 전송했어요. 확인을 기다리는 중이에요.`
  에서 멈춤

## 원인
- 모바일 `DemoSessionViewModel`의 송금 상세 폴링이 너무 짧았음
  - 기존: `REMITTANCE_STATUS_POLL_ATTEMPTS = 12`
  - 1.5초 간격 기준 약 18초만 폴링
- 상세 조회 중 일시 예외가 한 번 나면 폴링이 바로 종료됐음
- 백엔드는 더 오래 `BROADCASTED -> CONFIRMED` 전이를 기다리므로,
  모바일만 `SUBMITTED`에 남는 경우가 발생함

## 적용 파일
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/session/DemoSessionViewModelTest.kt`

## 재적용 포인트

### 1. 폴링 시도 횟수 늘리기
`DemoSessionViewModel.kt`

```kotlin
private const val REMITTANCE_STATUS_POLL_DELAY_MS = 1500L
// Keep one polling cycle close to the backend receipt timeout window.
private const val REMITTANCE_STATUS_POLL_ATTEMPTS = 200
```

의도:
- 백엔드 receipt timeout window와 모바일 폴링 수명을 더 가깝게 맞춤

### 2. remittance remote refresh 후 active transfer가 있으면 폴링 재동기화
`updateRemittanceRemoteState(...)` 내부

```kotlin
applyRemittanceRemoteState(remoteState)
syncRemittanceStatusPolling(session, remoteState)
```

추가 함수:

```kotlin
private fun syncRemittanceStatusPolling(session: AuthSession, remoteState: RemittanceRemoteState) {
    val activeTransfer = remoteState.payload?.activeTransfer
    if (activeTransfer != null && !activeTransfer.isTerminalStatus()) {
        startRemittanceStatusPolling(session, activeTransfer.transferId)
        return
    }
    cancelRemittanceStatusPolling()
}
```

의도:
- silent refresh/load 이후에도 in-flight transfer가 남아 있으면 폴링을 다시 이어감

### 3. 폴링 중 일시 예외가 나도 종료하지 않기
`startRemittanceStatusPolling(...)` 내부

기존:

```kotlin
} catch (_: Exception) {
    return@launch
}
```

변경:

```kotlin
} catch (_: Exception) {
    return@repeat
}
```

의도:
- 일시적인 네트워크/백엔드 오류 한 번으로 트래커가 영구 정지하지 않게 함

### 4. 폴링 루프가 끝난 뒤 마지막 silent refresh 한 번 더 수행
`startRemittanceStatusPolling(...)` 마지막

```kotlin
refreshRemittanceRemoteStateSilently(session)
```

의도:
- 폴링 루프 중 terminal detail을 못 받았더라도 마지막 상태를 한 번 더 당겨옴

## 테스트 재적용 포인트
`DemoSessionViewModelTest.kt`

### 1. fake remittance repository가 상세 조회 예외를 흉내낼 수 있게 변경

기존:

```kotlin
transferDetailResults: List<RemittanceTransferDetailPayload> = emptyList()
```

변경:

```kotlin
transferDetailResults: List<Result<RemittanceTransferDetailPayload>> = emptyList()
```

그리고 `getTransferDetail(...)`에서:

```kotlin
val outcome = queuedTransferDetails.removeFirstOrNull()
    ?: error("getTransferDetail should not be called without a prepared result")
val detail = outcome.getOrThrow()
```

### 2. 기존 테스트들의 전달값도 `Result.success(...)`로 변경

예:

```kotlin
transferDetailResults = listOf(Result.success(confirmedTransferDetail))
```

### 3. 회귀 테스트 추가
테스트 이름:

```kotlin
fun `transfer tracker keeps polling after transient detail error and reflects confirmed second transfer`() = runTest
```

검증 포인트:
- 두 번째 송금 선택
- 첫 폴링: `BROADCASTED`
- 두 번째 폴링: `Result.failure(...)`
- 세 번째 폴링: `CONFIRMED`
- 최종적으로 모바일 `TransferStatus.CONFIRMED` 반영 확인

## 확인할 diff 요약
- `REMITTANCE_STATUS_POLL_ATTEMPTS`: `12 -> 200`
- `updateRemittanceRemoteState(...)` 끝에 `syncRemittanceStatusPolling(...)` 추가
- `catch (_: Exception)` 처리: `return@launch -> return@repeat`
- 폴링 루프 종료 후 `refreshRemittanceRemoteStateSilently(session)` 추가
- 테스트 fake repository를 `Result<...>` 기반으로 변경
- 두 번째 송금 + transient error 회귀 테스트 추가

## 참고
- 기존 상세 계획 문서:
  - `docs/execplans/active/2026-03-20-mobile-remittance-tracker-stuck.md`
