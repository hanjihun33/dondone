package com.dondone.mobile.core.map

import android.os.Build

object KakaoMapSupport {
    private val supportedAbiPrefixes = listOf("arm64-v8a", "armeabi-v7a")

    fun isRuntimeSupported(): Boolean {
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        return supportedAbiPrefixes.any { prefix -> primaryAbi.startsWith(prefix) }
    }
}
