package com.dondone.mobile

import android.app.Application
import com.dondone.mobile.core.map.KakaoMapSupport
import com.kakao.vectormap.KakaoMapSdk

class DonDoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (KakaoMapSupport.isMapAvailable(BuildConfig.KAKAO_NATIVE_APP_KEY)) {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
    }
}
