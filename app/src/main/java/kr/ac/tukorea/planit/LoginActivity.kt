package kr.ac.tukorea.planit

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // system bar 영역 패딩 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 로그인 버튼 찾기
        val kakaoLoginButton = findViewById<ImageButton>(R.id.kakaoLoginButton)

        kakaoLoginButton.setOnClickListener {
            Log.d("LoginActivity", "카카오 로그인 버튼 클릭됨")

            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                // 카카오톡으로 로그인
                UserApiClient.instance.loginWithKakaoTalk(this) { token: OAuthToken?, error: Throwable? ->
                    handleLoginResult(token, error)
                }
            } else {
                // 카카오 계정(웹뷰) 로그인
                UserApiClient.instance.loginWithKakaoAccount(this) { token: OAuthToken?, error: Throwable? ->
                    handleLoginResult(token, error)
                }
            }
        }
    }

    private fun handleLoginResult(token: OAuthToken?, error: Throwable?) {
        if (error != null) {
            Log.e("LoginActivity", "카카오 로그인 실패", error)
        } else if (token != null) {
            Log.i("LoginActivity", "카카오 로그인 성공: ${token.accessToken}")

            // 사용자 정보 요청
            UserApiClient.instance.me { user, userError ->
                if (userError != null) {
                    Log.e("LoginActivity", "사용자 정보 요청 실패", userError)
                } else if (user != null) {
                    Log.i("LoginActivity", "사용자 정보:")
                    Log.i("LoginActivity", "닉네임: ${user.kakaoAccount?.profile?.nickname}")
                    Log.i("LoginActivity", "이메일: ${user.kakaoAccount?.email}")

                    // TODO: 로그인 성공 처리 → 메인화면 이동 or 서버 전송 등
                }
            }
        }
    }
}
