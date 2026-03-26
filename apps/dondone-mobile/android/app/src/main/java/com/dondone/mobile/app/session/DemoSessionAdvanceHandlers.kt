package com.dondone.mobile.app.session

import com.dondone.mobile.data.advance.AdvanceRemoteState
import com.dondone.mobile.data.advance.AdvanceRepository
import com.dondone.mobile.data.advance.AdvanceRequestDetailPayload
import com.dondone.mobile.data.advance.AdvanceUnauthorizedException
import com.dondone.mobile.data.auth.AuthSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class DemoSessionAdvanceHandlers(
    private val scope: CoroutineScope,
    private val advanceRepository: AdvanceRepository,
    private val authUiStateFlow: MutableStateFlow<AuthUiState>,
    private val advanceRemoteStateFlow: MutableStateFlow<AdvanceRemoteState>,
    private val selectedAdvanceAmountFlow: MutableStateFlow<Int?>,
    private val advanceRequestUiStateFlow: MutableStateFlow<AdvanceRequestUiState>,
    private val advanceRequestDetailUiStateFlow: MutableStateFlow<AdvanceRequestDetailUiState>,
    private val unauthenticatedMessage: String,
    private val loadAdvanceRemoteState: suspend (AuthSession) -> Unit,
    private val mergeAdvanceRequestDetail: (AdvanceRequestDetailPayload) -> Unit,
    private val expireSession: suspend (String?) -> Unit
) {
    fun selectAdvanceAmount(amount: Int) {
        selectedAdvanceAmountFlow.value = amount
        if (advanceRequestUiStateFlow.value.message != null) {
            advanceRequestUiStateFlow.value = AdvanceRequestUiState()
        }
    }

    fun clearAdvanceRequestMessage() {
        if (!advanceRequestUiStateFlow.value.isSubmitting && advanceRequestUiStateFlow.value.message != null) {
            advanceRequestUiStateFlow.value = AdvanceRequestUiState()
        }
    }

    fun openAdvanceRequestDetail(requestId: Long) {
        val cached = advanceRemoteStateFlow.value.requestDetailsById[requestId]
        if (cached != null) {
            advanceRequestDetailUiStateFlow.value = AdvanceRequestDetailUiState(detail = cached)
            return
        }

        val session = authUiStateFlow.value.session ?: run {
            advanceRequestDetailUiStateFlow.value = AdvanceRequestDetailUiState(
                errorMessage = "로그인 후 다시 시도해 주세요."
            )
            return
        }

        scope.launch {
            advanceRequestDetailUiStateFlow.value = AdvanceRequestDetailUiState(isLoading = true)
            try {
                val detail = advanceRepository.getRequestDetail(session.accessToken, requestId)
                mergeAdvanceRequestDetail(detail)
                advanceRequestDetailUiStateFlow.value = AdvanceRequestDetailUiState(detail = detail)
            } catch (error: AdvanceUnauthorizedException) {
                expireSession(error.message)
                advanceRequestDetailUiStateFlow.value = AdvanceRequestDetailUiState(
                    errorMessage = error.message ?: "세션이 만료되어 다시 로그인해 주세요."
                )
            } catch (error: Exception) {
                advanceRequestDetailUiStateFlow.value = AdvanceRequestDetailUiState(
                    errorMessage = error.message ?: "미리받기 상세를 불러오지 못했어요."
                )
            }
        }
    }

    fun closeAdvanceRequestDetail() {
        advanceRequestDetailUiStateFlow.value = AdvanceRequestDetailUiState()
    }

    fun requestAdvance() {
        val session = authUiStateFlow.value.session ?: run {
            advanceRequestUiStateFlow.value = AdvanceRequestUiState(
                message = "로그인 후 다시 시도해 주세요.",
                isError = true
            )
            return
        }
        val eligibility = advanceRemoteStateFlow.value.eligibility ?: run {
            advanceRequestUiStateFlow.value = AdvanceRequestUiState(
                message = "근무 조건을 다시 확인한 뒤 시도해 주세요.",
                isError = true
            )
            return
        }

        val availableAmount = eligibility.availableAmountInWholeAssetUnits
        val requestedAmount = (selectedAdvanceAmountFlow.value ?: availableAmount)
            .coerceAtMost(availableAmount)
        if (requestedAmount <= 0) {
            advanceRequestUiStateFlow.value = AdvanceRequestUiState(
                message = "신청 가능한 금액이 없어요.",
                isError = true
            )
            return
        }

        scope.launch {
            advanceRequestUiStateFlow.value = AdvanceRequestUiState(isSubmitting = true)
            try {
                val result = advanceRepository.createRequest(
                    accessToken = session.accessToken,
                    workplaceId = eligibility.workplaceId,
                    requestedAmountAtomic = requestedAmount.toAtomicAmount(eligibility.assetDecimals)
                )
                val detail = advanceRepository.getRequestDetail(session.accessToken, result.requestId)
                loadAdvanceRemoteState(session)
                mergeAdvanceRequestDetail(detail)
                advanceRequestDetailUiStateFlow.value = AdvanceRequestDetailUiState(detail = detail)
                val successMessage = if (result.status == "SUBMITTED") {
                    "미리받기 신청이 접수되었어요. 관리자 승인 후 금액이 확정됩니다."
                } else {
                    "미리받기 신청이 반영되었어요. ${result.status} · ${result.approvedAmount ?: 0L}원"
                }
                advanceRequestUiStateFlow.value = AdvanceRequestUiState(
                    message = successMessage,
                    isError = false
                )
            } catch (error: AdvanceUnauthorizedException) {
                expireSession(error.message)
                advanceRequestUiStateFlow.value = AdvanceRequestUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                advanceRequestUiStateFlow.value = AdvanceRequestUiState(
                    message = error.message ?: "미리받기 신청에 실패했어요. 잠시 후 다시 시도해 주세요.",
                    isError = true
                )
            }
        }
    }

    fun refreshAdvanceRemoteState() {
        val session = authUiStateFlow.value.session
        if (session == null) {
            advanceRemoteStateFlow.value = AdvanceRemoteState.unauthenticated(unauthenticatedMessage)
            return
        }

        scope.launch {
            loadAdvanceRemoteState(session)
        }
    }
}
