package com.dondone.mobile.data.workproof

import java.time.LocalDate
import java.time.LocalDateTime

data class WorkproofWorkplacePayload(
    val workplaceId: Long,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val allowedRadiusMeters: Int
)

data class WorkproofRecordPayload(
    val recordId: Long,
    val workDate: LocalDate,
    val status: String,
    val checkInDeviceAt: LocalDateTime,
    val checkOutDeviceAt: LocalDateTime?,
    val workedMinutes: Long?,
    val modified: Boolean,
    val reflectionStatus: String,
    val riskFlags: List<String>
)

data class WorkproofRemotePayload(
    val workplace: WorkproofWorkplacePayload,
    val records: List<WorkproofRecordPayload>
)

enum class WorkproofRemoteMode {
    LOADING,
    UNAUTHENTICATED,
    EMPTY,
    ERROR,
    CONTENT
}

data class WorkproofRemoteState(
    val mode: WorkproofRemoteMode,
    val payload: WorkproofRemotePayload? = null,
    val errorMessage: String? = null
) {
    val isAuthenticated: Boolean
        get() = mode != WorkproofRemoteMode.UNAUTHENTICATED

    val isLoading: Boolean
        get() = mode == WorkproofRemoteMode.LOADING

    companion object {
        fun loading() = WorkproofRemoteState(mode = WorkproofRemoteMode.LOADING)

        fun unauthenticated(message: String) = WorkproofRemoteState(
            mode = WorkproofRemoteMode.UNAUTHENTICATED,
            errorMessage = message
        )

        fun empty(message: String) = WorkproofRemoteState(
            mode = WorkproofRemoteMode.EMPTY,
            errorMessage = message
        )

        fun error(message: String) = WorkproofRemoteState(
            mode = WorkproofRemoteMode.ERROR,
            errorMessage = message
        )

        fun content(payload: WorkproofRemotePayload) = WorkproofRemoteState(
            mode = WorkproofRemoteMode.CONTENT,
            payload = payload
        )
    }
}
