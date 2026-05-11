package com.example.next_contest

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

class NextContestApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.KAKAO_MAP_NATIVE_KEY.isNotBlank()) {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_MAP_NATIVE_KEY)
        }
    }
}
