package kr.ac.tukorea.planit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kr.ac.tukorea.planit.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.kakaoLoginButton.setOnClickListener{
            // TODO: 로그인 동작 수행
            //이후 잘 되면 메인 액티비티로 이동
            Toast.makeText(this,"로그인에 성공하였습니다", Toast.LENGTH_SHORT).show()
            var intent=Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}