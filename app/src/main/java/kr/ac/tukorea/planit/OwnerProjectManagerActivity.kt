package kr.ac.tukorea.planit

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kr.ac.tukorea.planit.databinding.ActivityOwnerProjectManagerBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OwnerProjectManagerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityOwnerProjectManagerBinding.inflate(layoutInflater)

        setContentView(binding.root)


        // 0. 화면 비율 채우기
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. 팀원 불러오기
        Thread {
            val url = URL("http://56.155.134.194:8000/load_member")

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
            }


            val jsonInput = JSONObject().apply {
                put("team_name", "Team123")
                // put("user_email", None)
                // put("user_onwer", None)
            }

            connection.outputStream.use { it.write(jsonInput.toString().toByteArray(Charsets.UTF_8)) }


            val responseJson = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })

            val Members = responseJson.getJSONArray("member").let { array ->
                List(array.length()) { i ->
                    val obj = array.getJSONObject(i)
                    val team_name = obj.getString("team_name")
                    val user_email = obj.getString("user_email")
                    val user_owner = obj.getString("user_owner")

                    Log.d("Members", team_name)
                    Log.d("Members", user_email)
                    Log.d("Members", user_owner)
                }
            }
        }.start()

        // 2. 초대 이메일 보내기
        binding.editInviteEmail.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                Thread {
                    val url = URL("http://56.155.134.194:8000/add_member")

                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        doOutput = true
                    }


                    val jsonInput = JSONObject().apply {
                        put("team_name", "Team123") // put("team_name", sharedPreferences.getString("team_name", ""))
                        put("user_email", binding.editInviteEmail.text.toString().trim())
                        put("user_owner", false)
                    }

                    connection.outputStream.use { it.write(jsonInput.toString().toByteArray(Charsets.UTF_8)) }


                    connection.inputStream.bufferedReader().use { it.readText() }

                    binding.editInviteEmail.setText("")
                }.start()

                true
            }
            else false
        }

        // 3. 멤버 삭제하기
        binding.btnDeleteMember.setOnClickListener {
            Thread {
                val url = URL("http://56.155.134.194:8000/delete_member")

                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                }


                val jsonInput = JSONObject().apply {
                    put("team_name", "Team123") // put("team_name", sharedPreferences.getString("team_name", ""))
                    put("user_email", "123") // put("user_email", member.email)
                    // put("user_owner", None)
                }

                connection.outputStream.use { it.write(jsonInput.toString().toByteArray(Charsets.UTF_8)) }


                connection.inputStream.bufferedReader().use { it.readText() }
            }.start()
        }

        // 4. 팀 삭제하기 (팀장 페이지)
        binding.btnDeleteProject.setOnClickListener {
            Thread {
                val url = URL("http://56.155.134.194:8000/delete_team")

                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                }


                val jsonInput = JSONObject().apply {
                    put("team_name", "Team123") // put("team_name", sharedPreferences.getString("team_name", ""))
                    // put("user_email", None)
                    // put("user_owner", None)
                }

                connection.outputStream.use { it.write(jsonInput.toString().toByteArray(Charsets.UTF_8)) }


                connection.inputStream.bufferedReader().use { it.readText() }
            }.start()
        }

//        // 5. 팀 나가기 (개인 페이지)
//        binding.btnDeleteMember.setOnClickListener {
//            Thread {
//                val url = URL("http://56.155.134.194:8000/delete_member")
//
//                val connection = (url.openConnection() as HttpURLConnection).apply {
//                    requestMethod = "POST"
//                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
//                    doOutput = true
//                }
//
//
//                val jsonInput = JSONObject().apply {
//                    put("team_name", "Test123") // put("team_name", sharedPreferences.getString("team_name", ""))
//                    put("user_email", "123") // put("user_email", sharedPreferences.getString("user_email", ""))
//                    // put("user_owner", None)
//                }
//
//                connection.outputStream.use { it.write(jsonInput.toString().toByteArray(Charsets.UTF_8)) }
//
//
//                connection.inputStream.bufferedReader().use { it.readText() }
//            }.start()
//        }
    }
}