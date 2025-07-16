package kr.ac.tukorea.planit

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, "0b4637b92ea391f3549344285a5dacdf")
    }
}
