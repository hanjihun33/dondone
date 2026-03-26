package com.dondone.mobile.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.util.Log
import com.dondone.mobile.BuildConfig
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val CURRENT_LOCATION_LOG_TAG = "CurrentLocationProvider"
private const val CURRENT_LOCATION_FETCH_BUDGET_MS = 4_000L
private const val CURRENT_LOCATION_TIMEOUT_NETWORK_MS = 1_200L
private const val CURRENT_LOCATION_TIMEOUT_GPS_MS = 2_500L
private const val CURRENT_LOCATION_TIMEOUT_PASSIVE_MS = 300L
private const val CURRENT_LOCATION_TIMEOUT_FALLBACK_MS = 1_000L
private const val CURRENT_LOCATION_MAX_CURRENT_AGE_GPS_MS = 20_000L
private const val CURRENT_LOCATION_MAX_CURRENT_AGE_NETWORK_MS = 30_000L
private const val CURRENT_LOCATION_MAX_CURRENT_AGE_PASSIVE_MS = 30_000L
private const val CURRENT_LOCATION_MAX_CURRENT_ACCURACY_METERS = 120f
private const val CURRENT_LOCATION_DEFAULT_MAX_LAST_KNOWN_ACCURACY_METERS = 1_000f
private const val CURRENT_LOCATION_MAX_LAST_KNOWN_AGE_MS = 10 * 60 * 1000L
private val CURRENT_LOCATION_PROVIDER_PRIORITY_FINE = listOf(
    LocationManager.NETWORK_PROVIDER,
    LocationManager.GPS_PROVIDER,
    LocationManager.PASSIVE_PROVIDER
)
private val CURRENT_LOCATION_PROVIDER_PRIORITY_COARSE = listOf(
    LocationManager.NETWORK_PROVIDER,
    LocationManager.PASSIVE_PROVIDER
)

data class CurrentLocationSnapshot(
    val latitude: Double,
    val longitude: Double
)

sealed interface CurrentLocationResult {
    data class Success(val location: CurrentLocationSnapshot) : CurrentLocationResult

    data object PermissionRequired : CurrentLocationResult

    data class Error(
        val reason: CurrentLocationErrorReason = CurrentLocationErrorReason.UNKNOWN
    ) : CurrentLocationResult
}

enum class CurrentLocationErrorReason {
    SERVICE_UNAVAILABLE,
    LOCATION_DISABLED,
    UNKNOWN
}

interface CurrentLocationProvider {
    suspend fun fetch(
        maxLastKnownAccuracyMeters: Float = CURRENT_LOCATION_DEFAULT_MAX_LAST_KNOWN_ACCURACY_METERS
    ): CurrentLocationResult
}

class AndroidCurrentLocationProvider(
    private val context: Context
) : CurrentLocationProvider {
    override suspend fun fetch(maxLastKnownAccuracyMeters: Float): CurrentLocationResult {
        if (!hasCurrentLocationPermission(context)) {
            debugLog("fetch: permission missing (fine/coarse)")
            return CurrentLocationResult.PermissionRequired
        }
        val hasFinePermission = hasFineLocationPermission(context)
        if (!hasFinePermission) {
            // 출퇴근 반경 판단에는 정밀 위치가 필요해 대략 위치 권한만으로는 조회를 진행하지 않는다.
            debugLog("fetch: fine location permission missing")
            return CurrentLocationResult.PermissionRequired
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: run {
                debugLog("fetch: location manager unavailable")
                return CurrentLocationResult.Error(CurrentLocationErrorReason.SERVICE_UNAVAILABLE)
            }
        val enabledProviders = locationManager.getProviders(true).toSet()
        val providersInPriorityOrder = locationProviderPriority(hasFinePermission)
            .filter(enabledProviders::contains)
        if (providersInPriorityOrder.isEmpty()) {
            debugLog("fetch: no enabled provider for fine location")
            return CurrentLocationResult.Error(CurrentLocationErrorReason.LOCATION_DISABLED)
        }
        debugLog("fetch: provider priority=${providersInPriorityOrder.joinToString(",")}")

        val currentLocation = requestCurrentLocation(
            locationManager = locationManager,
            providersInPriorityOrder = providersInPriorityOrder
        )
        if (currentLocation != null) {
            debugLog("fetch: using current location")
            return CurrentLocationResult.Success(
                CurrentLocationSnapshot(
                    latitude = currentLocation.latitude,
                    longitude = currentLocation.longitude
                )
            )
        }

        val lastKnownLocation = bestLastKnownLocation(
            locationManager = locationManager,
            hasFineLocationPermission = hasFinePermission,
            maxLastKnownAccuracyMeters = maxLastKnownAccuracyMeters
        )
        if (lastKnownLocation != null) {
            debugLog("fetch: using last known location ${lastKnownLocation.toDiagnosticText()}")
            return CurrentLocationResult.Success(
                CurrentLocationSnapshot(
                    latitude = lastKnownLocation.latitude,
                    longitude = lastKnownLocation.longitude
                )
            )
        }
        debugLog("fetch: failed to resolve usable location")
        return CurrentLocationResult.Error(CurrentLocationErrorReason.UNKNOWN)
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(
        locationManager: LocationManager,
        providersInPriorityOrder: List<String>
    ): Location? {
        val startedAtMs = System.currentTimeMillis()
        providersInPriorityOrder.forEach { provider ->
            val elapsedMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
            val remainingBudgetMs = (CURRENT_LOCATION_FETCH_BUDGET_MS - elapsedMs).coerceAtLeast(0L)
            if (remainingBudgetMs <= 0L) {
                debugLog("fetch: budget exhausted before provider=$provider")
                return null
            }
            val providerTimeoutMs = minOf(providerTimeoutMs(provider), remainingBudgetMs)
            val candidate = withTimeoutOrNull(providerTimeoutMs) {
                requestSingleLocation(locationManager, provider)
            }
            if (candidate != null) {
                debugLog("fetch: current[$provider] ${candidate.toDiagnosticText()}")
                if (isUsableCurrentLocation(candidate, provider)) {
                    return candidate
                }
                debugLog("fetch: current[$provider] rejected by age/accuracy policy")
            } else {
                debugLog("fetch: current[$provider] unavailable within timeout=${providerTimeoutMs}ms")
            }
        }
        val totalElapsedMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
        debugLog("fetch: no usable current location within budget=${CURRENT_LOCATION_FETCH_BUDGET_MS}ms, elapsed=${totalElapsedMs}ms")
        return null
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocation(
        locationManager: LocationManager,
        provider: String
    ): Location? = suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
        runCatching {
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(context)
            ) { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
        }.onFailure {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
        continuation.invokeOnCancellation { cancellationSignal.cancel() }
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnownLocation(
        locationManager: LocationManager,
        hasFineLocationPermission: Boolean,
        maxLastKnownAccuracyMeters: Float
    ): Location? {
        val enabledProviders = locationManager.getProviders(true).toSet()
        val providersInPriorityOrder = locationProviderPriority(hasFineLocationPermission)
            .filter(enabledProviders::contains)

        providersInPriorityOrder.forEach { provider ->
            val candidate = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            if (candidate != null) {
                debugLog("lastKnown[$provider]: ${candidate.toDiagnosticText()}")
            }
            if (
                candidate != null &&
                isUsableLastKnownLocation(
                    location = candidate,
                    maxLastKnownAccuracyMeters = maxLastKnownAccuracyMeters
                )
            ) {
                return candidate
            }
        }
        debugLog("lastKnown: no usable candidate (maxAccuracy=${maxLastKnownAccuracyMeters}m)")
        return null
    }

    private fun isUsableLastKnownLocation(
        location: Location,
        maxLastKnownAccuracyMeters: Float
    ): Boolean {
        val ageMs = (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
        if (ageMs > CURRENT_LOCATION_MAX_LAST_KNOWN_AGE_MS) {
            return false
        }
        if (!location.hasAccuracy()) {
            return false
        }
        return location.accuracy > 0f && location.accuracy <= maxLastKnownAccuracyMeters
    }

    private fun isUsableCurrentLocation(location: Location, provider: String): Boolean {
        val ageMs = (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
        val maxAgeMs = currentLocationMaxAgeMs(provider)
        if (ageMs > maxAgeMs) {
            return false
        }
        if (!location.hasAccuracy()) {
            return false
        }
        return location.accuracy > 0f && location.accuracy <= CURRENT_LOCATION_MAX_CURRENT_ACCURACY_METERS
    }
}

private fun currentLocationMaxAgeMs(provider: String): Long {
    return when (provider) {
        LocationManager.NETWORK_PROVIDER -> CURRENT_LOCATION_MAX_CURRENT_AGE_NETWORK_MS
        LocationManager.GPS_PROVIDER -> CURRENT_LOCATION_MAX_CURRENT_AGE_GPS_MS
        LocationManager.PASSIVE_PROVIDER -> CURRENT_LOCATION_MAX_CURRENT_AGE_PASSIVE_MS
        else -> CURRENT_LOCATION_MAX_CURRENT_AGE_GPS_MS
    }
}

private fun providerTimeoutMs(provider: String): Long {
    return when (provider) {
        LocationManager.NETWORK_PROVIDER -> CURRENT_LOCATION_TIMEOUT_NETWORK_MS
        LocationManager.GPS_PROVIDER -> CURRENT_LOCATION_TIMEOUT_GPS_MS
        LocationManager.PASSIVE_PROVIDER -> CURRENT_LOCATION_TIMEOUT_PASSIVE_MS
        else -> CURRENT_LOCATION_TIMEOUT_FALLBACK_MS
    }
}

private fun Location.toDiagnosticText(): String {
    val ageMs = (System.currentTimeMillis() - time).coerceAtLeast(0L)
    val latRounded = (latitude * 1000.0).toInt() / 1000.0
    val lngRounded = (longitude * 1000.0).toInt() / 1000.0
    val accuracyText = if (hasAccuracy()) accuracy.toString() else "none"
    return "lat=$latRounded,lng=$lngRounded,accuracy=$accuracyText,ageMs=$ageMs"
}

private fun debugLog(message: String) {
    if (BuildConfig.DEBUG) {
        Log.d(CURRENT_LOCATION_LOG_TAG, message)
    }
}

class UnavailableCurrentLocationProvider(
    private val reason: CurrentLocationErrorReason = CurrentLocationErrorReason.SERVICE_UNAVAILABLE
) : CurrentLocationProvider {
    override suspend fun fetch(maxLastKnownAccuracyMeters: Float): CurrentLocationResult =
        CurrentLocationResult.Error(reason)
}

internal fun locationProviderPriority(hasFineLocationPermission: Boolean): List<String> {
    return if (hasFineLocationPermission) {
        CURRENT_LOCATION_PROVIDER_PRIORITY_FINE
    } else {
        CURRENT_LOCATION_PROVIDER_PRIORITY_COARSE
    }
}

internal fun selectPreferredLocationProvider(
    enabledProviders: Collection<String>,
    hasFineLocationPermission: Boolean
): String? {
    val priority = locationProviderPriority(hasFineLocationPermission)
    return priority.firstOrNull(enabledProviders::contains)
}

fun hasFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

fun hasCurrentLocationPermission(context: Context): Boolean {
    val fineGranted = hasFineLocationPermission(context)
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}
