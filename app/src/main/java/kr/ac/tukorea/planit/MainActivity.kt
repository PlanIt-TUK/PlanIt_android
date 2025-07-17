package kr.ac.tukorea.planit

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.tukorea.planit.databinding.CalendarMainViewBinding
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Callback
import okhttp3.Call
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray


// 1. 태스크 데이터 클래스
data class Task(
    val id: Int,
    val teamName: String,
    val taskName: String,
    val taskStart: String,
    val taskEnd: String,
    val taskState: Boolean,
    val taskTarget: String,
    val userEmail: String
)

class MainActivity : AppCompatActivity() {
    private val TAG = this::class.simpleName
    private lateinit var binding: CalendarMainViewBinding
    // 사용자 이메일 받아오기
    private lateinit var currentUserEmail: String
    // RecyclerView와 Adapter 변수 선언
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var sampleTasks: List<Task>
    private val dtFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentUserEmail = intent.getStringExtra("user_email") ?: ""
        Log.d("TeamMainActivity", "User Email: $currentUserEmail")

        enableEdgeToEdge()


        binding = CalendarMainViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 샘플 데이터 로드, 실제 백엔드와 연동시 주석처리해주시면 됩니다.
        loadSampleTasks()

        setupCalendar()
        setupRecyclerView()
        binding.myCalendar.selectToday()

        val btnAddTask = findViewById<Button>(R.id.btnAddTask)
        btnAddTask.setOnClickListener {
            handleAddTaskClick()  // ← 이 함수 내부에 네 로직 넣으면 됨
        }
    }
    // 새 일정 추가 버튼 클릭 함수
    private fun handleAddTaskClick() {
        val newTask = Task(
            id = System.currentTimeMillis().toInt(),
            teamName = "project a",
            taskName = "하이요",
            taskStart = "2025-07-16 10:00:00",
            taskEnd = "2025-07-16 22:00:00",
            taskState = false,
            taskTarget = "프로젝트 X",
            userEmail = "who1061@naver.com"
        )
        val json = """
        {
            "team_name": "${newTask.teamName}",
            "task_name": "${newTask.taskName}",
            "task_start": "${newTask.taskStart}",
            "task_end": "${newTask.taskEnd}",
            "task_state": ${newTask.taskState},  
            "task_target": "${newTask.taskTarget}",
            "user_email": "${newTask.userEmail}"
        }
    """.trimIndent()

        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://56.155.134.194:8000/add_task") // 실제 서버 URL로 바꿔주세요
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("handleAddTaskClick", "서버 전송 실패", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("handleAddTaskClick", "서버 전송 성공: ${response.code}")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "일정 추가 성공", Toast.LENGTH_SHORT).show()
                        // 필요 시 UI 업데이트 로직 추가 (예: 리사이클러뷰 갱신)
                    }
                } else {
                    Log.e("handleAddTaskClick", "서버 응답 오류: ${response.code}")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "일정 추가 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
        val intent = Intent(this, TeamMainActivity::class.java)
        intent.putExtra("user_email", currentUserEmail)
        intent.putExtra("team_name", "project a")
        startActivity(intent)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupCalendar() {
        with(binding) {
            myCalendar.setOnDateSelectedListener { selectedDateString ->
                // 선택 날짜 LocalDate & ISO-8601 문자열(요청용) 만들기
                val selectedLocalDate =
                    LocalDate.parse(selectedDateString, DateTimeFormatter.ofPattern("yyyy.M.d"))
                val isoString =
                    selectedLocalDate.atStartOfDay().toString()          // "2025-07-17T00:00:00"

                // ── ❶ 서버에 현재 유저의 ‘모든’ 일정 요청 (날짜 조건 X) ──
                val json = """
        {
          "user_email": "$currentUserEmail"
        }
    """.trimIndent()

                Thread {
                    // 1. HTTP 연결 ────────────────────────────────────────
                    val url = URL("http://56.155.134.194:8000/load_task")
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        doOutput = true
                    }

                    // 2. 요청 바디 작성
                    val jsonInput = JSONObject().apply {
                        put("user_email", currentUserEmail)          // ✱ 필수
                        // ↓ 필요하다면 추가 조건
                        put("team_name",  "project a")
                        put("task_target","프로젝트 X")
                        put("task_state", false)
                    }
                    connection.outputStream.use { it.write(jsonInput.toString().toByteArray()) }

                    // 3. 응답(문자열) & 원본 로그
                    val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("DBG", "RAW = $responseStr")

                    
                    // 4. JSON 파싱 → List<Task>
                    val rootObj   = JSONObject(responseStr)
                    val jsonArr   = rootObj.getJSONArray("task")        // 서버가 'task' 배열 반환
                    val allTasks: List<Task> = List(jsonArr.length()) { i ->
                        jsonArr.getJSONObject(i).run {
                            Task(
                                id         = optInt("id"),
                                teamName   = optString("team_name"),
                                taskName   = optString("task_name"),
                                taskStart  = optString("task_start"),
                                taskEnd    = optString("task_end"),
                                taskState  = optBoolean("task_state"),
                                taskTarget = optString("task_target"),
                                userEmail  = optString("user_email")
                            ).also { Log.d("Tasks", "▶ $it") }
                        }
                    }

                    // 5. 날짜·이메일 필터
                    val filtered = allTasks.filter { t ->
                        try {
                            val start = LocalDateTime.parse(t.taskStart, dtFormatter).toLocalDate()
                            val end   = LocalDateTime.parse(t.taskEnd,   dtFormatter).toLocalDate()
                            !selectedLocalDate.isBefore(start) &&        // start ≤ 선택일
                                    !selectedLocalDate.isAfter(end)   &&         // 선택일 ≤ end
                                    t.userEmail == currentUserEmail
                        } catch (e: Exception) { false }
                    }
                    Log.d("DBG", "filtered = ${filtered.size}")

                    // 6. 리사이클러뷰 갱신 (메인 스레드)
                    runOnUiThread { taskAdapter.updateData(filtered) }
                }.start()
            }
        }
    }
//    private fun parseTasks(json: String): List<Task> {
//        val rootObj   = JSONObject(json)
//
//        // ① output_ai가 JSONArray 직접 담는 방식
//        val jsonArray = rootObj.getJSONArray("output_ai")
//
//        /* ── 혹시 output_ai가 문자열로 들어온다면: ──
//           val outputStr = rootObj.getString("output_ai")
//           val jsonArray = JSONArray(outputStr)
//        */
//
//        val tasks = mutableListOf<Task>()
//        for (i in 0 until jsonArray.length()) {
//            val item = jsonArray.getJSONObject(i)
//            tasks.add(
//                Task(
//                    id         = item.optInt("id"),
//                    teamName   = item.optString("team_name"),
//                    taskName   = item.optString("task_name"),
//                    taskStart  = item.optString("task_start"),
//                    taskEnd    = item.optString("task_end"),
//                    taskState  = item.optBoolean("task_state"),
//                    taskTarget = item.optString("task_target"),
//                    userEmail  = item.optString("user_email")
//                )
//            )
//        }
//        return tasks
//    }

    private fun setupRecyclerView() {
        // RecyclerView 참조 획득
        recyclerView = findViewById(R.id.recyclerView)

        // Adapter 초기화
        taskAdapter = TaskAdapter(
            onItemClick = { task ->
                // 아이템 클릭 시 실행될 코드
                handleTaskClick(task)
            },
            onCheckboxClick = { task, isChecked ->
                // 1) RecyclerView UI 업데이트
                taskAdapter.updateTaskCompletion(task.id, isChecked)

                // 2) DB 업데이트 함수 호출 (별도로 구현)
                updateTaskCompletionStatus(task, isChecked)

                // 3) 토스트 등 UI 알림
                handleCheckboxClick(task, isChecked)
            }
        )

        // RecyclerView에 Adapter 설정
        recyclerView.adapter = taskAdapter

        // LayoutManager 설정 (수직 방향 리스트)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 선택사항: 아이템 간 간격 설정
        val itemDecoration = object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.bottom = 16 // 아이템 간 16dp 간격
            }
        }
        recyclerView.addItemDecoration(itemDecoration)

        // 성능 최적화
        recyclerView.setHasFixedSize(true)
    }

    /**
     * 샘플 태스크 데이터를 로드하는 메서드
     */
    private fun loadSampleTasks() {
        sampleTasks = listOf(
            Task(id = 1, teamName = "Team Alpha", taskName = "기획안 작성", taskStart = "2025-07-15 09:00:00", taskEnd = "2025-07-15 12:00:00", taskState = false, taskTarget = "프로젝트 A", userEmail = "who1061@naver.com"),
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
        // ▼ (3) 토글된 상태를 그대로 전달 (기존 코드와 동일, 위치만 확인)
        updateTaskCompletionStatus(task, isChecked)

        val message = if (isChecked) "태스크 완료: ${task.taskName}"
        else            "태스크 미완료: ${task.taskName}"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 태스크 완료 상태를 업데이트하는 메서드 (실제 데이터 저장)
     * @param taskId 업데이트할 태스크 ID
     * @param isCompleted 새로운 완료 상태
     */
    private fun updateTaskCompletionStatus(task: Task, newState: Boolean) {   // ⬅︎ 파라미터명만 읽기 편하게
        val client = OkHttpClient()

        // ---------- JSON Body ----------
        val jsonBody = """
        {
          "team_name": "${task.teamName}",
          "task_name": "${task.taskName}",
          "task_state": $newState            // ⬅︎ true / false 토글 값
        }
    """.trimIndent()

        val request = Request.Builder()
            .url("http://56.155.134.194:8000/update_task")   // 서버 스펙 동일
            .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "[update] 실패", e)     // ⬅︎ 실패 로그
            }
            override fun onResponse(call: Call, response: Response) {
                Log.d("MainActivity", "[update] 응답: ${'$'}{response.code}")  // ⬅︎ 응답 코드 확인
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

}

// 2. TaskViewHolder 클래스
class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    // 뷰 요소들 바인딩
    private val taskTime: TextView = itemView.findViewById(R.id.task_time)
    private val taskTitle: TextView = itemView.findViewById(R.id.task_title)
    private val taskProject: TextView = itemView.findViewById(R.id.task_project)
    private val taskCheckBox: CheckBox = itemView.findViewById(R.id.task_checkbox)
    private val taskLine: View = itemView.findViewById(R.id.task_line)

    /**
     * 태스크 데이터를 뷰에 바인딩하는 메서드
     * @param task 바인딩할 Task 객체
     * @param onItemClick 아이템 클릭 시 실행할 람다 함수
     * @param onCheckboxClick 체크박스 클릭 시 실행할 람다 함수
     */
    fun bind(
        task: Task,
        onItemClick: (Task) -> Unit,
        onCheckboxClick: (Task, Boolean) -> Unit
    ) {
        // 체크박스 리스너를 먼저 제거 (중복 호출 방지)
        taskCheckBox.setOnCheckedChangeListener(null)

        // 데이터를 각 뷰에 설정
        val inputFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val dateFormatter = DateTimeFormatter.ofPattern("MM.dd")

        val start = try {
            LocalDateTime.parse(task.taskStart, inputFormatter)
        } catch (e: Exception) {
            null
        }
        val end = try {
            LocalDateTime.parse(task.taskEnd, inputFormatter)
        } catch (e: Exception) {
            null
        }

        val timeText = when {
            start != null && end != null -> {
                if (start.toLocalDate() == end.toLocalDate()) {
                    // 같은 날짜: 시간 줄바꿈 후 ~ 종료시간
                    val startTime = start.format(timeFormatter)
                    val endTime = end.format(timeFormatter)
                    "   $startTime\n~ $endTime"
                } else {
                    // 날짜 다름: ~ MM.dd
                    "~ ${end.format(dateFormatter)}"
                }
            }
            else -> "         " // 파싱 실패 시 공백
        }
        taskTime.text = timeText
        taskTitle.text = task.taskName
        taskProject.text = task.teamName
        taskCheckBox.isChecked = task.taskState

        // 완료 상태에 따른 UI 변경
        updateUIForCompletionState(task.taskState)

        // 아이템 클릭 리스너 설정 (체크박스 제외)
        itemView.setOnClickListener {
            onItemClick(task)
        }

        // 체크박스 클릭 리스너 설정 (리스너 재설정)
        taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            // 현재 상태와 다를 때만 콜백 호출
            if (isChecked != task.taskState) {
                onCheckboxClick(task, isChecked)
            }



        }
    }

    /**
     * 완료 상태에 따른 UI 업데이트
     * @param isCompleted 완료 여부
     */
    private fun updateUIForCompletionState(isCompleted: Boolean) {
        if (isCompleted) {
            // 완료된 태스크: 텍스트 색상 변경 및 취소선 추가
            taskTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.todo_complete))
            taskTitle.paintFlags = taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            taskProject.setTextColor(ContextCompat.getColor(itemView.context, R.color.todo_complete))
            taskTime.setTextColor(ContextCompat.getColor(itemView.context, R.color.todo_complete))
        } else {
            // 미완료된 태스크: 원래 색상으로 복원
            taskTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
            taskTitle.paintFlags = taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            taskProject.setTextColor(Color.parseColor("#9A9A9A"))
            taskTime.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
        }
    }
}

// 3. TaskAdapter 클래스
class TaskAdapter(
    private var taskList: MutableList<Task> = mutableListOf(),
    private val onItemClick: (Task) -> Unit,
    private val onCheckboxClick: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskViewHolder>() {

    /**
     * ViewHolder를 생성하는 메서드
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    /**
     * ViewHolder에 데이터를 바인딩하는 메서드
     */
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        holder.bind(task, onItemClick) { clickedTask, isChecked ->
            // ▼ (1) 여기 유지 -------------------------------
            updateTaskCompletion(clickedTask.id, isChecked)

            // ▼ (2) 이미 토글된 값(isChecked)을 그대로 넘긴다
            onCheckboxClick(clickedTask, isChecked)   // ⬅︎ 수정 없음, 그냥 확인만
        }
    }

    /**
     * 전체 아이템 개수를 반환하는 메서드
     */
    override fun getItemCount(): Int = taskList.size

    /**
     * 새로운 데이터로 리스트를 업데이트하는 메서드
     * @param newList 새로운 Task 리스트
     */
    fun updateData(newList: List<Task>) {
        taskList.clear()
        Log.d("DBG", "updateData in, list = ${newList.size}")

        val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME   // "2025-07-10T10:00:00"

        val sorted = newList.sortedWith { a, b ->
            // 1) 완료 여부
            if (a.taskState != b.taskState) {
                return@sortedWith if (a.taskState) 1 else -1   // false 먼저
            }

            // 2) 같은 완료 그룹 안에서 날짜 타입 비교
            fun isSameDay(t: Task): Boolean {
                val s = LocalDateTime.parse(t.taskStart, fmt).toLocalDate()
                val e = LocalDateTime.parse(t.taskEnd,   fmt).toLocalDate()
                return s == e
            }
            val aSame = isSameDay(a)
            val bSame = isSameDay(b)
            if (aSame != bSame) {
                return@sortedWith if (aSame) -1 else 1          // 당일-일정 먼저
            }

            // 3) 같은 타입 → 세부 시간 비교
            val aStart = LocalDateTime.parse(a.taskStart, fmt)
            val bStart = LocalDateTime.parse(b.taskStart, fmt)
            val aEnd   = LocalDateTime.parse(a.taskEnd,   fmt)
            val bEnd   = LocalDateTime.parse(b.taskEnd,   fmt)

            return@sortedWith if (aSame) {
                aStart.compareTo(bStart)        // 당일-일정 : 시작시간
            } else {
                aEnd.compareTo(bEnd)            // 며칠-짜리 : 마감시간
            }
        }

        taskList.addAll(sorted)
        notifyDataSetChanged()
    }

    /**
     * 특정 태스크의 완료 상태를 업데이트하는 메서드
     * @param taskId 업데이트할 태스크의 ID
     * @param isCompleted 새로운 완료 상태
     */
    fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        val index = taskList.indexOfFirst { it.id == taskId }
        if (index != -1) {
            taskList[index] = taskList[index].copy(taskState = isCompleted)
            notifyItemChanged(index)
        }
    }

    /**
     * 새 태스크를 추가하는 메서드
     * @param task 추가할 Task 객체
     */
    fun addTask(task: Task) {
        taskList.add(task)
        notifyItemInserted(taskList.size - 1)
    }

    /**
     * 특정 위치의 태스크를 제거하는 메서드
     * @param position 제거할 위치
     */
    fun removeTask(position: Int) {
        if (position in 0 until taskList.size) {
            taskList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * 태스크 ID로 태스크를 제거하는 메서드
     * @param taskId 제거할 태스크의 ID
     */
    fun removeTaskById(taskId: Int) {
        val index = taskList.indexOfFirst { it.id == taskId }
        if (index != -1) {
            removeTask(index)
        }
    }

    /**
     * 완료된 태스크들을 모두 제거하는 메서드
     */
    fun removeCompletedTasks() {
        val completedIndices = taskList.mapIndexedNotNull { index, task ->
            if (task.taskState) index else null
        }.reversed() // 뒤에서부터 삭제

        completedIndices.forEach { index ->
            taskList.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}