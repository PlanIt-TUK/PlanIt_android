package kr.ac.tukorea.planit

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kr.ac.tukorea.planit.databinding.ActivityAddTaskBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AddTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTaskBinding
    private val client = OkHttpClient()
    private val isoFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAddTask.setOnClickListener { addTask() }
    }

    private fun addTask() {
        val title = binding.editTitle.text.toString().trim()
        val startRaw = binding.editStartDate.text.toString().trim()
        val endRaw   = binding.editEndDate.text.toString().trim()
        val userEmail = intent.getStringExtra("user_email") ?: ""

        if (title.isEmpty() || startRaw.isEmpty() || endRaw.isEmpty()) {
            Toast.makeText(this, "제목·날짜를 모두 입력하세요.", Toast.LENGTH_SHORT).show(); return
        }

        // ① 문자열 → LocalDateTime 파싱 (사용자가 ‘2025‑07‑18 09:00’ 입력했다고 가정)
        val startDt = LocalDateTime.parse(startRaw.replace(' ', 'T'))
        val endDt   = LocalDateTime.parse(endRaw.replace(' ', 'T'))

        // ② JSON
        val json = JSONObject().apply {
            put("team_name", "")                // 개인 할 일
            put("task_target", "")
            put("task_name", title)
            put("task_start", isoFmt.format(startDt))
            put("task_end",   isoFmt.format(endDt))
            put("task_state", false)           // ENUM
            put("task_color", 0)                // 기본 색상
            put("user_email", userEmail)
        }.toString()

        val reqBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("http://56.155.134.194:8000/add_task")
            .post(reqBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@AddTaskActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show() }
                Log.e("AddTaskActivity", "add_task 실패", e)
            }
            override fun onResponse(call: Call, resp: Response) {
                runOnUiThread {
                    if (resp.isSuccessful) {
                        Toast.makeText(this@AddTaskActivity, "할 일 등록 완료!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK); finish()
                    } else {
                        Toast.makeText(this@AddTaskActivity, "등록 실패: ${resp.code}", Toast.LENGTH_SHORT).show()
                        Log.e("AddTaskActivity", "body=${resp.body?.string()}")
                    }
                }
            }
        })
    }
}
