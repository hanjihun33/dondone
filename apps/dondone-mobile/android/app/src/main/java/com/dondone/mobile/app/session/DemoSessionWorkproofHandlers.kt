package com.dondone.mobile.app.session

import android.content.Context
import android.util.Log
import com.dondone.mobile.BuildConfig
import com.dondone.mobile.R
import com.dondone.mobile.core.location.CurrentLocationErrorReason
import com.dondone.mobile.core.location.CurrentLocationProvider
import com.dondone.mobile.core.location.CurrentLocationResult
import com.dondone.mobile.data.auth.AuthSession
import com.dondone.mobile.data.workproof.WorkproofRemoteMode
import com.dondone.mobile.data.workproof.WorkproofRemoteState
import com.dondone.mobile.data.workproof.WorkproofRepository
import com.dondone.mobile.data.workproof.WorkproofUnauthorizedException
import com.dondone.mobile.domain.model.DemoState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

private const val WORKPROOF_LOCATION_LOADING_FALLBACK = "현재 위치를 확인 중이에요."
private const val WORKPROOF_LOCATION_PERMISSION_REQUIRED_FALLBACK = "위치 권한을 허용하면 현재 위치를 확인할 수 있어요."
private const val WORKPROOF_LOCATION_SERVICE_UNAVAILABLE_FALLBACK = "위치 서비스를 사용할 수 없어요."
private const val WORKPROOF_LOCATION_DISABLED_FALLBACK = "위치 서비스가 꺼져 있어 현재 위치를 확인할 수 없어요."
private const val WORKPROOF_LOCATION_ERROR_FALLBACK = "현재 위치를 확인하지 못했어요. 위치 서비스와 GPS를 확인해 주세요."
private const val WORKPROOF_LOCATION_TRY_AGAIN_FALLBACK = "현재 위치를 확인한 뒤 다시 시도해 주세요."
private const val WORKPROOF_HANDLER_LOG_TAG = "WorkproofHandlers"
private const val WORKPROOF_SUBMIT_LOCATION_FRESHNESS_WINDOW_MS = 30_000L
private const val WORKPROOF_LAST_KNOWN_ACCURACY_MIN_METERS = 100f
private const val WORKPROOF_LAST_KNOWN_ACCURACY_MAX_METERS = 1_000f

internal class DemoSessionWorkproofHandlers(
    private val appContext: Context?,
    private val scope: CoroutineScope,
    private val currentLocationProvider: CurrentLocationProvider,
    private val workproofRepository: WorkproofRepository,
    private val uiStateFlow: MutableStateFlow<DemoState>,
    private val authUiStateFlow: MutableStateFlow<AuthUiState>,
    private val workproofRemoteStateFlow: MutableStateFlow<WorkproofRemoteState>,
    private val workproofActionUiStateFlow: MutableStateFlow<WorkproofActionUiState>,
    private val workproofCurrentLocationUiStateFlow: MutableStateFlow<WorkproofCurrentLocationUiState>,
    private val applyWorkproofRemoteState: suspend (WorkproofRemoteState) -> Unit,
    private val expireSession: suspend (String?) -> Unit
) {
    private val inFlightCurrentLocationRefresh = AtomicReference<Deferred<Boolean>?>(null)

    fun refreshWorkproofCurrentLocation() {
        scope.launch {
            refreshWorkproofCurrentLocationInternal()
        }
    }

    fun clockIn() {
        submitWorkproofAction(
            fallback = { state -> DemoSessionReducer.clockIn(state) },
            remoteCall = { session -> workproofRepository.clockIn(session.accessToken, uiStateFlow.value.workproof) },
            successMessage = "출근 기록이 백엔드에 저장됐어요.",
            failureMessage = "출근 기록을 저장하지 못했어요."
        )
    }

    fun clockOut() {
        submitWorkproofAction(
            fallback = { state -> DemoSessionReducer.clockOut(state) },
            remoteCall = { session -> workproofRepository.clockOut(session.accessToken, uiStateFlow.value.workproof) },
            successMessage = "퇴근 기록이 백엔드에 저장됐어요.",
            failureMessage = "퇴근 기록을 저장하지 못했어요."
        )
    }

    private fun submitWorkproofAction(
        fallback: (DemoState) -> DemoState,
        remoteCall: suspend (AuthSession) -> WorkproofRemoteState,
        successMessage: String,
        failureMessage: String
    ) {
        val session = authUiStateFlow.value.session
        if (session == null) {
            debugLog("submitWorkproofAction: unauthenticated, applying local fallback reducer")
            uiStateFlow.update { state -> fallback(state) }
            return
        }

        scope.launch {
            workproofActionUiStateFlow.value = WorkproofActionUiState(isSubmitting = true)
            try {
                val nowMillis = System.currentTimeMillis()
                val shouldReuseCurrentLocation = workproofCurrentLocationUiStateFlow.value.isFresh(
                    nowMillis = nowMillis,
                    freshnessWindowMillis = WORKPROOF_SUBMIT_LOCATION_FRESHNESS_WINDOW_MS
                )
                if (!shouldReuseCurrentLocation && !refreshWorkproofCurrentLocationInternal()) {
                    workproofActionUiStateFlow.value = WorkproofActionUiState(
                        message = currentLocationStatusMessage(workproofCurrentLocationUiStateFlow.value.status),
                        isError = true
                    )
                    return@launch
                }
                if (shouldReuseCurrentLocation) {
                    debugLog(
                        "submitWorkproofAction: reuse recent current location (<=${WORKPROOF_SUBMIT_LOCATION_FRESHNESS_WINDOW_MS}ms)"
                    )
                }
                val remoteState = remoteCall(session)
                if (remoteState.mode != WorkproofRemoteMode.CONTENT) {
                    debugLog("submitWorkproofAction: remote mode=${remoteState.mode}, error=${remoteState.errorMessage}")
                    workproofRemoteStateFlow.value = remoteState
                    workproofActionUiStateFlow.value = WorkproofActionUiState(
                        message = remoteState.errorMessage ?: failureMessage,
                        isError = true
                    )
                    return@launch
                }
                applyWorkproofRemoteState(remoteState)
                debugLog("submitWorkproofAction: remote success, mode=${remoteState.mode}")
                workproofActionUiStateFlow.value = WorkproofActionUiState(message = successMessage)
            } catch (error: WorkproofUnauthorizedException) {
                debugLog("submitWorkproofAction: unauthorized ${error.message}")
                expireSession(error.message)
                workproofActionUiStateFlow.value = WorkproofActionUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                debugLog("submitWorkproofAction: failure ${error.message}")
                workproofActionUiStateFlow.value = WorkproofActionUiState(
                    message = error.message ?: failureMessage,
                    isError = true
                )
            }
        }
    }

    private suspend fun refreshWorkproofCurrentLocationInternal(): Boolean {
        while (true) {
            val existingRefresh = inFlightCurrentLocationRefresh.get()
            if (existingRefresh != null) {
                debugLog("refreshCurrentLocation: join in-flight refresh")
                return existingRefresh.await()
            }

            val newRefresh = scope.async(start = CoroutineStart.LAZY) {
                runWorkproofCurrentLocationRefresh()
            }
            if (inFlightCurrentLocationRefresh.compareAndSet(null, newRefresh)) {
                return try {
                    newRefresh.await()
                } finally {
                    inFlightCurrentLocationRefresh.compareAndSet(newRefresh, null)
                }
            }
            newRefresh.cancel()
        }
    }

    private suspend fun runWorkproofCurrentLocationRefresh(): Boolean {
        workproofCurrentLocationUiStateFlow.value = WorkproofCurrentLocationUiState(
            status = WorkproofCurrentLocationStatus.LOADING
        )
        debugLog("refreshCurrentLocation: started")

        val maxLastKnownAccuracyMeters = uiStateFlow.value.workproof.allowedRadiusMeters
            .toFloat()
            .coerceIn(
                WORKPROOF_LAST_KNOWN_ACCURACY_MIN_METERS,
                WORKPROOF_LAST_KNOWN_ACCURACY_MAX_METERS
            )
        debugLog("refreshCurrentLocation: lastKnown maxAccuracy=${maxLastKnownAccuracyMeters}m")

        return when (
            val result = currentLocationProvider.fetch(
                maxLastKnownAccuracyMeters = maxLastKnownAccuracyMeters
            )
        ) {
            is CurrentLocationResult.Success -> {
                uiStateFlow.update { state ->
                    state.copy(
                        workproof = state.workproof.copy(
                            currentLatitude = result.location.latitude,
                            currentLongitude = result.location.longitude
                        )
                    )
                }
                debugLog(
                    "refreshCurrentLocation: success lat=${result.location.latitude},lng=${result.location.longitude}"
                )
                workproofCurrentLocationUiStateFlow.value = WorkproofCurrentLocationUiState(
                    status = WorkproofCurrentLocationStatus.READY,
                    lastResolvedAtMillis = System.currentTimeMillis()
                )
                true
            }

            CurrentLocationResult.PermissionRequired -> {
                debugLog("refreshCurrentLocation: permission required")
                workproofCurrentLocationUiStateFlow.value = WorkproofCurrentLocationUiState(
                    status = WorkproofCurrentLocationStatus.PERMISSION_REQUIRED
                )
                false
            }

            is CurrentLocationResult.Error -> {
                debugLog("refreshCurrentLocation: error reason=${result.reason}")
                workproofCurrentLocationUiStateFlow.value = WorkproofCurrentLocationUiState(
                    status = result.reason.toUiStatus()
                )
                false
            }
        }
    }

    private fun CurrentLocationErrorReason.toUiStatus(): WorkproofCurrentLocationStatus {
        return when (this) {
            CurrentLocationErrorReason.SERVICE_UNAVAILABLE -> WorkproofCurrentLocationStatus.SERVICE_UNAVAILABLE
            CurrentLocationErrorReason.LOCATION_DISABLED -> WorkproofCurrentLocationStatus.LOCATION_DISABLED
            CurrentLocationErrorReason.UNKNOWN -> WorkproofCurrentLocationStatus.ERROR
        }
    }

    private fun currentLocationStatusMessage(status: WorkproofCurrentLocationStatus): String {
        val stringRes = when (status) {
            WorkproofCurrentLocationStatus.LOADING -> R.string.workproof_location_status_loading
            WorkproofCurrentLocationStatus.PERMISSION_REQUIRED -> R.string.workproof_location_status_permission_required
            WorkproofCurrentLocationStatus.SERVICE_UNAVAILABLE -> R.string.workproof_location_status_service_unavailable
            WorkproofCurrentLocationStatus.LOCATION_DISABLED -> R.string.workproof_location_status_disabled
            WorkproofCurrentLocationStatus.ERROR -> R.string.workproof_location_status_error
            WorkproofCurrentLocationStatus.IDLE,
            WorkproofCurrentLocationStatus.READY -> R.string.workproof_location_status_try_again
        }
        return appContext?.getString(stringRes)
            ?: when (status) {
                WorkproofCurrentLocationStatus.LOADING -> WORKPROOF_LOCATION_LOADING_FALLBACK
                WorkproofCurrentLocationStatus.PERMISSION_REQUIRED -> WORKPROOF_LOCATION_PERMISSION_REQUIRED_FALLBACK
                WorkproofCurrentLocationStatus.SERVICE_UNAVAILABLE -> WORKPROOF_LOCATION_SERVICE_UNAVAILABLE_FALLBACK
                WorkproofCurrentLocationStatus.LOCATION_DISABLED -> WORKPROOF_LOCATION_DISABLED_FALLBACK
                WorkproofCurrentLocationStatus.ERROR -> WORKPROOF_LOCATION_ERROR_FALLBACK
                WorkproofCurrentLocationStatus.IDLE,
                WorkproofCurrentLocationStatus.READY -> WORKPROOF_LOCATION_TRY_AGAIN_FALLBACK
            }
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(WORKPROOF_HANDLER_LOG_TAG, message)
        }
    }
}
