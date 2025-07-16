package kr.ac.tukorea.planit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import kr.ac.tukorea.planit.databinding.ActivityLoginBinding
import com.kakao.sdk.common.util.Utility

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.i("KakaoKeyHash", Utility.getKeyHash(this))

        // ✅ ViewBinding 적용
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ 시스템 바 영역 패딩
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ✅ 카카오 로그인 버튼 클릭 리스너 설정
        binding.kakaoLoginButton.setOnClickListener {
            Log.d("LoginActivity", "카카오 로그인 버튼 클릭됨")
            Toast.makeText(this, "로그인 시도", Toast.LENGTH_SHORT).show()

            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                    handleLoginResult(token, error)
                }
            } else {
                UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                    handleLoginResult(token, error)
                }
            }
        }
    }

    private fun handleLoginResult(token: OAuthToken?, error: Throwable?) {
        Log.d("LoginActivity", "handleLoginResult() 실행됨")

        if (error != null) {
            Log.e("LoginActivity", "카카오 로그인 실패", error)
        } else if (token != null) {
            Log.i("LoginActivity", "카카오 로그인 성공: ${token.accessToken}")

            UserApiClient.instance.me { user, userError ->
                if (userError != null) {
                    Log.e("LoginActivity", "사용자 정보 요청 실패", userError)
                } else if (user != null) {
                    Log.i("LoginActivity", "사용자 정보 성공: 이메일=${user.kakaoAccount?.email}")
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
