package kr.ac.tukorea.planit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kr.ac.tukorea.planit.CalenderBoardFragment
import kr.ac.tukorea.planit.databinding.ActivityTeamMainBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TeamMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityTeamMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // 0. 화면 배율 채우기
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. 권한 불러오기
        binding.btnSetting.setOnClickListener {
            Thread {
                val url = URL("http://56.155.134.194:8000/load_member")

                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                }


                val jsonInput = JSONObject().apply {
                    put("team_name", "Team123")
                    put("user_email", "123")
                    // put("user_onwer", None)
                }

                connection.outputStream.use { it.write(jsonInput.toString().toByteArray(Charsets.UTF_8)) }


                val responseJson = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })

                val owner = responseJson.getJSONArray("member").getJSONObject(0).getString("user_owner")

                if (owner == "true") {
                    runOnUiThread {
                        val intent = Intent(this@TeamMainActivity, OwnerProjectManagerActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                else {
                    runOnUiThread {
                        val intent = Intent(this@TeamMainActivity, MemberProjectManagerActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }.start()
        }
    }
}
