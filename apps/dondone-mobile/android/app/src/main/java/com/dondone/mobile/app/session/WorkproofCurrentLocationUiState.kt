package com.dondone.mobile.app.session

enum class WorkproofCurrentLocationStatus {
    IDLE,
    LOADING,
    READY,
    PERMISSION_REQUIRED,
    SERVICE_UNAVAILABLE,
    LOCATION_DISABLED,
    ERROR
}

data class WorkproofCurrentLocationUiState(
    val status: WorkproofCurrentLocationStatus = WorkproofCurrentLocationStatus.IDLE
) {
    val hasUsableLocation: Boolean
        get() = status == WorkproofCurrentLocationStatus.READY
}
