package com.dondone.mobile.app.session

import com.dondone.mobile.data.advance.AdvanceRemoteState
import com.dondone.mobile.data.advance.AdvanceRepository
import com.dondone.mobile.data.advance.AdvanceCreateResult
import com.dondone.mobile.data.advance.AdvanceRequestDetailPayload
import com.dondone.mobile.data.auth.AuthRepository
import com.dondone.mobile.data.auth.AuthSession
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
        viewModel.signup("새 사용자", "new@example.com", "password123")
        advanceUntilIdle()

        assertEquals(1, authRepository.signupCalls.size)
        assertEquals(Triple("새 사용자", "new@example.com", "password123"), authRepository.signupCalls.single())
        assertTrue(viewModel.authUiState.value.isAuthenticated)
        assertEquals(session.accessToken, viewModel.authUiState.value.session?.accessToken)
        assertEquals(listOf(session.accessToken), advanceRepository.loadedTokens)
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

    private fun testSession(
        accessToken: String = "token-123",
        email: String = "test@gmail.com"
    ) = AuthSession(
        accessToken = accessToken,
        tokenType = "Bearer",
        expiresAtEpochMillis = Long.MAX_VALUE,
        userId = 7L,
        email = email,
        name = "테스트 사용자"
    )

    private class FakeAuthRepository(
        private val restoredSession: AuthSession? = null,
        private val loginSession: AuthSession? = null,
        private val signupSession: AuthSession? = null
    ) : AuthRepository {
        val loginCalls = mutableListOf<Pair<String, String>>()
        val signupCalls = mutableListOf<Triple<String, String, String>>()
        var logoutCalled: Boolean = false

        override suspend fun restore(): AuthSession? = restoredSession

        override suspend fun signup(name: String, email: String, password: String): AuthSession {
            signupCalls += Triple(name, email, password)
            return signupSession ?: error("signup should not be called without a prepared session")
        }

        override suspend fun login(email: String, password: String): AuthSession {
            loginCalls += email to password
            return loginSession ?: error("login should not be called without a prepared session")
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
}
