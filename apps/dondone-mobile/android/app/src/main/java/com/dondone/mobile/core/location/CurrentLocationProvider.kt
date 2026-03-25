package com.dondone.mobile.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val CURRENT_LOCATION_TIMEOUT_MS = 8_000L
private const val CURRENT_LOCATION_MAX_LAST_KNOWN_AGE_MS = 120_000L
private const val CURRENT_LOCATION_MAX_LAST_KNOWN_ACCURACY_METERS = 120f
private val CURRENT_LOCATION_PROVIDER_PRIORITY = listOf(
    LocationManager.GPS_PROVIDER,
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
    suspend fun fetch(): CurrentLocationResult
}

class AndroidCurrentLocationProvider(
    private val context: Context
) : CurrentLocationProvider {
    override suspend fun fetch(): CurrentLocationResult {
        if (!hasCurrentLocationPermission(context)) {
            return CurrentLocationResult.PermissionRequired
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return CurrentLocationResult.Error(CurrentLocationErrorReason.SERVICE_UNAVAILABLE)
        val provider = preferredProvider(locationManager)
            ?: return CurrentLocationResult.Error(CurrentLocationErrorReason.LOCATION_DISABLED)

        val lastKnownLocation = bestLastKnownLocation(locationManager)
        if (lastKnownLocation != null && isUsableLastKnownLocation(lastKnownLocation)) {
            return CurrentLocationResult.Success(
                CurrentLocationSnapshot(
                    latitude = lastKnownLocation.latitude,
                    longitude = lastKnownLocation.longitude
                )
            )
        }
        val location = withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT_MS) {
            requestSingleLocation(locationManager, provider) ?: lastKnownLocation
        } ?: lastKnownLocation

        return if (location != null) {
            CurrentLocationResult.Success(
                CurrentLocationSnapshot(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )
        } else {
            CurrentLocationResult.Error(CurrentLocationErrorReason.UNKNOWN)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocation(
        locationManager: LocationManager,
        provider: String
    ): Location? = suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
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
        continuation.invokeOnCancellation { cancellationSignal.cancel() }
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnownLocation(locationManager: LocationManager): Location? {
        val candidates = locationManager.getProviders(true)
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
        return candidates.minByOrNull(Location::getAccuracy)
            ?: candidates.maxByOrNull(Location::getTime)
    }

    private fun preferredProvider(locationManager: LocationManager): String? {
        val enabledProviders = locationManager.getProviders(true)
        return CURRENT_LOCATION_PROVIDER_PRIORITY.firstOrNull(enabledProviders::contains)
    }

    private fun isUsableLastKnownLocation(location: Location): Boolean {
        val ageMs = (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
        if (ageMs > CURRENT_LOCATION_MAX_LAST_KNOWN_AGE_MS) {
            return false
        }
        return !location.hasAccuracy() || location.accuracy <= CURRENT_LOCATION_MAX_LAST_KNOWN_ACCURACY_METERS
    }
}

class UnavailableCurrentLocationProvider(
    private val reason: CurrentLocationErrorReason = CurrentLocationErrorReason.SERVICE_UNAVAILABLE
) : CurrentLocationProvider {
    override suspend fun fetch(): CurrentLocationResult = CurrentLocationResult.Error(reason)
}

fun hasCurrentLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}
