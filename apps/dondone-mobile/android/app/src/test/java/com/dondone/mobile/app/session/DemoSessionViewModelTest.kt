package com.dondone.mobile.app.session

import com.dondone.mobile.data.advance.AdvanceRemoteState
import com.dondone.mobile.data.advance.AdvanceRepository
import com.dondone.mobile.data.advance.AdvanceCreateResult
import com.dondone.mobile.data.advance.AdvanceRequestDetailPayload
import com.dondone.mobile.data.auth.AuthRepository
import com.dondone.mobile.data.auth.AuthSession
import com.dondone.mobile.data.remittance.RemittanceCreateTransferPayload
import com.dondone.mobile.data.remittance.RemittanceRecipientPayload
import com.dondone.mobile.data.remittance.RemittanceRecipientSearchPayload
import com.dondone.mobile.data.remittance.RemittanceRemotePayload
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.data.remittance.RemittanceRepository
import com.dondone.mobile.data.remittance.RemittanceTransferDetailPayload
import com.dondone.mobile.data.remittance.RemittanceTransferPrecheckPayload
import com.dondone.mobile.data.remittance.RemittanceTransferSummaryPayload
import com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload
import com.dondone.mobile.data.remittance.RemittanceWalletPayload
import com.dondone.mobile.data.remittance.RemittanceCompletionNoticeStore
import com.dondone.mobile.data.vault.VaultCreateTransactionPayload
import com.dondone.mobile.data.vault.VaultInterestPreviewPayload
import com.dondone.mobile.data.vault.VaultRemotePayload
import com.dondone.mobile.data.vault.VaultRemoteState
import com.dondone.mobile.data.vault.VaultRepository
import com.dondone.mobile.data.vault.VaultSummaryPayload
import com.dondone.mobile.data.vault.VaultTransactionDetailPayload
import com.dondone.mobile.data.wage.WageDepositRecordPayload
import com.dondone.mobile.data.wage.WageRemoteState
import com.dondone.mobile.data.wage.WageRepository
import com.dondone.mobile.data.wage.WageVerificationCreatedPayload
import com.dondone.mobile.data.wage.WageVerificationDetailPayload
import com.dondone.mobile.data.workproof.WorkproofRemotePayload
import com.dondone.mobile.data.workproof.WorkproofRemoteState
import com.dondone.mobile.data.workproof.WorkproofRepository
import com.dondone.mobile.data.workproof.WorkproofCorrectionRequestMutation
import com.dondone.mobile.data.workproof.WorkproofCorrectionSubmitResult
import com.dondone.mobile.data.workproof.WorkproofWorkplacePayload
import com.dondone.mobile.core.location.CurrentLocationProvider
import com.dondone.mobile.core.location.CurrentLocationResult
import com.dondone.mobile.core.location.CurrentLocationSnapshot
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.domain.model.WorkproofData
import com.dondone.mobile.feature.home.presentation.toHomeUiModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private data class CreateRecipientCall(
        val accessToken: String,
        val alias: String,
        val relation: String,
        val walletAddress: String,
        val targetUserId: Long?
    )

    private data class UpdateRecipientCall(
        val accessToken: String,
        val recipientId: String,
        val alias: String,
        val relation: String,
        val walletAddress: String
    )

    @Test
    fun `restore session loads authenticated advance state`() = runTest {
        val session = testSession()
        val authRepository = FakeAuthRepository(restoredSession = session)
        val advanceRepository = FakeAdvanceRepository()

        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = advanceRepository
        )

        advanceUntilIdle()

        assertTrue(viewModel.authUiState.value.isAuthenticated)
        assertEquals(session.email, viewModel.authUiState.value.session?.email)
        assertEquals(listOf(session.accessToken), advanceRepository.loadedTokens)
        assertTrue(viewModel.advanceRemoteState.value.isAuthenticated)
    }

    @Test
    fun `login success stores session and requests advance with token`() = runTest {
        val session = testSession(accessToken = "fresh-token", email = "worker@example.com")
        val authRepository = FakeAuthRepository(loginSession = session)
        val advanceRepository = FakeAdvanceRepository()
        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = advanceRepository
        )

        advanceUntilIdle()
        viewModel.login("worker@example.com", "secret")
        advanceUntilIdle()

        assertEquals(1, authRepository.loginCalls.size)
        assertEquals("worker@example.com" to "secret", authRepository.loginCalls.single())
        assertTrue(viewModel.authUiState.value.isAuthenticated)
        assertEquals(session.accessToken, viewModel.authUiState.value.session?.accessToken)
        assertEquals(listOf(session.accessToken), advanceRepository.loadedTokens)
    }

    @Test
    fun `signup success stores session and requests advance with token`() = runTest {
        val session = testSession(accessToken = "signup-token", email = "new@example.com")
        val authRepository = FakeAuthRepository(signupSession = session)
        val advanceRepository = FakeAdvanceRepository()
        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = advanceRepository
        )

        advanceUntilIdle()
        viewModel.signup("새 사용자", "new@example.com", "password123", "01012345678")
        advanceUntilIdle()

        assertEquals(1, authRepository.signupCalls.size)
        assertEquals(
            SignupCall("새 사용자", "new@example.com", "password123", "01012345678"),
            authRepository.signupCalls.single()
        )
        assertTrue(viewModel.authUiState.value.isAuthenticated)
        assertEquals(session.accessToken, viewModel.authUiState.value.session?.accessToken)
        assertEquals(listOf(session.accessToken), advanceRepository.loadedTokens)
    }

    @Test
    fun `update profile updates authenticated session`() = runTest {
        val session = testSession(phoneNumber = "01012345678")
        val updatedSession = session.copy(name = "수정 사용자", phoneNumber = "01099998888")
        val authRepository = FakeAuthRepository(
            restoredSession = session,
            updateProfileSession = updatedSession
        )
        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = FakeAdvanceRepository()
        )

        advanceUntilIdle()
        viewModel.updateProfile("수정 사용자", "010-9999-8888")
        advanceUntilIdle()

        assertEquals(listOf("수정 사용자" to "01099998888"), authRepository.updateProfileCalls)
        assertEquals("수정 사용자", viewModel.authUiState.value.session?.name)
        assertEquals("01099998888", viewModel.authUiState.value.session?.phoneNumber)
        assertEquals("내 정보를 수정했어요.", viewModel.profileUpdateUiState.value.message)
    }

    @Test
    fun `update profile expires session on unauthorized`() = runTest {
        val session = testSession(phoneNumber = "01012345678")
        val authRepository = FakeAuthRepository(
            restoredSession = session,
            updateProfileError = com.dondone.mobile.data.auth.AuthUnauthorizedException()
        )
        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = FakeAdvanceRepository()
        )

        advanceUntilIdle()
        viewModel.updateProfile("수정 사용자", "010-9999-8888")
        advanceUntilIdle()

        assertFalse(viewModel.authUiState.value.isAuthenticated)
        assertEquals("세션이 만료되어 다시 로그인해 주세요.", viewModel.profileUpdateUiState.value.message)
        assertTrue(viewModel.profileUpdateUiState.value.isError)
    }

    @Test
    fun `redeem worker registration code refreshes authenticated remote state`() = runTest {
        val session = testSession()
        val updatedSession = session.copy(
            companyCode = "DN-SEOUL-2914",
            companyName = "돈던 물류",
            workplaceName = "서울 허브"
        )
        val authRepository = FakeAuthRepository(
            restoredSession = session,
            redeemWorkerRegistrationCodeSession = updatedSession
        )
        val advanceRepository = FakeAdvanceRepository()
        val workproofRepository = FakeWorkproofRepository(workplaceName = "서울 허브")
        val remittanceRepository = FakeRemittanceRepository(
            result = RemittanceRemoteState.content(
                RemittanceRemotePayload(
                    wallet = RemittanceWalletPayload(
                        walletAddress = "0x1111111111111111111111111111111111111111",
                        fundingStatus = "FUNDED",
                        fundingFailureReason = null,
                        fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                        createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                    ),
                    balance = RemittanceWalletBalancePayload(
                        walletAddress = "0x1111111111111111111111111111111111111111",
                        assetSymbol = "dUSDC",
                        assetDecimals = 6,
                        tokenBalanceAtomic = "128500000",
                        nativeBalanceWei = "10000000000000000"
                    ),
                    recipients = emptyList(),
                    transfers = emptyList(),
                    activeTransfer = null
                )
            )
        )
        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = advanceRepository,
            workproofRepository = workproofRepository,
            remittanceRepository = remittanceRepository
        )

        advanceUntilIdle()
        advanceRepository.loadedTokens.clear()
        workproofRepository.loadedTokens.clear()

        viewModel.redeemWorkerRegistrationCode("WORKER-AB12-CD34")
        advanceUntilIdle()

        assertEquals(listOf(session.accessToken), advanceRepository.loadedTokens)
        assertEquals(listOf(session.accessToken), workproofRepository.loadedTokens)
        assertEquals("서울 허브", viewModel.authUiState.value.session?.workplaceName)
    }

    @Test
    fun `logout clears authenticated state and remote advance state`() = runTest {
        val session = testSession()
        val authRepository = FakeAuthRepository(restoredSession = session)
        val advanceRepository = FakeAdvanceRepository()
        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = advanceRepository
        )

        advanceUntilIdle()
        viewModel.logout()
        advanceUntilIdle()

        assertFalse(viewModel.authUiState.value.isAuthenticated)
        assertNull(viewModel.authUiState.value.session)
        assertEquals("로그인 후 실연동 데이터를 불러옵니다.", viewModel.advanceRemoteState.value.errorMessage)
        assertTrue(authRepository.logoutCalled)
    }

    @Test
    fun `refresh current location updates workproof coordinates`() = runTest {
        val locationProvider = FakeCurrentLocationProvider(
            result = CurrentLocationResult.Success(CurrentLocationSnapshot(35.1234, 128.5678))
        )
        val viewModel = DemoSessionViewModel(
            advanceRepository = FakeAdvanceRepository(),
            authRepository = FakeAuthRepository(),
            currentLocationProvider = locationProvider
        )

        advanceUntilIdle()
        viewModel.refreshWorkproofCurrentLocation()
        advanceUntilIdle()

        assertEquals(35.1234, viewModel.uiState.value.workproof.currentLatitude, 0.0)
        assertEquals(128.5678, viewModel.uiState.value.workproof.currentLongitude, 0.0)
        assertEquals(WorkproofCurrentLocationStatus.READY, viewModel.workproofCurrentLocationUiState.value.status)
        assertEquals(1, locationProvider.fetchCount)
    }

    @Test
    fun `refresh current location reuses in-flight fetch`() = runTest {
        val locationProvider = FakeCurrentLocationProvider(
            result = CurrentLocationResult.Success(CurrentLocationSnapshot(35.1234, 128.5678)),
            fetchDelayMillis = 1_000L
        )
        val viewModel = DemoSessionViewModel(
            advanceRepository = FakeAdvanceRepository(),
            authRepository = FakeAuthRepository(),
            currentLocationProvider = locationProvider
        )

        advanceUntilIdle()
        viewModel.refreshWorkproofCurrentLocation()
        viewModel.refreshWorkproofCurrentLocation()
        advanceUntilIdle()

        assertEquals(1, locationProvider.fetchCount)
    }

    @Test
    fun `clock in refreshes current location before remote request`() = runTest {
        val session = testSession()
        val locationProvider = FakeCurrentLocationProvider(
            result = CurrentLocationResult.Success(CurrentLocationSnapshot(35.2031, 126.8083))
        )
        val workproofRepository = FakeWorkproofRepository()
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = workproofRepository,
            currentLocationProvider = locationProvider
        )

        advanceUntilIdle()
        workproofRepository.clockInRequests.clear()

        viewModel.clockIn()
        advanceUntilIdle()

        val submittedWorkproof = requireNotNull(workproofRepository.clockInRequests.singleOrNull())
        assertEquals(35.2031, submittedWorkproof.currentLatitude, 0.0)
        assertEquals(126.8083, submittedWorkproof.currentLongitude, 0.0)
        assertEquals(WorkproofCurrentLocationStatus.READY, viewModel.workproofCurrentLocationUiState.value.status)
    }

    @Test
    fun `clock in joins in-flight current location refresh`() = runTest {
        val session = testSession()
        val locationProvider = FakeCurrentLocationProvider(
            result = CurrentLocationResult.Success(CurrentLocationSnapshot(35.2031, 126.8083)),
            fetchDelayMillis = 1_000L
        )
        val workproofRepository = FakeWorkproofRepository()
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = workproofRepository,
            currentLocationProvider = locationProvider
        )

        advanceUntilIdle()
        workproofRepository.clockInRequests.clear()
        viewModel.refreshWorkproofCurrentLocation()
        viewModel.clockIn()
        advanceUntilIdle()

        assertEquals(1, locationProvider.fetchCount)
        val submittedWorkproof = requireNotNull(workproofRepository.clockInRequests.singleOrNull())
        assertEquals(35.2031, submittedWorkproof.currentLatitude, 0.0)
        assertEquals(126.8083, submittedWorkproof.currentLongitude, 0.0)
    }

    @Test
    fun `refresh current location aligns last known accuracy with policy radius`() = runTest {
        val session = testSession()
        val locationProvider = FakeCurrentLocationProvider(
            result = CurrentLocationResult.Success(CurrentLocationSnapshot(35.2031, 126.8083))
        )
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(allowedRadiusMeters = 1_500),
            currentLocationProvider = locationProvider
        )

        advanceUntilIdle()
        viewModel.refreshWorkproofCurrentLocation()
        advanceUntilIdle()

        assertEquals(1_000f, locationProvider.requestedMaxLastKnownAccuracyMeters ?: -1f, 0f)
    }

    @Test
    fun `clock in stops when current location permission is missing`() = runTest {
        val session = testSession()
        val locationProvider = FakeCurrentLocationProvider(
            result = CurrentLocationResult.PermissionRequired
        )
        val workproofRepository = FakeWorkproofRepository()
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = workproofRepository,
            currentLocationProvider = locationProvider
        )

        advanceUntilIdle()
        workproofRepository.clockInRequests.clear()

        viewModel.clockIn()
        advanceUntilIdle()

        assertTrue(workproofRepository.clockInRequests.isEmpty())
        assertEquals(
            "위치 권한을 허용하면 현재 위치를 확인할 수 있어요.",
            viewModel.workproofActionUiState.value.message
        )
        assertTrue(viewModel.workproofActionUiState.value.isError)
        assertEquals(
            WorkproofCurrentLocationStatus.PERMISSION_REQUIRED,
            viewModel.workproofCurrentLocationUiState.value.status
        )
    }

    @Test
    fun `unauthenticated remote advance response clears restored session`() = runTest {
        val session = testSession()
        val authRepository = FakeAuthRepository(restoredSession = session)
        val advanceRepository = FakeAdvanceRepository(
            result = AdvanceRemoteState.unauthenticated("세션이 만료되어 다시 로그인해 주세요.")
        )
        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = advanceRepository
        )

        advanceUntilIdle()

        assertFalse(viewModel.authUiState.value.isAuthenticated)
        assertEquals("세션이 만료되어 다시 로그인해 주세요.", viewModel.authUiState.value.errorMessage)
        assertTrue(authRepository.logoutCalled)
    }

    @Test
    fun `request advance submits selected amount and refreshes remote state`() = runTest {
        val session = testSession()
        val authRepository = FakeAuthRepository(restoredSession = session)
        val remoteState = AdvanceRemoteState.content(
            workplaceName = "DonDone Cafe",
            eligibility = com.dondone.mobile.data.advance.AdvanceEligibilityPayload(
                workplaceId = 1L,
                assetSymbol = "USDC",
                assetDecimals = 6,
                exchangeRateSnapshot = BigDecimal("1350"),
                availableAmountAtomic = 180_000L * 1_000_000L,
                availableDisplayKrwAmount = 180_000L,
                maxCapAmountAtomic = 500_000L * 1_000_000L,
                maxCapDisplayKrwAmount = 500_000L,
                currentTierName = "T2",
                repaymentTier = "T2",
                blockReasonCodes = emptyList(),
                noticeReasonCodes = emptyList(),
                estimatedFeeAmountAtomic = 0L,
                estimatedFeeDisplayKrwAmount = 0L,
                estimatedRepaymentDate = "2026-03-25",
                disclaimer = "데모 시뮬레이션",
                needsReviewRecordCount = 0
            ),
            requests = emptyList()
        )
        val advanceRepository = FakeAdvanceRepository(result = remoteState)
        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = advanceRepository
        )

        advanceUntilIdle()
        viewModel.selectAdvanceAmount(100_000)
        viewModel.requestAdvance()
        advanceUntilIdle()

        assertEquals(listOf(session.accessToken, session.accessToken), advanceRepository.loadedTokens)
        assertEquals(
            listOf(Triple(session.accessToken, 1L, 100_000L * 1_000_000L)),
            advanceRepository.createCalls
        )
        assertEquals("미리받기 신청이 반영되었어요. APPROVED · 100000원", viewModel.advanceRequestUiState.value.message)
        assertFalse(viewModel.advanceRequestUiState.value.isError)
        assertEquals(1L, viewModel.advanceRequestDetailUiState.value.detail?.requestId)
        assertTrue(viewModel.advanceRemoteState.value.requests.any { it.requestId == 1L })
    }

    @Test
    fun `request advance shows minimum unit message when remaining amount is below 1 asset`() = runTest {
        val session = testSession()
        val authRepository = FakeAuthRepository(restoredSession = session)
        val remoteState = AdvanceRemoteState.content(
            workplaceName = "DonDone Cafe",
            eligibility = com.dondone.mobile.data.advance.AdvanceEligibilityPayload(
                workplaceId = 1L,
                assetSymbol = "dUSDC",
                assetDecimals = 6,
                exchangeRateSnapshot = BigDecimal("1450"),
                availableAmountAtomic = 480_000L,
                availableDisplayKrwAmount = 700L,
                maxCapAmountAtomic = 34_000_000L,
                maxCapDisplayKrwAmount = 49_300L,
                currentTierName = "T1",
                repaymentTier = "T1",
                blockReasonCodes = emptyList(),
                noticeReasonCodes = emptyList(),
                estimatedFeeAmountAtomic = 0L,
                estimatedFeeDisplayKrwAmount = 0L,
                estimatedRepaymentDate = "2026-03-31",
                disclaimer = "데모 시뮬레이션",
                needsReviewRecordCount = 0
            ),
            requests = emptyList()
        )
        val advanceRepository = FakeAdvanceRepository(result = remoteState)
        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = advanceRepository
        )

        advanceUntilIdle()
        viewModel.requestAdvance()
        advanceUntilIdle()

        assertTrue(advanceRepository.createCalls.isEmpty())
        assertEquals(
            "최소 신청 단위는 1 dUSDC예요. 남은 신청 가능 금액이 그보다 적어요.",
            viewModel.advanceRequestUiState.value.message
        )
        assertTrue(viewModel.advanceRequestUiState.value.isError)
    }

    @Test
    fun `open advance detail uses remote detail and caches it`() = runTest {
        val session = testSession()
        val authRepository = FakeAuthRepository(restoredSession = session)
        val advanceRepository = FakeAdvanceRepository()
        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = advanceRepository
        )

        advanceUntilIdle()
        viewModel.openAdvanceRequestDetail(1L)
        advanceUntilIdle()

        assertEquals(listOf(session.accessToken to 1L), advanceRepository.detailCalls)
        assertEquals(1L, viewModel.advanceRequestDetailUiState.value.detail?.requestId)
    }

    @Test
    fun `opening transfer flow keeps idle state when latest remote transfer is already confirmed`() = runTest {
        val session = testSession()
        val authRepository = FakeAuthRepository(restoredSession = session)
        val advanceRepository = FakeAdvanceRepository()
        val remittanceRepository = FakeRemittanceRepository(
            result = RemittanceRemoteState.content(
                RemittanceRemotePayload(
                    wallet = RemittanceWalletPayload(
                        walletAddress = "0x1111111111111111111111111111111111111111",
                        fundingStatus = "FUNDED",
                        fundingFailureReason = null,
                        fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                        createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                    ),
                    balance = RemittanceWalletBalancePayload(
                        walletAddress = "0x1111111111111111111111111111111111111111",
                        assetSymbol = "dUSDC",
                        assetDecimals = 6,
                        tokenBalanceAtomic = "500000000",
                        nativeBalanceWei = "10000000000000000"
                    ),
                    recipients = listOf(
                        RemittanceRecipientPayload(
                            recipientId = "rcpt-1",
                            alias = "테스트 수신자",
                            relation = "FRIEND",
                            walletAddress = "0x2222222222222222222222222222222222222222",
                            allowed = true,
                            recentlyUpdated = false,
                            updatedAt = LocalDateTime.parse("2026-03-19T09:05:00")
                        )
                    ),
                    transfers = listOf(
                        RemittanceTransferSummaryPayload(
                            transferId = "tx-1",
                            status = "CONFIRMED",
                            assetSymbol = "dUSDC",
                            amountAtomic = 360_000_000L,
                            recipientId = "rcpt-1",
                            recipientAlias = "테스트 수신자",
                            recipientAddress = "0x2222222222222222222222222222222222222222",
                            txHash = "0xabc",
                            updatedAt = LocalDateTime.parse("2026-03-19T09:10:00")
                        )
                    ),
                    activeTransfer = RemittanceTransferDetailPayload(
                        transferId = "tx-1",
                        status = "CONFIRMED",
                        assetSymbol = "dUSDC",
                        amountAtomic = 360_000_000L,
                        senderAddress = "0x1111111111111111111111111111111111111111",
                        recipientId = "rcpt-1",
                        recipientAlias = "테스트 수신자",
                        recipientAddress = "0x2222222222222222222222222222222222222222",
                        txHash = "0xabc",
                        failureCode = null,
                        createdAt = LocalDateTime.parse("2026-03-19T09:09:00"),
                        updatedAt = LocalDateTime.parse("2026-03-19T09:10:00")
                    )
                )
            )
        )
        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = advanceRepository,
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = remittanceRepository,
            wageRepository = FakeWageRepository()
        )

        advanceUntilIdle()
        viewModel.updateTransferAmount(320)
        viewModel.selectRecipient("R-002")
        viewModel.openTransferFlow()
        advanceUntilIdle()

        assertEquals(TransferStatus.IDLE, viewModel.uiState.value.remittance.status)
        assertEquals(2, remittanceRepository.loadedTokens.size)
        assertEquals("rcpt-1", viewModel.uiState.value.remittance.selectedRecipientId)
        assertEquals(0, viewModel.uiState.value.remittance.draftAmountUsd)
    }

    @Test
    fun `account manage wallet add stores local recipient when unauthenticated`() = runTest {
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = FakeRemittanceRepository(RemittanceRemoteState.unauthenticated("로그인이 필요해요."))
        )

        advanceUntilIdle()
        viewModel.addRecipientFromAccountManage(
            alias = "Minh Nguyen",
            relation = "FAMILY",
            walletAddress = "0x7F4F0b8E8fA0d3B6bA91F5bEEfa2276c9168a20D",
            targetUserId = null
        )
        advanceUntilIdle()

        assertEquals("Minh Nguyen", viewModel.uiState.value.remittance.recipients.first().name)
        assertEquals("가족", viewModel.uiState.value.remittance.recipients.first().relationship)
        assertEquals("수신 지갑을 추가했어요.", viewModel.remittanceActionUiState.value.message)
        assertFalse(viewModel.remittanceActionUiState.value.isError)
    }

    @Test
    fun `transfer recipient add requires authenticated session`() = runTest {
        val remittanceRepository = FakeRemittanceRepository(
            RemittanceRemoteState.unauthenticated("로그인이 필요해요.")
        )
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = remittanceRepository
        )

        advanceUntilIdle()
        val initialRecipientCount = viewModel.uiState.value.remittance.recipients.size

        viewModel.addRecipientFromTransfer(
            alias = "Minh Nguyen",
            relation = "FAMILY",
            walletAddress = "0x7F4F0b8E8fA0d3B6bA91F5bEEfa2276c9168a20D",
            targetUserId = null
        )
        advanceUntilIdle()

        assertEquals(initialRecipientCount, viewModel.uiState.value.remittance.recipients.size)
        assertTrue(remittanceRepository.createRecipientCalls.isEmpty())
        assertEquals("로그인 후 다시 시도해 주세요.", viewModel.remittanceActionUiState.value.message)
        assertTrue(viewModel.remittanceActionUiState.value.isError)
    }

    @Test
    fun `account manage wallet add uses remote recipient create when authenticated`() = runTest {
        val session = testSession()
        val createdRecipient = RemittanceRecipientPayload(
            recipientId = "rcpt-added",
            alias = "Minh Nguyen",
            relation = "FAMILY",
            walletAddress = "0x7F4F0b8E8fA0d3B6bA91F5bEEfa2276c9168a20D",
            allowed = true,
            recentlyUpdated = false,
            updatedAt = LocalDateTime.parse("2026-03-19T11:00:00")
        )
        val initialRemoteState = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "500000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = emptyList(),
                transfers = emptyList(),
                activeTransfer = null
            )
        )
        val remoteStateAfterCreate = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "500000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = listOf(createdRecipient),
                transfers = emptyList(),
                activeTransfer = null
            )
        )
        val remittanceRepository = FakeRemittanceRepository(
            result = initialRemoteState,
            createdRecipient = createdRecipient,
            resultAfterCreate = remoteStateAfterCreate
        )
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = remittanceRepository
        )

        advanceUntilIdle()
        viewModel.addRecipientFromAccountManage(
            alias = "Minh Nguyen",
            relation = "FAMILY",
            walletAddress = "0x7F4F0b8E8fA0d3B6bA91F5bEEfa2276c9168a20D",
            targetUserId = null
        )
        advanceUntilIdle()

        assertEquals(
            listOf(
                CreateRecipientCall(
                    accessToken = session.accessToken,
                    alias = "Minh Nguyen",
                    relation = "FAMILY",
                    walletAddress = "0x7F4F0b8E8fA0d3B6bA91F5bEEfa2276c9168a20D",
                    targetUserId = null
                )
            ),
            remittanceRepository.createRecipientCalls
        )
        assertEquals("rcpt-added", viewModel.uiState.value.remittance.selectedRecipientId)
        assertEquals("수신 지갑을 추가했어요.", viewModel.remittanceActionUiState.value.message)
        assertFalse(viewModel.remittanceActionUiState.value.isError)
    }

    @Test
    fun `account manage wallet edit updates local recipient when unauthenticated`() = runTest {
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = FakeRemittanceRepository(RemittanceRemoteState.unauthenticated("로그인이 필요해요."))
        )

        advanceUntilIdle()
        viewModel.updateRecipientFromAccountManage(
            recipientId = "R-001",
            alias = "수정된 Minh",
            relation = "FRIEND",
            walletAddress = "0x1234567890abcdef1234567890abcdef12345678"
        )
        advanceUntilIdle()

        assertEquals("수정된 Minh", viewModel.uiState.value.remittance.recipients.first().name)
        assertEquals("친구", viewModel.uiState.value.remittance.recipients.first().relationship)
        assertEquals("0x1234567890abcdef1234567890abcdef12345678", viewModel.uiState.value.remittance.recipients.first().address)
        assertEquals("수신 지갑을 수정했어요.", viewModel.remittanceActionUiState.value.message)
        assertFalse(viewModel.remittanceActionUiState.value.isError)
    }

    @Test
    fun `account manage wallet edit uses remote recipient update when authenticated`() = runTest {
        val session = testSession()
        val initialRecipient = RemittanceRecipientPayload(
            recipientId = "rcpt-1",
            alias = "Minh Nguyen",
            relation = "FAMILY",
            walletAddress = "0x7F4F0b8E8fA0d3B6bA91F5bEEfa2276c9168a20D",
            allowed = true,
            recentlyUpdated = false,
            updatedAt = LocalDateTime.parse("2026-03-19T11:00:00")
        )
        val updatedRecipient = initialRecipient.copy(
            alias = "Anh Nguyen",
            relation = "FRIEND",
            walletAddress = "0x1234567890abcdef1234567890abcdef12345678"
        )
        val initialRemoteState = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "500000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = listOf(initialRecipient),
                transfers = emptyList(),
                activeTransfer = null
            )
        )
        val remoteStateAfterUpdate = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "500000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = listOf(updatedRecipient),
                transfers = emptyList(),
                activeTransfer = null
            )
        )
        val remittanceRepository = FakeRemittanceRepository(
            result = initialRemoteState,
            resultAfterUpdate = remoteStateAfterUpdate
        )
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = remittanceRepository
        )

        advanceUntilIdle()
        viewModel.updateRecipientFromAccountManage(
            recipientId = "rcpt-1",
            alias = "Anh Nguyen",
            relation = "FRIEND",
            walletAddress = "0x1234567890abcdef1234567890abcdef12345678"
        )
        advanceUntilIdle()

        assertEquals(
            listOf(
                UpdateRecipientCall(
                    accessToken = session.accessToken,
                    recipientId = "rcpt-1",
                    alias = "Anh Nguyen",
                    relation = "FRIEND",
                    walletAddress = "0x1234567890abcdef1234567890abcdef12345678"
                )
            ),
            remittanceRepository.updateRecipientCalls
        )
        assertEquals("Anh Nguyen", viewModel.uiState.value.remittance.recipients.first().name)
        assertEquals("친구", viewModel.uiState.value.remittance.recipients.first().relationship)
        assertEquals("수신 지갑을 수정했어요.", viewModel.remittanceActionUiState.value.message)
        assertFalse(viewModel.remittanceActionUiState.value.isError)
    }

    @Test
    fun `confirmed transfer reload shows terminal tracker and reset allows next transfer`() = runTest {
        val session = testSession()
        val walletAddress = "0x1111111111111111111111111111111111111111"
        val recipientAddress = "0x2222222222222222222222222222222222222222"
        val initialRemoteState = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = walletAddress,
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = walletAddress,
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "500000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = listOf(
                    RemittanceRecipientPayload(
                        recipientId = "rcpt-1",
                        alias = "테스트 수신자",
                        relation = "FRIEND",
                        walletAddress = recipientAddress,
                        allowed = true,
                        recentlyUpdated = false,
                        updatedAt = LocalDateTime.parse("2026-03-19T09:05:00")
                    )
                ),
                transfers = emptyList(),
                activeTransfer = null
            )
        )
        val confirmedTransferDetail = RemittanceTransferDetailPayload(
            transferId = "tx-1",
            status = "CONFIRMED",
            assetSymbol = "dUSDC",
            amountAtomic = 360_000_000L,
            senderAddress = walletAddress,
            recipientId = "rcpt-1",
            recipientAlias = "테스트 수신자",
            recipientAddress = recipientAddress,
            txHash = "0xabc",
            failureCode = null,
            createdAt = LocalDateTime.parse("2026-03-19T09:09:00"),
            updatedAt = LocalDateTime.parse("2026-03-19T09:10:00")
        )
        val remoteStateAfterConfirmation = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = walletAddress,
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = walletAddress,
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "140000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = listOf(
                    RemittanceRecipientPayload(
                        recipientId = "rcpt-1",
                        alias = "테스트 수신자",
                        relation = "FRIEND",
                        walletAddress = recipientAddress,
                        allowed = true,
                        recentlyUpdated = false,
                        updatedAt = LocalDateTime.parse("2026-03-19T09:05:00")
                    )
                ),
                transfers = listOf(
                    RemittanceTransferSummaryPayload(
                        transferId = "tx-1",
                        status = "CONFIRMED",
                        assetSymbol = "dUSDC",
                        amountAtomic = 360_000_000L,
                        recipientId = "rcpt-1",
                        recipientAlias = "테스트 수신자",
                        recipientAddress = recipientAddress,
                        txHash = "0xabc",
                        updatedAt = LocalDateTime.parse("2026-03-19T09:10:00")
                    )
                ),
                activeTransfer = confirmedTransferDetail
            )
        )
        val remittanceRepository = FakeRemittanceRepository(
            result = initialRemoteState,
            precheckResult = RemittanceTransferPrecheckPayload(
                allowed = true,
                policyCode = null,
                assetSymbol = "dUSDC",
                highAmountThresholdAtomic = 1_000_000_000L,
                recentRecipientConfirmationRequired = false,
                recentRecipientUpdatedAt = null,
                walletAddress = walletAddress,
                currentTokenBalanceAtomic = "500000000",
                currentNativeBalanceWei = "10000000000000000"
            ),
            createTransferResult = RemittanceCreateTransferPayload(
                transferId = "tx-1",
                status = "REQUESTED",
                assetSymbol = "dUSDC",
                amountAtomic = 360_000_000L,
                recipientId = "rcpt-1",
                createdAt = LocalDateTime.parse("2026-03-19T09:09:00")
            ),
            transferDetailResults = listOf(confirmedTransferDetail),
            resultAfterTerminalLoad = remoteStateAfterConfirmation
        )
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = remittanceRepository,
            wageRepository = FakeWageRepository()
        )

        advanceUntilIdle()
        viewModel.updateTransferAmount(360)
        viewModel.submitTransfer()
        advanceUntilIdle()
        viewModel.confirmTransfer()
        advanceUntilIdle()
        advanceTimeBy(1_500L)
        advanceUntilIdle()

        assertEquals("140000000", viewModel.remittanceRemoteState.value.payload?.balance?.tokenBalanceAtomic)
        assertEquals(TransferStatus.CONFIRMED, viewModel.uiState.value.remittance.status)
        assertEquals("0xabc", viewModel.uiState.value.remittance.txHash)
        assertEquals(3, remittanceRepository.loadedTokens.size)
        assertEquals(
            "140 dUSDC",
            viewModel.uiState.value.toHomeUiModel(
                remittanceRemoteState = viewModel.remittanceRemoteState.value,
                isAuthenticated = true
            ).account.balanceText
        )

        viewModel.resetTransfer()
        advanceUntilIdle()

        assertEquals(TransferStatus.IDLE, viewModel.uiState.value.remittance.status)
        assertEquals("", viewModel.uiState.value.remittance.txHash)

        viewModel.refreshRemittanceRemoteState()
        advanceUntilIdle()

        assertEquals(TransferStatus.IDLE, viewModel.uiState.value.remittance.status)
        assertEquals("", viewModel.uiState.value.remittance.txHash)

        viewModel.updateTransferAmount(360)
        advanceUntilIdle()
        viewModel.submitTransfer()
        advanceUntilIdle()

        assertEquals(TransferStatus.REVIEWING, viewModel.uiState.value.remittance.status)
    }

    @Test
    fun `remittance polling keeps tracking until delayed confirmation arrives`() = runTest {
        val session = testSession()
        val walletAddress = "0x1111111111111111111111111111111111111111"
        val recipientAddress = "0x2222222222222222222222222222222222222222"
        val initialRemoteState = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = walletAddress,
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = walletAddress,
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "500000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = listOf(
                    RemittanceRecipientPayload(
                        recipientId = "rcpt-1",
                        alias = "테스트 수신자",
                        relation = "FRIEND",
                        walletAddress = recipientAddress,
                        allowed = true,
                        recentlyUpdated = false,
                        updatedAt = LocalDateTime.parse("2026-03-19T09:05:00")
                    )
                ),
                transfers = emptyList(),
                activeTransfer = null
            )
        )
        val delayedTransferDetails = buildList {
            repeat(12) { attempt ->
                add(
                    RemittanceTransferDetailPayload(
                        transferId = "tx-1",
                        status = "BROADCASTED",
                        assetSymbol = "dUSDC",
                        amountAtomic = 360_000_000L,
                        senderAddress = walletAddress,
                        recipientId = "rcpt-1",
                        recipientAlias = "테스트 수신자",
                        recipientAddress = recipientAddress,
                        txHash = "0xabc",
                        failureCode = null,
                        createdAt = LocalDateTime.parse("2026-03-19T09:09:00"),
                        updatedAt = LocalDateTime.parse("2026-03-19T09:${10 + attempt}:00")
                    )
                )
            }
            add(
                RemittanceTransferDetailPayload(
                    transferId = "tx-1",
                    status = "CONFIRMED",
                    assetSymbol = "dUSDC",
                    amountAtomic = 360_000_000L,
                    senderAddress = walletAddress,
                    recipientId = "rcpt-1",
                    recipientAlias = "테스트 수신자",
                    recipientAddress = recipientAddress,
                    txHash = "0xabc",
                    failureCode = null,
                    createdAt = LocalDateTime.parse("2026-03-19T09:09:00"),
                    updatedAt = LocalDateTime.parse("2026-03-19T09:22:00")
                )
            )
        }
        val remoteStateAfterConfirmation = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = walletAddress,
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = walletAddress,
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "140000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = listOf(
                    RemittanceRecipientPayload(
                        recipientId = "rcpt-1",
                        alias = "테스트 수신자",
                        relation = "FRIEND",
                        walletAddress = recipientAddress,
                        allowed = true,
                        recentlyUpdated = false,
                        updatedAt = LocalDateTime.parse("2026-03-19T09:05:00")
                    )
                ),
                transfers = listOf(
                    RemittanceTransferSummaryPayload(
                        transferId = "tx-1",
                        status = "CONFIRMED",
                        assetSymbol = "dUSDC",
                        amountAtomic = 360_000_000L,
                        recipientId = "rcpt-1",
                        recipientAlias = "테스트 수신자",
                        recipientAddress = recipientAddress,
                        txHash = "0xabc",
                        updatedAt = LocalDateTime.parse("2026-03-19T09:22:00")
                    )
                ),
                activeTransfer = delayedTransferDetails.last()
            )
        )
        val remittanceRepository = FakeRemittanceRepository(
            result = initialRemoteState,
            precheckResult = RemittanceTransferPrecheckPayload(
                allowed = true,
                policyCode = null,
                assetSymbol = "dUSDC",
                highAmountThresholdAtomic = 1_000_000_000L,
                recentRecipientConfirmationRequired = false,
                recentRecipientUpdatedAt = null,
                walletAddress = walletAddress,
                currentTokenBalanceAtomic = "500000000",
                currentNativeBalanceWei = "10000000000000000"
            ),
            createTransferResult = RemittanceCreateTransferPayload(
                transferId = "tx-1",
                status = "REQUESTED",
                assetSymbol = "dUSDC",
                amountAtomic = 360_000_000L,
                recipientId = "rcpt-1",
                createdAt = LocalDateTime.parse("2026-03-19T09:09:00")
            ),
            transferDetailResults = delayedTransferDetails,
            resultAfterTerminalLoad = remoteStateAfterConfirmation
        )
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = remittanceRepository,
            wageRepository = FakeWageRepository()
        )

        advanceUntilIdle()
        viewModel.updateTransferAmount(360)
        viewModel.submitTransfer()
        advanceUntilIdle()
        viewModel.confirmTransfer()
        advanceUntilIdle()
        advanceTimeBy(1_500L * 13)
        advanceUntilIdle()

        assertEquals(TransferStatus.CONFIRMED, viewModel.uiState.value.remittance.status)
        assertEquals("0xabc", viewModel.uiState.value.remittance.txHash)
        assertEquals("140000000", viewModel.remittanceRemoteState.value.payload?.balance?.tokenBalanceAtomic)
    }

    @Test
    fun `transfer create success enqueues home launch and terminal completion notice`() = runTest {
        val session = testSession()
        val walletAddress = "0x1111111111111111111111111111111111111111"
        val recipientAddress = "0x2222222222222222222222222222222222222222"
        val initialRemoteState = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = walletAddress,
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = walletAddress,
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "500000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = listOf(
                    RemittanceRecipientPayload(
                        recipientId = "rcpt-1",
                        alias = "테스트 수신자",
                        relation = "FRIEND",
                        walletAddress = recipientAddress,
                        allowed = true,
                        recentlyUpdated = false,
                        updatedAt = LocalDateTime.parse("2026-03-19T09:05:00")
                    )
                ),
                transfers = emptyList(),
                activeTransfer = null
            )
        )
        val confirmedTransferDetail = RemittanceTransferDetailPayload(
            transferId = "tx-1",
            status = "CONFIRMED",
            assetSymbol = "dUSDC",
            amountAtomic = 360_000_000L,
            senderAddress = walletAddress,
            recipientId = "rcpt-1",
            recipientAlias = "테스트 수신자",
            recipientAddress = recipientAddress,
            txHash = "0xabc",
            failureCode = null,
            createdAt = LocalDateTime.parse("2026-03-19T09:09:00"),
            updatedAt = LocalDateTime.parse("2026-03-19T09:10:00")
        )
        val remoteStateAfterConfirmation = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = walletAddress,
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = walletAddress,
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "140000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = initialRemoteState.payload?.recipients.orEmpty(),
                transfers = listOf(
                    RemittanceTransferSummaryPayload(
                        transferId = "tx-1",
                        status = "CONFIRMED",
                        assetSymbol = "dUSDC",
                        amountAtomic = 360_000_000L,
                        recipientId = "rcpt-1",
                        recipientAlias = "테스트 수신자",
                        recipientAddress = recipientAddress,
                        txHash = "0xabc",
                        updatedAt = LocalDateTime.parse("2026-03-19T09:10:00")
                    )
                ),
                activeTransfer = confirmedTransferDetail
            )
        )
        val remittanceRepository = FakeRemittanceRepository(
            result = initialRemoteState,
            precheckResult = RemittanceTransferPrecheckPayload(
                allowed = true,
                policyCode = null,
                assetSymbol = "dUSDC",
                highAmountThresholdAtomic = 1_000_000_000L,
                recentRecipientConfirmationRequired = false,
                recentRecipientUpdatedAt = null,
                walletAddress = walletAddress,
                currentTokenBalanceAtomic = "500000000",
                currentNativeBalanceWei = "10000000000000000"
            ),
            createTransferResult = RemittanceCreateTransferPayload(
                transferId = "tx-1",
                status = "REQUESTED",
                assetSymbol = "dUSDC",
                amountAtomic = 360_000_000L,
                recipientId = "rcpt-1",
                createdAt = LocalDateTime.parse("2026-03-19T09:09:00")
            ),
            transferDetailResults = listOf(confirmedTransferDetail),
            resultAfterTerminalLoad = remoteStateAfterConfirmation
        )
        val noticeStore = FakeRemittanceCompletionNoticeStore()
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = remittanceRepository,
            remittanceCompletionNoticeStore = noticeStore,
            wageRepository = FakeWageRepository()
        )

        advanceUntilIdle()
        viewModel.updateTransferAmount(360)
        viewModel.submitTransfer()
        advanceUntilIdle()
        viewModel.confirmTransfer()
        runCurrent()

        assertEquals(1L, viewModel.remittanceLaunchRequest.value?.requestId)
        assertEquals("송금 요청을 접수했어요.", viewModel.remittanceActionUiState.value.message)

        advanceTimeBy(1_500L)
        advanceUntilIdle()

        assertEquals("tx-1", viewModel.remittanceCompletionNoticeUiState.value.transferId)
        assertEquals(TransferStatus.CONFIRMED, viewModel.remittanceCompletionNoticeUiState.value.status)
        assertEquals("송금이 완료됐어요.", viewModel.remittanceActionUiState.value.message)
        assertEquals(null, noticeStore.readDismissedTransferId(session.userId))
    }

    @Test
    fun `dismissed remittance completion notice is not restored for same transfer`() = runTest {
        val session = testSession()
        val confirmedTransferDetail = RemittanceTransferDetailPayload(
            transferId = "tx-1",
            status = "CONFIRMED",
            assetSymbol = "dUSDC",
            amountAtomic = 360_000_000L,
            senderAddress = "0x1111111111111111111111111111111111111111",
            recipientId = "rcpt-1",
            recipientAlias = "테스트 수신자",
            recipientAddress = "0x2222222222222222222222222222222222222222",
            txHash = "0xabc",
            failureCode = null,
            createdAt = LocalDateTime.parse("2026-03-19T09:09:00"),
            updatedAt = LocalDateTime.parse("2026-03-19T09:10:00")
        )
        val remoteState = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "140000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = listOf(
                    RemittanceRecipientPayload(
                        recipientId = "rcpt-1",
                        alias = "테스트 수신자",
                        relation = "FRIEND",
                        walletAddress = "0x2222222222222222222222222222222222222222",
                        allowed = true,
                        recentlyUpdated = false,
                        updatedAt = LocalDateTime.parse("2026-03-19T09:05:00")
                    )
                ),
                transfers = listOf(
                    RemittanceTransferSummaryPayload(
                        transferId = "tx-1",
                        status = "CONFIRMED",
                        assetSymbol = "dUSDC",
                        amountAtomic = 360_000_000L,
                        recipientId = "rcpt-1",
                        recipientAlias = "테스트 수신자",
                        recipientAddress = "0x2222222222222222222222222222222222222222",
                        txHash = "0xabc",
                        updatedAt = LocalDateTime.parse("2026-03-19T09:10:00")
                    )
                ),
                activeTransfer = confirmedTransferDetail
            )
        )
        val noticeStore = FakeRemittanceCompletionNoticeStore()
        val firstViewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = FakeRemittanceRepository(result = remoteState),
            remittanceCompletionNoticeStore = noticeStore,
            wageRepository = FakeWageRepository()
        )

        advanceUntilIdle()

        assertEquals("tx-1", firstViewModel.remittanceCompletionNoticeUiState.value.transferId)

        firstViewModel.dismissRemittanceCompletionNotice()

        assertEquals("tx-1", noticeStore.readDismissedTransferId(session.userId))
        assertEquals(null, firstViewModel.remittanceCompletionNoticeUiState.value.transferId)

        val secondViewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = FakeRemittanceRepository(result = remoteState),
            remittanceCompletionNoticeStore = noticeStore,
            wageRepository = FakeWageRepository()
        )

        advanceUntilIdle()

        assertEquals(null, secondViewModel.remittanceCompletionNoticeUiState.value.transferId)
    }

    @Test
    fun `open transfer flow is blocked while remote transfer is still pending`() = runTest {
        val session = testSession()
        val remoteState = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "500000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = listOf(
                    RemittanceRecipientPayload(
                        recipientId = "rcpt-1",
                        alias = "테스트 수신자",
                        relation = "FRIEND",
                        walletAddress = "0x2222222222222222222222222222222222222222",
                        allowed = true,
                        recentlyUpdated = false,
                        updatedAt = LocalDateTime.parse("2026-03-19T09:05:00")
                    )
                ),
                transfers = listOf(
                    RemittanceTransferSummaryPayload(
                        transferId = "tx-1",
                        status = "BROADCASTED",
                        assetSymbol = "dUSDC",
                        amountAtomic = 360_000_000L,
                        recipientId = "rcpt-1",
                        recipientAlias = "테스트 수신자",
                        recipientAddress = "0x2222222222222222222222222222222222222222",
                        txHash = "0xabc",
                        updatedAt = LocalDateTime.parse("2026-03-19T09:10:00")
                    )
                ),
                activeTransfer = RemittanceTransferDetailPayload(
                    transferId = "tx-1",
                    status = "BROADCASTED",
                    assetSymbol = "dUSDC",
                    amountAtomic = 360_000_000L,
                    senderAddress = "0x1111111111111111111111111111111111111111",
                    recipientId = "rcpt-1",
                    recipientAlias = "테스트 수신자",
                    recipientAddress = "0x2222222222222222222222222222222222222222",
                    txHash = "0xabc",
                    failureCode = null,
                    createdAt = LocalDateTime.parse("2026-03-19T09:09:00"),
                    updatedAt = LocalDateTime.parse("2026-03-19T09:10:00")
                )
            )
        )
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = FakeRemittanceRepository(result = remoteState),
            wageRepository = FakeWageRepository()
        )

        advanceUntilIdle()

        assertFalse(viewModel.openTransferFlow())
        assertEquals("이전 송금 결과를 확인하고 있어요.", viewModel.remittanceActionUiState.value.message)
        assertEquals("tx-1", viewModel.remittanceRemoteState.value.payload?.activeTransfer?.transferId)
    }

    @Test
    fun `logout clears only current user dismissed transfer id`() = runTest {
        val currentSession = testSession()
        val otherSession = testSession(accessToken = "token-456", email = "other@gmail.com").copy(userId = 99L)
        val noticeStore = FakeRemittanceCompletionNoticeStore().apply {
            saveDismissedTransferId(currentSession.userId, "tx-current")
            saveDismissedTransferId(otherSession.userId, "tx-other")
        }
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = currentSession),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = FakeRemittanceRepository(RemittanceRemoteState.unauthenticated("로그인이 필요해요.")),
            remittanceCompletionNoticeStore = noticeStore,
            wageRepository = FakeWageRepository()
        )

        advanceUntilIdle()
        viewModel.logout()
        advanceUntilIdle()

        assertEquals(null, noticeStore.readDismissedTransferId(currentSession.userId))
        assertEquals("tx-other", noticeStore.readDismissedTransferId(otherSession.userId))
    }

    @Test
    fun `silent remittance reload still expires session when terminal refresh becomes unauthenticated`() = runTest {
        val session = testSession()
        val walletAddress = "0x1111111111111111111111111111111111111111"
        val recipientAddress = "0x2222222222222222222222222222222222222222"
        val initialRemoteState = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = walletAddress,
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = walletAddress,
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "500000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = listOf(
                    RemittanceRecipientPayload(
                        recipientId = "rcpt-1",
                        alias = "테스트 수신자",
                        relation = "FRIEND",
                        walletAddress = recipientAddress,
                        allowed = true,
                        recentlyUpdated = false,
                        updatedAt = LocalDateTime.parse("2026-03-19T09:05:00")
                    )
                ),
                transfers = emptyList(),
                activeTransfer = null
            )
        )
        val confirmedTransferDetail = RemittanceTransferDetailPayload(
            transferId = "tx-1",
            status = "CONFIRMED",
            assetSymbol = "dUSDC",
            amountAtomic = 360_000_000L,
            senderAddress = walletAddress,
            recipientId = "rcpt-1",
            recipientAlias = "테스트 수신자",
            recipientAddress = recipientAddress,
            txHash = "0xabc",
            failureCode = null,
            createdAt = LocalDateTime.parse("2026-03-19T09:09:00"),
            updatedAt = LocalDateTime.parse("2026-03-19T09:10:00")
        )
        val authRepository = FakeAuthRepository(restoredSession = session)
        val remittanceRepository = FakeRemittanceRepository(
            result = initialRemoteState,
            precheckResult = RemittanceTransferPrecheckPayload(
                allowed = true,
                policyCode = null,
                assetSymbol = "dUSDC",
                highAmountThresholdAtomic = 1_000_000_000L,
                recentRecipientConfirmationRequired = false,
                recentRecipientUpdatedAt = null,
                walletAddress = walletAddress,
                currentTokenBalanceAtomic = "500000000",
                currentNativeBalanceWei = "10000000000000000"
            ),
            createTransferResult = RemittanceCreateTransferPayload(
                transferId = "tx-1",
                status = "REQUESTED",
                assetSymbol = "dUSDC",
                amountAtomic = 360_000_000L,
                recipientId = "rcpt-1",
                createdAt = LocalDateTime.parse("2026-03-19T09:09:00")
            ),
            transferDetailResults = listOf(confirmedTransferDetail),
            resultAfterTerminalLoad = RemittanceRemoteState.unauthenticated("세션이 만료되어 다시 로그인해 주세요.")
        )
        val viewModel = DemoSessionViewModel(
            authRepository = authRepository,
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = remittanceRepository,
            wageRepository = FakeWageRepository()
        )

        advanceUntilIdle()
        viewModel.updateTransferAmount(360)
        viewModel.submitTransfer()
        advanceUntilIdle()
        viewModel.confirmTransfer()
        advanceUntilIdle()
        advanceTimeBy(1_500L)
        advanceUntilIdle()

        assertFalse(viewModel.authUiState.value.isAuthenticated)
        assertEquals("세션이 만료되어 다시 로그인해 주세요.", viewModel.authUiState.value.errorMessage)
        assertTrue(authRepository.logoutCalled)
    }

    @Test
    fun `vault deposit confirmation updates authenticated home balance`() = runTest {
        val session = testSession()
        val remittanceRepository = FakeRemittanceRepository(
            result = remittanceState(tokenBalanceAtomic = "500000000"),
            resultAfterTerminalLoad = remittanceState(tokenBalanceAtomic = "400000000")
        )
        val vaultRepository = FakeVaultRepository(
            initialState = vaultState(
                walletTokenBalanceAtomic = "500000000",
                storedAmountAtomic = "0",
                availableToStoreAmountAtomic = "500000000",
                latestTransaction = null
            ),
            stateAfterTerminalLoad = vaultState(
                walletTokenBalanceAtomic = "400000000",
                storedAmountAtomic = "100000000",
                availableToStoreAmountAtomic = "400000000",
                latestTransaction = null
            ),
            createDepositResult = VaultCreateTransactionPayload(
                requestId = "vtx-1",
                txType = "DEPOSIT",
                status = "REQUESTED",
                detailPath = "/api/vault/transactions/vtx-1",
                createdAt = LocalDateTime.parse("2026-03-23T10:00:00")
            ),
            transactionDetails = listOf(
                VaultTransactionDetailPayload(
                    requestId = "vtx-1",
                    txType = "DEPOSIT",
                    status = "CONFIRMED",
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    vaultAddress = "0x2222222222222222222222222222222222222222",
                    assetSymbol = "dUSDC",
                    amountAtomic = "100000000",
                    shareDelta = "100000000",
                    txHash = "0xabc",
                    failureCode = null,
                    createdAt = LocalDateTime.parse("2026-03-23T10:00:00"),
                    updatedAt = LocalDateTime.parse("2026-03-23T10:01:00"),
                    confirmedAt = LocalDateTime.parse("2026-03-23T10:01:00")
                )
            )
        )
        val viewModel = DemoSessionViewModel(
            authRepository = FakeAuthRepository(restoredSession = session),
            advanceRepository = FakeAdvanceRepository(),
            workproofRepository = FakeWorkproofRepository(),
            remittanceRepository = remittanceRepository,
            vaultRepository = vaultRepository,
            wageRepository = FakeWageRepository()
        )

        advanceUntilIdle()
        viewModel.submitVaultAction()
        advanceUntilIdle()

        assertEquals("예치 요청을 접수했어요.", viewModel.vaultActionUiState.value.message)

        advanceTimeBy(1_500L)
        advanceUntilIdle()

        assertEquals("400000000", viewModel.remittanceRemoteState.value.payload?.balance?.tokenBalanceAtomic)
        assertEquals(
            "400 dUSDC",
            viewModel.uiState.value.toHomeUiModel(
                remittanceRemoteState = viewModel.remittanceRemoteState.value,
                isAuthenticated = true
            ).account.balanceText
        )
        assertEquals(2, remittanceRepository.loadedTokens.size)
    }

    private fun testSession(
        accessToken: String = "token-123",
        email: String = "test@gmail.com",
        phoneNumber: String? = "01012345678"
    ) = AuthSession(
        accessToken = accessToken,
        tokenType = "Bearer",
        expiresAtEpochMillis = Long.MAX_VALUE,
        userId = 7L,
        email = email,
        name = "테스트 사용자",
        phoneNumber = phoneNumber,
        companyCode = null
    )

    private data class SignupCall(
        val name: String,
        val email: String,
        val password: String,
        val phoneNumber: String
    )

    private class FakeRemittanceCompletionNoticeStore : RemittanceCompletionNoticeStore {
        private val dismissedTransferIds = mutableMapOf<Long, String>()

        override fun readDismissedTransferId(userId: Long): String? = dismissedTransferIds[userId]

        override fun saveDismissedTransferId(userId: Long, transferId: String) {
            dismissedTransferIds[userId] = transferId
        }

        override fun clear(userId: Long) {
            dismissedTransferIds.remove(userId)
        }

        override fun clearAll() {
            dismissedTransferIds.clear()
        }
    }

    private class FakeAuthRepository(
        private val restoredSession: AuthSession? = null,
        private val loginSession: AuthSession? = null,
        private val signupSession: AuthSession? = null,
        private val updateProfileSession: AuthSession? = null,
        private val redeemWorkerRegistrationCodeSession: AuthSession? = null,
        private val updateProfileError: Exception? = null
    ) : AuthRepository {
        val loginCalls = mutableListOf<Pair<String, String>>()
        val signupCalls = mutableListOf<SignupCall>()
        val updateProfileCalls = mutableListOf<Pair<String, String>>()
        var logoutCalled: Boolean = false

        override suspend fun restore(): AuthSession? = restoredSession

        override suspend fun signup(name: String, email: String, password: String, phoneNumber: String): AuthSession {
            signupCalls += SignupCall(name, email, password, phoneNumber)
            return signupSession ?: error("signup should not be called without a prepared session")
        }

        override suspend fun login(email: String, password: String): AuthSession {
            loginCalls += email to password
            return loginSession ?: error("login should not be called without a prepared session")
        }

        override suspend fun updateProfile(session: AuthSession, name: String, phoneNumber: String): AuthSession {
            updateProfileCalls += name to phoneNumber
            updateProfileError?.let { throw it }
            return updateProfileSession ?: error("updateProfile should not be called without a prepared session")
        }

        override suspend fun redeemWorkerRegistrationCode(
            session: AuthSession,
            registrationCode: String
        ): AuthSession {
            return redeemWorkerRegistrationCodeSession ?: session.copy(companyCode = registrationCode)
        }

        override suspend fun logout() {
            logoutCalled = true
        }
    }

    private class FakeAdvanceRepository(
        private val result: AdvanceRemoteState = AdvanceRemoteState.content(
            workplaceName = "DonDone Cafe",
            eligibility = com.dondone.mobile.data.advance.AdvanceEligibilityPayload(
                workplaceId = 1L,
                assetSymbol = "USDC",
                assetDecimals = 6,
                exchangeRateSnapshot = BigDecimal("1350"),
                availableAmountAtomic = 180_000L * 1_000_000L,
                availableDisplayKrwAmount = 180_000L,
                maxCapAmountAtomic = 500_000L * 1_000_000L,
                maxCapDisplayKrwAmount = 500_000L,
                currentTierName = "T2",
                repaymentTier = "T2",
                blockReasonCodes = emptyList(),
                noticeReasonCodes = emptyList(),
                estimatedFeeAmountAtomic = 0L,
                estimatedFeeDisplayKrwAmount = 0L,
                estimatedRepaymentDate = "2026-03-25",
                disclaimer = "데모 시뮬레이션",
                needsReviewRecordCount = 0
            ),
            requests = emptyList()
        )
    ) : AdvanceRepository {
        val loadedTokens = mutableListOf<String>()
        val createCalls = mutableListOf<Triple<String, Long, Long>>()
        val detailCalls = mutableListOf<Pair<String, Long>>()

        override suspend fun load(accessToken: String): AdvanceRemoteState {
            loadedTokens += accessToken
            return result
        }

        override suspend fun createRequest(
            accessToken: String,
            workplaceId: Long,
            requestedAmountAtomic: Long
        ): AdvanceCreateResult {
            createCalls += Triple(accessToken, workplaceId, requestedAmountAtomic)
            return AdvanceCreateResult(
                requestId = 1L,
                assetSymbol = "USDC",
                assetDecimals = 6,
                exchangeRateSnapshot = BigDecimal("1350"),
                status = "APPROVED",
                requestStatus = "APPROVED",
                payoutStatus = "READY",
                approvedAmountAtomic = requestedAmountAtomic,
                approvedDisplayKrwAmount = requestedAmountAtomic / 1_000_000L,
                feeAmountAtomic = 0L,
                feeDisplayKrwAmount = 0L,
                repaymentDueDate = "2026-03-25",
                eligibilitySnapshot = com.dondone.mobile.data.advance.AdvanceEligibilitySnapshotPayload(
                    assetSymbol = "USDC",
                    assetDecimals = 6,
                    exchangeRateSnapshot = BigDecimal("1350"),
                    availableAmountAtomic = 180_000L * 1_000_000L,
                    availableDisplayKrwAmount = 180_000L,
                    maxCapAmountAtomic = 500_000L * 1_000_000L,
                    maxCapDisplayKrwAmount = 500_000L,
                    policyRate = "0.35",
                    reflectedWorkDays = 5,
                    reflectedWorkMinutes = 2_400L,
                    needsReviewRecordCount = 0
                )
            )
        }

        override suspend fun getRequestDetail(
            accessToken: String,
            requestId: Long
        ): AdvanceRequestDetailPayload {
            detailCalls += accessToken to requestId
            return AdvanceRequestDetailPayload(
                requestId = requestId,
                workplaceId = 1L,
                assetSymbol = "USDC",
                assetDecimals = 6,
                exchangeRateSnapshot = BigDecimal("1350"),
                requestedAmountAtomic = 100_000L * 1_000_000L,
                requestedDisplayKrwAmount = 100_000L,
                approvedAmountAtomic = 100_000L * 1_000_000L,
                approvedDisplayKrwAmount = 100_000L,
                feeAmountAtomic = 0L,
                feeDisplayKrwAmount = 0L,
                status = "APPROVED",
                requestStatus = "APPROVED",
                payoutStatus = "READY",
                payoutTxHash = null,
                repaymentDueDate = "2026-03-25",
                eligibilitySnapshot = com.dondone.mobile.data.advance.AdvanceEligibilitySnapshotPayload(
                    assetSymbol = "USDC",
                    assetDecimals = 6,
                    exchangeRateSnapshot = BigDecimal("1350"),
                    availableAmountAtomic = 180_000L * 1_000_000L,
                    availableDisplayKrwAmount = 180_000L,
                    maxCapAmountAtomic = 500_000L * 1_000_000L,
                    maxCapDisplayKrwAmount = 500_000L,
                    policyRate = "0.35",
                    reflectedWorkDays = 5,
                    reflectedWorkMinutes = 2_400L,
                    needsReviewRecordCount = 0
                ),
                createdAt = "2026-03-16T09:00:00"
            )
        }
    }

    private class FakeWorkproofRepository(
        private val workplaceName: String = "DonDone Cafe",
        private val allowedRadiusMeters: Int = 100
    ) : WorkproofRepository {
        val loadedTokens = mutableListOf<String>()
        val clockInRequests = mutableListOf<WorkproofData>()
        val clockOutRequests = mutableListOf<WorkproofData>()

        override suspend fun load(accessToken: String): WorkproofRemoteState {
            loadedTokens += accessToken
            return WorkproofRemoteState.content(
                WorkproofRemotePayload(
                    workplace = WorkproofWorkplacePayload(
                        workplaceId = 1L,
                        name = workplaceName,
                        address = "서울시 강남구 테헤란로",
                        latitude = 37.5013,
                        longitude = 127.0396,
                        allowedRadiusMeters = allowedRadiusMeters
                    ),
                    records = emptyList()
                )
            )
        }

        override suspend fun clockIn(accessToken: String, workproof: WorkproofData): WorkproofRemoteState {
            clockInRequests += workproof
            return load(accessToken)
        }

        override suspend fun clockOut(accessToken: String, workproof: WorkproofData): WorkproofRemoteState {
            clockOutRequests += workproof
            return load(accessToken)
        }

        override suspend fun createCorrectionRequest(
            accessToken: String,
            request: WorkproofCorrectionRequestMutation
        ): WorkproofCorrectionSubmitResult = error("not used")
    }

    private class FakeCurrentLocationProvider(
        private val result: CurrentLocationResult,
        private val fetchDelayMillis: Long = 0L
    ) : CurrentLocationProvider {
        var fetchCount: Int = 0
        var requestedMaxLastKnownAccuracyMeters: Float? = null

        override suspend fun fetch(maxLastKnownAccuracyMeters: Float): CurrentLocationResult {
            fetchCount += 1
            requestedMaxLastKnownAccuracyMeters = maxLastKnownAccuracyMeters
            if (fetchDelayMillis > 0L) {
                delay(fetchDelayMillis)
            }
            return result
        }
    }

    private class FakeWageRepository : WageRepository {
        override suspend fun load(
            accessToken: String,
            month: YearMonth,
            asOf: LocalDate,
            paydayDay: Int
        ): WageRemoteState = WageRemoteState.empty("급여 데이터 없음")

        override suspend fun createDeposit(
            accessToken: String,
            month: YearMonth,
            depositDate: LocalDate,
            actualDepositAmount: Long,
            deductionsKnown: Boolean,
            note: String?
        ): WageDepositRecordPayload = error("not used")

        override suspend fun createVerification(
            accessToken: String,
            month: YearMonth,
            workplaceId: Long,
            actualDepositAmount: Long,
            deductionsKnown: Boolean,
            memo: String?
        ): WageVerificationCreatedPayload = error("not used")

        override suspend fun getVerificationDetail(
            accessToken: String,
            verificationId: Long
        ): WageVerificationDetailPayload = error("not used")
    }

    private class FakeVaultRepository(
        initialState: VaultRemoteState,
        private val stateAfterTerminalLoad: VaultRemoteState? = null,
        private val createDepositResult: VaultCreateTransactionPayload? = null,
        transactionDetails: List<VaultTransactionDetailPayload> = emptyList()
    ) : VaultRepository {
        private var currentState: VaultRemoteState = initialState
        private val queuedTransactionDetails = ArrayDeque(transactionDetails)

        override suspend fun load(accessToken: String): VaultRemoteState = currentState

        override suspend fun createDeposit(
            accessToken: String,
            amountAtomic: Long
        ): VaultCreateTransactionPayload {
            return createDepositResult ?: error("createDeposit should not be called without a prepared result")
        }

        override suspend fun createWithdrawal(
            accessToken: String,
            amountAtomic: Long
        ): VaultCreateTransactionPayload = error("not used")

        override suspend fun getTransactionDetail(
            accessToken: String,
            requestId: String
        ): VaultTransactionDetailPayload {
            val detail = queuedTransactionDetails.removeFirstOrNull()
                ?: error("No prepared vault transaction detail for $requestId")
            if (detail.status == "CONFIRMED" || detail.status == "FAILED" || detail.status == "TIMED_OUT") {
                stateAfterTerminalLoad?.let { currentState = it }
            }
            return detail
        }
    }

    private class FakeRemittanceRepository(
        private val result: RemittanceRemoteState,
        private val createdRecipient: RemittanceRecipientPayload? = null,
        private val resultAfterCreate: RemittanceRemoteState? = null,
        private val resultAfterUpdate: RemittanceRemoteState? = null,
        private val precheckResult: RemittanceTransferPrecheckPayload? = null,
        private val createTransferResult: RemittanceCreateTransferPayload? = null,
        transferDetailResults: List<RemittanceTransferDetailPayload> = emptyList(),
        private val resultAfterTerminalLoad: RemittanceRemoteState? = null
    ) : RemittanceRepository {
        private var currentResult: RemittanceRemoteState = result
        private val queuedTransferDetails = ArrayDeque(transferDetailResults)
        val loadedTokens = mutableListOf<String>()
        val createRecipientCalls = mutableListOf<CreateRecipientCall>()
        val updateRecipientCalls = mutableListOf<UpdateRecipientCall>()

        override suspend fun load(accessToken: String): RemittanceRemoteState {
            loadedTokens += accessToken
            return currentResult
        }

        override suspend fun searchRecipientsByPhone(
            accessToken: String,
            phoneNumber: String
        ): List<RemittanceRecipientSearchPayload> = emptyList()

        override suspend fun createRecipient(
            accessToken: String,
            alias: String,
            relation: String,
            walletAddress: String,
            targetUserId: Long?
        ): RemittanceRecipientPayload {
            createRecipientCalls += CreateRecipientCall(accessToken, alias, relation, walletAddress, targetUserId)
            resultAfterCreate?.let { currentResult = it }
            return createdRecipient ?: error("createRecipient should not be called without a prepared recipient")
        }

        override suspend fun updateRecipient(
            accessToken: String,
            recipientId: String,
            alias: String,
            relation: String,
            walletAddress: String
        ): RemittanceRecipientPayload {
            updateRecipientCalls += UpdateRecipientCall(accessToken, recipientId, alias, relation, walletAddress)
            resultAfterUpdate?.let { currentResult = it }
            return currentResult.payload?.recipients?.firstOrNull { it.recipientId == recipientId }
                ?: error("updateRecipient should be called with a prepared updated recipient")
        }

        override suspend fun precheck(
            accessToken: String,
            recipientId: String,
            amountAtomic: Long,
            highAmountConfirmed: Boolean,
            recentRecipientConfirmed: Boolean
        ): RemittanceTransferPrecheckPayload {
            return precheckResult ?: error("precheck should not be called without a prepared result")
        }

        override suspend fun createTransfer(
            accessToken: String,
            recipientId: String,
            amountAtomic: Long,
            highAmountConfirmed: Boolean,
            recentRecipientConfirmed: Boolean
        ): RemittanceCreateTransferPayload {
            resultAfterCreate?.let { currentResult = it }
            return createTransferResult ?: error("createTransfer should not be called without a prepared result")
        }

        override suspend fun getTransferDetail(
            accessToken: String,
            transferId: String
        ): RemittanceTransferDetailPayload {
            val detail = queuedTransferDetails.removeFirstOrNull()
                ?: error("getTransferDetail should not be called without a prepared result")
            if (detail.status == "CONFIRMED" || detail.status == "FAILED" || detail.status == "TIMED_OUT") {
                resultAfterTerminalLoad?.let { currentResult = it }
            }
            return detail
        }
    }

    private fun remittanceState(tokenBalanceAtomic: String): RemittanceRemoteState {
        return RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = tokenBalanceAtomic,
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = emptyList(),
                transfers = emptyList(),
                activeTransfer = null
            )
        )
    }

    private fun vaultState(
        walletTokenBalanceAtomic: String,
        storedAmountAtomic: String,
        availableToStoreAmountAtomic: String,
        latestTransaction: VaultTransactionDetailPayload?
    ): VaultRemoteState {
        return VaultRemoteState.content(
            VaultRemotePayload(
                summary = VaultSummaryPayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    vaultAddress = "0x2222222222222222222222222222222222222222",
                    network = "sepolia",
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    storedAmountAtomic = storedAmountAtomic,
                    accruedYieldAtomic = "0",
                    walletTokenBalanceAtomic = walletTokenBalanceAtomic,
                    availableToStoreAmountAtomic = availableToStoreAmountAtomic,
                    shareBalance = storedAmountAtomic,
                    interestPreview = VaultInterestPreviewPayload(
                        dailyEstimatedYieldAtomic = "0",
                        monthlyEstimatedYieldAtomic = "0",
                        yearlyEstimatedYieldAtomic = "0",
                        apyBps = 500
                    ),
                    disclaimer = "Vault values are demo-only estimates on testnet and do not guarantee real profit."
                ),
                latestTransaction = latestTransaction
            )
        )
    }
}
