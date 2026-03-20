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
import com.dondone.mobile.data.workproof.WorkproofRemotePayload
import com.dondone.mobile.data.workproof.WorkproofRemoteState
import com.dondone.mobile.data.workproof.WorkproofRepository
import com.dondone.mobile.data.workproof.WorkproofWorkplacePayload
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.domain.model.WorkproofData
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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
                availableAmount = 180_000L,
                repaymentTier = "T2",
                blockReasonCodes = emptyList(),
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
        assertEquals(listOf(Triple(session.accessToken, 1L, 100_000L)), advanceRepository.createCalls)
        assertEquals("미리받기 신청이 반영됐어요. APPROVED · 100000원", viewModel.advanceRequestUiState.value.message)
        assertFalse(viewModel.advanceRequestUiState.value.isError)
        assertEquals(1L, viewModel.advanceRequestDetailUiState.value.detail?.requestId)
        assertTrue(viewModel.advanceRemoteState.value.requests.any { it.requestId == 1L })
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
            remittanceRepository = remittanceRepository
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

    private class FakeAuthRepository(
        private val restoredSession: AuthSession? = null,
        private val loginSession: AuthSession? = null,
        private val signupSession: AuthSession? = null,
        private val updateProfileSession: AuthSession? = null,
        private val updateCompanyCodeSession: AuthSession? = null,
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

        override suspend fun updateCompanyCode(session: AuthSession, companyCode: String): AuthSession {
            return updateCompanyCodeSession ?: session.copy(companyCode = companyCode)
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
                availableAmount = 180_000L,
                repaymentTier = "T2",
                blockReasonCodes = emptyList(),
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
            requestedAmount: Long
        ): AdvanceCreateResult {
            createCalls += Triple(accessToken, workplaceId, requestedAmount)
            return AdvanceCreateResult(
                requestId = 1L,
                status = "APPROVED",
                approvedAmount = requestedAmount,
                feeAmount = 0L,
                repaymentDueDate = "2026-03-25"
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
                requestedAmount = 100_000L,
                approvedAmount = 100_000L,
                feeAmount = 0L,
                status = "APPROVED",
                repaymentDueDate = "2026-03-25",
                eligibilitySnapshot = com.dondone.mobile.data.advance.AdvanceEligibilitySnapshotPayload(
                    availableAmount = 180_000L,
                    maxCap = 500_000L,
                    policyRate = "0.35",
                    reflectedWorkDays = 5,
                    reflectedWorkMinutes = 2_400L,
                    needsReviewRecordCount = 0
                ),
                createdAt = "2026-03-16T09:00:00"
            )
        }
    }

    private class FakeWorkproofRepository : WorkproofRepository {
        override suspend fun load(accessToken: String): WorkproofRemoteState {
            return WorkproofRemoteState.content(
                WorkproofRemotePayload(
                    workplace = WorkproofWorkplacePayload(
                        workplaceId = 1L,
                        name = "DonDone Cafe",
                        address = "서울시 강남구 테헤란로",
                        latitude = 37.5013,
                        longitude = 127.0396,
                        allowedRadiusMeters = 100
                    ),
                    records = emptyList()
                )
            )
        }

        override suspend fun clockIn(accessToken: String, workproof: WorkproofData): WorkproofRemoteState {
            return load(accessToken)
        }

        override suspend fun clockOut(accessToken: String, workproof: WorkproofData): WorkproofRemoteState {
            return load(accessToken)
        }
    }

    private class FakeRemittanceRepository(
        private val result: RemittanceRemoteState,
        private val createdRecipient: RemittanceRecipientPayload? = null,
        private val resultAfterCreate: RemittanceRemoteState? = null,
        private val resultAfterUpdate: RemittanceRemoteState? = null
    ) : RemittanceRepository {
        private var currentResult: RemittanceRemoteState = result
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
        ): RemittanceTransferPrecheckPayload = error("not used")

        override suspend fun createTransfer(
            accessToken: String,
            recipientId: String,
            amountAtomic: Long,
            highAmountConfirmed: Boolean,
            recentRecipientConfirmed: Boolean
        ): RemittanceCreateTransferPayload = error("not used")

        override suspend fun getTransferDetail(
            accessToken: String,
            transferId: String
        ): RemittanceTransferDetailPayload = error("not used")
    }
}
