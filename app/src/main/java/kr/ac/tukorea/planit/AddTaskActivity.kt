package kr.ac.tukorea.planit.ui.add   // 파일이 위치한 실제 패키지 경로에 맞추어 수정하세요

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.children
import kr.ac.tukorea.planit.databinding.ActivityAddTaskBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlin.concurrent.thread

class AddTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTaskBinding

    private var selectedColor: Int       = 0         // 0‑11
    private var startDate:    LocalDate? = null
    private var endDate:      LocalDate? = null
    private lateinit var userEmail: String

    /* ────────────────────────── onCreate ────────────────────────── */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars = true

        userEmail = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("USER_EMAIL", null) ?: run {
            toast("로그인 정보가 없습니다")
            finish(); return
        }

        initDatePickers()
        initColorPicker()
        initAddButton()

        binding.btnBack.setOnClickListener { finish() }
    }

    /* ────────────────────────── 날짜 선택기 ────────────────────────── */
    private fun initDatePickers() = with(binding) {
        val today = LocalDate.now()

        fun showPicker(cb: (LocalDate) -> Unit) {
            DatePickerDialog(
                this@AddTaskActivity,
                { _, y, m, d -> cb(LocalDate.of(y, m + 1, d)) },
                today.year, today.monthValue - 1, today.dayOfMonth
            ).show()
        }

        editStartDate.setOnClickListener {
            showPicker { d ->
                startDate = d
                editStartDate.setText(d.toString())
            }
        }
        editEndDate.setOnClickListener {
            showPicker { d ->
                endDate = d
                editEndDate.setText(d.toString())
            }
        }
    }

    /* ────────────────────────── 색상 선택기 ────────────────────────── */
    private fun initColorPicker() = with(binding) {
        colorGrid.children.forEachIndexed { idx, view ->
            view.setOnClickListener {
                selectedColor = idx
                // 선택 효과를 따로 주지 않음 (bg_color_selected_border 제거)
            }
        }
    }

    /* ────────────────────────── “+ 할 일 추가” ────────────────────────── */
    private fun initAddButton() = with(binding) {
        btnAddTask.setOnClickListener {

            val title = editTitle.text.toString().trim()
            when {
                title.isBlank()                       -> { toast("제목을 입력하세요"); return@setOnClickListener }
                startDate == null || endDate == null  -> { toast("날짜를 모두 선택하세요"); return@setOnClickListener }
                endDate!!.isBefore(startDate)         -> { toast("마감일이 시작일보다 빠릅니다"); return@setOnClickListener }
            }

            val body = JSONObject().apply {
                put("team_name",   "")
                put("task_name",   title)
                put("task_start",  startDate.toString())
                put("task_end",    endDate.toString())
                put("task_state",  "TODO")
                put("task_color",  selectedColor)
                put("task_target", "")
                put("user_email",  userEmail)
            }

            thread {
                try {
                    val url = URL("http://56.155.134.194:8000/tasks")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        doOutput = true
                        connectTimeout = 8_000
                        readTimeout = 8_000
                    }
                    conn.outputStream.use { it.write(body.toString().toByteArray()) }

                    val code = conn.responseCode
                    runOnUiThread {
                        if (code in 200..299) {
                            toast("할 일이 추가되었습니다!")
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            toast("추가 실패: $code")
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { toast("네트워크 오류: ${e.localizedMessage}") }
                }
            }
        }
    }

    /* ────────────────────────── Toast helper ────────────────────────── */
    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
