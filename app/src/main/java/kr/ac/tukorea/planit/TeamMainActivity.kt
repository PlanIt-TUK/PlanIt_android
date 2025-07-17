package kr.ac.tukorea.planit

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kr.ac.tukorea.planit.MyCalendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import kr.ac.tukorea.planit.CalenderBoardFragment
import kr.ac.tukorea.planit.databinding.ActivityTeamMainBinding
import kr.ac.tukorea.planit.databinding.CalendarMainViewBinding
import kr.ac.tukorea.planit.databinding.MyCalendarBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class TeamMainActivity : AppCompatActivity() {
    private lateinit var binding2: ActivityTeamMainBinding
    private lateinit var binding: CalendarMainViewBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var sampleTasks: List<Task>

    private var currentUserEmail: String = ""
    private var currentTeamName: String = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = CalendarMainViewBinding.inflate(layoutInflater)
        binding2 = ActivityTeamMainBinding.inflate(layoutInflater)
        setContentView(binding2.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)

        currentUserEmail = intent.getStringExtra("user_email") ?: ""
        currentTeamName = intent.getStringExtra("team_name") ?: ""
        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("user_email", currentUserEmail)
            startActivity(intent)
            finish()
        }

        tvTitle.text = currentTeamName

        // 이 아래는 기존 코드 계속
        loadSampleTasks()
        setupCalendar()
        //setupRecyclerView()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupCalendar() {
        // 캘린더에서 날짜가 선택될 때 호출되는 리스너 설정
        with(binding) {
            myCalendar.setOnDateSelectedListener { selectedDate ->
                // 선택된 날짜 표시
                tvSelectedDate.text = "선택된 날짜: $selectedDate"
                // 선택된 날짜에 따른 추가 작업
                // TODO: 선택된 날짜별 할 일 불러오기
                // 날짜에 해당하는 태스크만 필터링

                try {
                    val inputFormatter = DateTimeFormatter.ofPattern("yyyy.M.d") // ex: "2025.7.18"
                    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val selectedLocalDate = LocalDate.parse(selectedDate, inputFormatter)

                    Log.d("Debug", "selectedDate: $selectedLocalDate")

                    val filteredTasks = sampleTasks.filter { task ->
                        // 날짜 필터는 기존대로 유지
                        val dateMatches = try {
                            val hasStart = task.taskStart.isNotBlank()
                            val hasEnd = task.taskEnd.isNotBlank()

                            val startDate = if (hasStart)
                                LocalDateTime.parse(task.taskStart, dateFormatter).toLocalDate()
                            else null

                            val endDate = if (hasEnd)
                                LocalDateTime.parse(task.taskEnd, dateFormatter).toLocalDate()
                            else null

                            when {
                                hasStart && hasEnd -> !selectedLocalDate.isBefore(startDate) && !selectedLocalDate.isAfter(endDate)
                                hasStart -> selectedLocalDate == startDate
                                hasEnd -> selectedLocalDate == endDate
                                else -> false
                            }
                        } catch (e: Exception) {
                            false
                        }
                        // 여기에 이메일 필터 추가!
                        dateMatches && (task.userEmail == currentUserEmail)
                    }

                    Log.d("MainActivity", "Filtered tasks count: ${filteredTasks.size}")
                    filteredTasks.forEach { task ->
                        Log.d("MainActivity", "Task: id=${task.id}, taskStart=${task.taskStart}, taskEnd=${task.taskEnd}, title=${task.taskName}")
                    }

                    taskAdapter.updateData(filteredTasks)

                } catch (e: Exception) {
                    Log.e("MainActivity", "날짜 파싱 오류", e)
                    taskAdapter.updateData(emptyList())
                }
            }
        }
    }

    private fun loadSampleTasks() {
        sampleTasks = listOf(
            Task(id = 1, teamName = "project a", taskName = "기획안 작성", taskStart = "2025-07-15 09:00:00", taskEnd = "2025-07-15 12:00:00", taskState = false, taskTarget = "프로젝트 A", userEmail = "who1061@naver.com"),
            Task(id = 2, teamName = "Team Alpha", taskName = "디자인 회의", taskStart = "2025-07-16 14:00:00", taskEnd = "2025-07-16 15:30:00", taskState = false, taskTarget = "프로젝트 A", userEmail = "who1061@naver.com"),
            Task(id = 3, teamName = "Team Beta", taskName = "개발 작업", taskStart = "2025-07-16 00:00:00", taskEnd = "2025-07-18 18:00:00", taskState = false, taskTarget = "프로젝트 B", userEmail = "who1061@naver.com"),
            Task(id = 4, teamName = "Team Beta", taskName = "기능 테스트", taskStart = "2025-07-16 00:00:00", taskEnd = "2025-07-17 23:59:59", taskState = false, taskTarget = "프로젝트 B", userEmail = "who1061@naver.com"),
            Task(id = 5, teamName = "Team Gamma", taskName = "마무리 정리", taskStart = "2025-07-17 10:00:00", taskEnd = "2025-07-18 18:00:00", taskState = true, taskTarget = "프로젝트 C", userEmail = "user5@example.com")
        )
        //taskAdapter.updateData(sampleTasks)
    }


    /**
     * 태스크 아이템 클릭 시 실행되는 메서드
     * @param task 클릭된 Task 객체
     */
    private fun handleTaskClick(task: Task) {
        // 태스크 상세 정보 표시 또는 편집 화면으로 이동
        Toast.makeText(
            this,
            "태스크 선택: ${task.taskName}",
            Toast.LENGTH_SHORT
        ).show()

        // 상세 Activity로 이동하는 경우
        // val intent = Intent(this, TaskDetailActivity::class.java)
        // intent.putExtra("task_id", task.id)
        // startActivity(intent)
    }

    /**
     * 체크박스 클릭 시 실행되는 메서드
     * @param task 클릭된 Task 객체
     * @param isChecked 새로운 체크 상태
     */
    private fun handleCheckboxClick(task: Task, isChecked: Boolean) {
        // 데이터베이스나 서버에 완료 상태 업데이트
        updateTaskCompletionStatus(task, isChecked)

        // 완료/미완료 상태에 따른 메시지 표시
        val message = if (isChecked) "태스크 완료: ${task.taskName}" else "태스크 미완료: ${task.taskName}"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 태스크 완료 상태를 업데이트하는 메서드 (실제 데이터 저장)
     * @param taskId 업데이트할 태스크 ID
     * @param isCompleted 새로운 완료 상태
     */
    private fun updateTaskCompletionStatus(task: Task, isCompleted: Boolean) {
        val client = OkHttpClient()

        val json = """
        {
          "team_name": "${task.teamName}",
          "task_name": "${task.taskName}",
          "task_state": ${isCompleted}
        }
    """.trimIndent()

        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://56.155.134.194:8000/update_task") // 실제 서버 주소
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "업데이트 실패", e)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("MainActivity", "업데이트 응답: ${response.code}")
            }
        })
    }

    /**
     * 새 태스크를 추가하는 메서드
     */
    fun addNewTask(taskName: String, taskStart: String, taskEnd: String, taskTarget: String,
                   teamName: String, taskState: Boolean = false, userEmail: String
    ) {
        val newTask = Task(
            id = System.currentTimeMillis().toInt(),
            teamName = teamName,
            taskName = taskName,
            taskStart = taskStart,
            taskEnd = taskEnd,
            taskState = taskState,
            taskTarget = taskTarget,
            userEmail = userEmail
        )
        taskAdapter.addTask(newTask)

        // 새 태스크로 스크롤
        recyclerView.smoothScrollToPosition(taskAdapter.itemCount - 1)
    }

    /**
     * 완료된 태스크들을 모두 제거하는 메서드
     */
    fun clearCompletedTasks() {
        taskAdapter.removeCompletedTasks()
    }
    class TeamMainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            val layoutId = when (position) {
                0 -> R.layout.fragment_calender_board
                1 -> R.layout.fragment_board
                2 -> R.layout.fragment_add_board
                else -> throw IllegalArgumentException("Invalid tab index")
            }

            return object : Fragment() {
                override fun onCreateView(
                    inflater: LayoutInflater,
                    container: ViewGroup?,
                    savedInstanceState: Bundle?
                ): View? {
                    return inflater.inflate(layoutId, container, false)
                }
            }
        }
    }

}