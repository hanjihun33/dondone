package com.dondone.mobile.feature.workproof.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dondone.mobile.BuildConfig
import com.dondone.mobile.R
import com.dondone.mobile.app.session.WorkproofCurrentLocationStatus
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.DonDoneErrorPanel
import com.dondone.mobile.core.map.KakaoMapSupport
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.shape.DotPoints
import com.kakao.vectormap.shape.PolygonOptions
import com.kakao.vectormap.shape.PolygonStyles
import com.kakao.vectormap.shape.PolygonStylesSet

@Composable
internal fun WorkproofWorkplaceMapCard(
    uiModel: WorkproofSummaryUiModel,
    onRefreshCurrentLocation: () -> Unit
) {
    val isKakaoMapAvailable = remember {
        KakaoMapSupport.isMapAvailable(BuildConfig.KAKAO_NATIVE_APP_KEY)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = stringResource(R.string.workproof_location_section_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )

        if (!isKakaoMapAvailable) {
            WorkproofMapFallbackCard(
                hasApiKey = BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank(),
                isRuntimeSupported = KakaoMapSupport.isRuntimeSupported()
            )
        } else {
            KakaoWorkplaceMapView(
                workplaceLatitude = uiModel.workplaceLatitude,
                workplaceLongitude = uiModel.workplaceLongitude,
                currentLatitude = uiModel.currentLatitude,
                currentLongitude = uiModel.currentLongitude,
                workplaceRadiusMeters = uiModel.workplaceRadiusMeters,
                currentLocationStatus = uiModel.currentLocationStatus,
                onRefreshCurrentLocation = onRefreshCurrentLocation
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WorkproofMapLegendItem(
                color = WorkproofMapCurrentPin,
                label = stringResource(R.string.workproof_location_legend_current)
            )
            WorkproofMapLegendItem(
                color = WorkproofMapWorkplacePin,
                label = stringResource(R.string.workproof_location_legend_workplace)
            )
        }
    }
}

@Composable
private fun WorkproofMapFallbackCard(
    hasApiKey: Boolean,
    isRuntimeSupported: Boolean
) {
    val fallbackBackground = colorResource(R.color.workproof_map_fallback_background)
    val message = when {
        !hasApiKey -> stringResource(R.string.workproof_map_fallback_missing_key)
        !isRuntimeSupported -> stringResource(R.string.workproof_map_fallback_runtime_unsupported)
        else -> stringResource(R.string.workproof_map_fallback_default)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(fallbackBackground)
            .border(1.dp, DawnBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun KakaoWorkplaceMapView(
    workplaceLatitude: Double,
    workplaceLongitude: Double,
    currentLatitude: Double,
    currentLongitude: Double,
    workplaceRadiusMeters: Int,
    currentLocationStatus: WorkproofCurrentLocationStatus?,
    onRefreshCurrentLocation: () -> Unit
) {
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshCurrentLocation()
    }
    var retryToken by rememberSaveable { mutableIntStateOf(0) }
    val mapIdentity = remember(retryToken) { "workproof-map-$retryToken" }
    val mapView = rememberKakaoMapViewWithLifecycle(mapIdentity)
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapErrorMessage by rememberSaveable(mapIdentity) { mutableStateOf<String?>(null) }
    var shouldFocusCurrentLocationAfterRefresh by rememberSaveable { mutableStateOf(false) }
    var hasStartedRefreshForFocus by rememberSaveable { mutableStateOf(false) }
    val workplacePosition = remember(workplaceLatitude, workplaceLongitude) {
        LatLng.from(workplaceLatitude, workplaceLongitude)
    }
    val currentPosition = remember(currentLatitude, currentLongitude) {
        LatLng.from(currentLatitude, currentLongitude)
    }
    val isRefreshingCurrentLocation = currentLocationStatus == WorkproofCurrentLocationStatus.LOADING

    LaunchedEffect(Unit) {
        onRefreshCurrentLocation()
    }

    LaunchedEffect(
        kakaoMap,
        workplaceLatitude,
        workplaceLongitude,
        currentLatitude,
        currentLongitude,
        workplaceRadiusMeters
    ) {
        val map = kakaoMap ?: return@LaunchedEffect
        addWorkproofMapMarkers(
            map = map,
            context = context,
            workplacePosition = workplacePosition,
            currentPosition = currentPosition,
            workplaceRadiusMeters = workplaceRadiusMeters
        )
    }

    LaunchedEffect(
        kakaoMap,
        workplaceLatitude,
        workplaceLongitude
    ) {
        val map = kakaoMap ?: return@LaunchedEffect
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(workplacePosition)
        )
    }

    LaunchedEffect(
        kakaoMap,
        shouldFocusCurrentLocationAfterRefresh,
        hasStartedRefreshForFocus,
        currentLocationStatus,
        currentLatitude,
        currentLongitude
    ) {
        if (!shouldFocusCurrentLocationAfterRefresh) {
            return@LaunchedEffect
        }
        when (currentLocationStatus) {
            WorkproofCurrentLocationStatus.LOADING -> {
                hasStartedRefreshForFocus = true
            }

            WorkproofCurrentLocationStatus.IDLE -> Unit
            WorkproofCurrentLocationStatus.READY,
            null -> {
                if (!hasStartedRefreshForFocus) {
                    return@LaunchedEffect
                }
                kakaoMap?.moveCamera(
                    CameraUpdateFactory.newCenterPosition(currentPosition)
                )
                shouldFocusCurrentLocationAfterRefresh = false
                hasStartedRefreshForFocus = false
            }

            else -> {
                shouldFocusCurrentLocationAfterRefresh = false
                hasStartedRefreshForFocus = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, DawnBorder, RoundedCornerShape(28.dp))
        ) {
            if (mapErrorMessage != null) {
                DonDoneErrorPanel(
                    title = stringResource(R.string.workproof_map_error_title),
                    message = mapErrorMessage ?: stringResource(R.string.workproof_map_error_message_default),
                    actionLabel = stringResource(R.string.workproof_map_retry),
                    onAction = {
                        mapErrorMessage = null
                        kakaoMap = null
                        retryToken += 1
                    },
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                AndroidView(
                    factory = {
                        mapView.apply {
                            start(
                                object : MapLifeCycleCallback() {
                                    override fun onMapDestroy() = Unit

                                    override fun onMapError(error: Exception) {
                                        mapErrorMessage = error.message
                                            ?: context.getString(R.string.workproof_map_error_initialize)
                                    }
                                },
                                object : KakaoMapReadyCallback() {
                                    override fun onMapReady(map: KakaoMap) {
                                        mapErrorMessage = null
                                        kakaoMap = map
                                        map.moveCamera(
                                            CameraUpdateFactory.newCenterPosition(workplacePosition)
                                        )
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, DawnBorder, RoundedCornerShape(16.dp))
                .clickable(enabled = !isRefreshingCurrentLocation) {
                    // 최신 위치 조회가 끝난 뒤 카메라를 한 번만 이동시킨다.
                    shouldFocusCurrentLocationAfterRefresh = true
                    hasStartedRefreshForFocus = false
                    if (hasWorkproofFineLocationPermission(context)) {
                        onRefreshCurrentLocation()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRefreshingCurrentLocation) {
                    stringResource(R.string.workproof_location_status_loading)
                } else {
                    stringResource(R.string.workproof_location_move_pin)
                },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (isRefreshingCurrentLocation) DawnTextSubtle.copy(alpha = 0.7f) else DawnTextSubtle
            )
        }
    }
}

@Composable
internal fun workproofCurrentLocationStatusMessage(
    status: WorkproofCurrentLocationStatus
): String {
    val stringRes = when (status) {
        WorkproofCurrentLocationStatus.LOADING -> R.string.workproof_location_status_loading
        WorkproofCurrentLocationStatus.PERMISSION_REQUIRED -> R.string.workproof_location_status_permission_required
        WorkproofCurrentLocationStatus.SERVICE_UNAVAILABLE -> R.string.workproof_location_status_service_unavailable
        WorkproofCurrentLocationStatus.LOCATION_DISABLED -> R.string.workproof_location_status_disabled
        WorkproofCurrentLocationStatus.ERROR -> R.string.workproof_location_status_error
        WorkproofCurrentLocationStatus.IDLE,
        WorkproofCurrentLocationStatus.READY -> R.string.workproof_location_status_try_again
    }
    return stringResource(stringRes)
}

private fun addWorkproofMapMarkers(
    map: KakaoMap,
    context: Context,
    workplacePosition: LatLng,
    currentPosition: LatLng,
    workplaceRadiusMeters: Int
) {
    val workplacePinBitmap = createMapPinBitmap(context, R.drawable.ic_workplace_pin)
    val currentPinBitmap = createMapPinBitmap(context, R.drawable.ic_current_location_pin)
    val radiusStyles = PolygonStylesSet.from(
        PolygonStyles.from(
            ContextCompat.getColor(context, R.color.workproof_map_radius_fill),
            1.0f,
            ContextCompat.getColor(context, R.color.workproof_map_radius_stroke)
        )
    )
    map.shapeManager?.layer?.let { shapeLayer ->
        shapeLayer.removeAll()
        shapeLayer.addPolygon(
            PolygonOptions.from(
                DotPoints.fromCircle(
                    workplacePosition,
                    workplaceRadiusMeters.toFloat()
                )
            ).setStylesSet(radiusStyles)
        )
    }

    map.labelManager?.let { labelManager ->
        labelManager.layer?.let { labelLayer ->
            labelLayer.removeAll()
            labelLayer.addLabel(
                LabelOptions.from(workplacePosition)
                    .apply {
                        if (workplacePinBitmap != null) setStyles(workplacePinBitmap) else setStyles(R.drawable.ic_workplace_pin)
                    }
                    .setRank(2000L)
                    .setVisible(true)
            )
            labelLayer.addLabel(
                LabelOptions.from(currentPosition)
                    .apply {
                        if (currentPinBitmap != null) setStyles(currentPinBitmap) else setStyles(R.drawable.ic_current_location_pin)
                    }
                    .setRank(1000L)
                    .setVisible(true)
            )
        }
    }
}

private fun createMapPinBitmap(
    context: Context,
    drawableResId: Int
): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, drawableResId) ?: return null
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: return null
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: return null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

@Composable
private fun WorkproofMapLegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = DawnText
        )
    }
}

@Composable
private fun rememberKakaoMapViewWithLifecycle(
    mapIdentity: String
): MapView {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember(mapIdentity) { MapView(context) }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.pause()
            mapView.finish()
        }
    }

    return mapView
}

private fun hasWorkproofFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
