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
    val status: WorkproofCurrentLocationStatus = WorkproofCurrentLocationStatus.IDLE,
    val lastResolvedAtMillis: Long? = null
) {
    val hasUsableLocation: Boolean
        get() = status == WorkproofCurrentLocationStatus.READY

    fun isFresh(nowMillis: Long, freshnessWindowMillis: Long): Boolean {
        if (status != WorkproofCurrentLocationStatus.READY) {
            return false
        }
        val resolvedAt = lastResolvedAtMillis ?: return false
        val ageMillis = (nowMillis - resolvedAt).coerceAtLeast(0L)
        return ageMillis <= freshnessWindowMillis
    }
}
