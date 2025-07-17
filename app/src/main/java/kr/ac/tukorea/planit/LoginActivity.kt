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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

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
                    return@me
                }
                if (user == null) {
                    Log.e("LoginActivity", "사용자 정보 없음")
                    return@me
                }

                val userEmail = user.kakaoAccount?.email ?: ""
                val userNickname = user.kakaoAccount?.profile?.nickname ?: ""
                val userImage = user.kakaoAccount?.profile?.profileImageUrl ?: ""

                val client = OkHttpClient()
                val json = """
                    {
                        "user_email": "$userEmail",
                        "user_nickname": "$userNickname",
                        "user_image": "$userImage"
                    }
                """.trimIndent()

                val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("http://56.155.134.194:8000/add_user") // ← 여기에 실제 EC2 주소 입력
                    .post(requestBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("LoginActivity", "백엔드 전송 실패", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.d("LoginActivity", "백엔드 전송 성공: ${response.code}")
                        runOnUiThread {
                            // ✅ MainActivity → TeamMainActivity 로 전환
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                })
            }

        }
    }
}
