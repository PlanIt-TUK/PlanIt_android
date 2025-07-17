package kr.ac.tukorea.planit

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.tukorea.planit.databinding.ActivityMainBinding
import kr.ac.tukorea.planit.databinding.CalendarMainViewBinding
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.net.HttpURLConnection

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

// 1. 프로젝트 데이터 클래스 추가
data class Project(
    val id: Int,
    val name: String,
    val isSelected: Boolean = false
)

class MainActivity : AppCompatActivity() {

    private val TAG = this::class.simpleName
    private lateinit var binding: ActivityMainBinding
    private lateinit var currentUserEmail: String

    // RecyclerView와 Adapter 변수 선언
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var sampleTasks: List<Task>

    // 드로어 내 뷰들
    private lateinit var currentProjectName: TextView
    private lateinit var projectRecyclerView: RecyclerView
    private lateinit var projectAdapter: ProjectAdapter
    private lateinit var btnAddProject: Button
    private var selectedProjectId: Int? = null
    //navigation
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var navDrawer: View
    // 드로어 내 뷰들
    private lateinit var projectListContainer: LinearLayout
    private lateinit var project1: Button
    private lateinit var project2: Button
    //private lateinit var binding2: CalendarMainViewBinding
    private var dtFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    private val addTaskLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                // TODO: 실제 서버 연동 시 최신 데이터 재로드
                loadSampleTasks()          // 데모용
                taskAdapter.updateData(sampleTasks)
            }
        }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentUserEmail = intent.getStringExtra("user_email") ?: ""
        Log.d("TeamMainActivity", "User Email: $currentUserEmail")

        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        //binding2 = CalendarMainViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//        binding.mainView.btnAddTask.setOnClickListener{
//            // TODO: 할 일 추가 ui 접목하여 유저 입력 받아오기
//            //addNewTask(title="새 할일","12:30","프로젝트3")
//            Toast.makeText(this,"할 일 생성: 새 할일", Toast.LENGTH_SHORT).show()
//            // TODO: 백엔드 연결 -> 할 일 서버에 저장(아님말고)
//        }

        setupCalendar()
        setupRecyclerView()
        //binding2.myCalendar.selectToday()
        binding.mainView.myCalendar.selectToday()
        // onCreate 내부, btnAddTask 클릭 리스너를 교체
//        binding.mainView.btnAddTask.setOnClickListener {
//            // AddTaskActivity로 이동
//            val intent = Intent(
//                this,
//                kr.ac.tukorea.planit.ui.add.AddTaskActivity::class.java
//            )
//            intent.putExtra("user_email", "example@gmail.com")
//            addTaskLauncher.launch(intent)
//        }
        val btnAddTask = findViewById<Button>(R.id.btnAddTask)

        btnAddTask.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java).apply {
                putExtra("user_email", currentUserEmail)   // 필요 정보 전달
            }
            startActivityForResult(intent, 100)           // 결과 필요 없으면 startActivity(intent)
        }

        findViewById<ImageView>(R.id.icon_hamberger).setOnClickListener{
            // TODO: 네비게이션 바 펼치기
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                // 열려있다면 닫기
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                // 닫혀있다면 열기
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        setupCalendar()
        // RecyclerView 초기화 및 설정
        setupRecyclerView()

        //네비게이션뷰
        setupDrawer()
        setupDrawerViews()
        setupProjectRecyclerView()
        setupButtonListeners()

        // 샘플 데이터 로드
        //실제 백엔드와 연동시 주석처리해주시면 됩니다.
        loadSampleTasks()
        //두번출현함
        //binding2.myCalendar.selectToday()

        binding.mainView.myCalendar.setOnClickListener {
            val intent = Intent(this,
                AddTaskActivity::class.java)
            addTaskLauncher.launch(intent)
        }
    }
    private fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
            ?: throw IllegalStateException("DrawerLayout not found")
        navDrawer = findViewById(R.id.nav_drawer)
            ?: throw IllegalStateException("Navigation drawer not found")

//        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
//        setSupportActionBar(toolbar)

        // 드로어 토글 설정
        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
//            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // 액션바에 햄버거 버튼 표시
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setHomeButtonEnabled(true)
    }

    private fun setupDrawerViews() {
        // 드로어 내 뷰들 참조
//        currentProjectName = navDrawer.findViewById(R.id.current_project_name)
//        projectListContainer = navDrawer.findViewById(R.id.project_list_container)
////        project1 = navDrawer.findViewById(R.id.project1)
////        project2 = navDrawer.findViewById(R.id.project2)
//        btnAddProject = navDrawer.findViewById(R.id.btn_add_project)
        currentProjectName = navDrawer.findViewById(R.id.current_project_name)
        projectRecyclerView = navDrawer.findViewById(R.id.project_recycler_view)
        btnAddProject = navDrawer.findViewById(R.id.btn_add_project)
    }

    private fun setupProjectRecyclerView() {
        projectAdapter = ProjectAdapter(
            onProjectClick = { project ->
                selectProject(project)
            }
        )?: throw IllegalStateException("projectAdapter not found")

        projectRecyclerView.adapter = projectAdapter
        projectRecyclerView.layoutManager = LinearLayoutManager(this)

        // 프로젝트 리스트 아이템 간격 설정
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
        projectRecyclerView.addItemDecoration(itemDecoration)
    }

    private fun setupButtonListeners() {
        btnAddProject.setOnClickListener {
            showAddProjectDialog()
        }
    }

    private fun selectProject(project: Project) {
//        // 현재 프로젝트명 업데이트
//        currentProjectName.text = projectName
//
//        // 선택된 프로젝트에 따른 처리
//        Toast.makeText(this, "$projectName 선택됨", Toast.LENGTH_SHORT).show()
//
//        // 여기에 프로젝트 변경에 따른 메인 콘텐츠 업데이트 로직 추가
//        updateMainContent(projectName)
//
//        // 드로어 닫기
//        drawerLayout.closeDrawer(GravityCompat.START)
        selectedProjectId = project.id
        currentProjectName.text = project.name

        // 어댑터에서 선택된 프로젝트 표시
        projectAdapter.selectProject(project.id)

        Toast.makeText(this, "${project.name} 선택됨", Toast.LENGTH_SHORT).show()

        // 메인 콘텐츠 업데이트
        updateMainContent(project.name)

        // 선택된 프로젝트에 따른 태스크 필터링
        //filterTasksByProject(project.name)

        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun updateMainContent(projectName: String) {
        // 메인 콘텐츠 영역 업데이트
        val mainContent = findViewById<LinearLayout>(R.id.main_view)
        // 프로젝트 메인 페이지로 이동
        val intent= Intent(this, TeamMainActivity::class.java)
        startActivity(intent)
        finish()

        // 예시: 액션바 타이틀 변경
        supportActionBar?.title = projectName
    }

    private fun showAddProjectDialog() {
        // 새 프로젝트 추가 다이얼로그 표시
        Toast.makeText(this, "새 프로젝트 추가 기능", Toast.LENGTH_SHORT).show()

        // TODO: id/name 타 액티비티에서 받아오기
        val newProject = Project(
            id = System.currentTimeMillis().toInt(),
            name = "새 프로젝트 ${System.currentTimeMillis() % 1000}"
        )

        addProject(newProject)
        Toast.makeText(this, "새 프로젝트가 추가되었습니다", Toast.LENGTH_SHORT).show()

        //drawerLayout.closeDrawer(GravityCompat.START)
    }

    fun addProject(project: Project) {
        projectAdapter.addProject(project)
        // TODO: 서버에 프로젝트 저장
        // apiService.addProject(project)
    }

    fun removeProject(projectId: Int) {
        projectAdapter.removeProject(projectId)
        // TODO: 서버에서 프로젝트 삭제
        //근데 이거 쓰나?
        // apiService.removeProject(projectId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (drawerToggle.onOptionsItemSelected(item)) {
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupCalendar() {
        Log.d("MainActivity", "setupCalendar() 호출됨")
        with(binding.mainView) {
            myCalendar.setOnDateSelectedListener { selectedDate ->
                Log.d("MainActivity", "선택된 날짜: $selectedDate")
                val inputFormatter = DateTimeFormatter.ofPattern("yyyy.M.d")
                val parsedDate = LocalDate.parse(selectedDate, inputFormatter)
                val isoString = parsedDate.atStartOfDay().toString()
                val selectedLocalDate=
                    LocalDate.parse(selectedDate, DateTimeFormatter.ofPattern("yyyy.M.d"))

                // 1) 서버로 보낼 JSON
                val json = """
                    {
                      "user_email": "$currentUserEmail",
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
                        put("team_name",  "")
                        put("task_target","")
                    }
                    connection.outputStream.use { it.write(jsonInput.toString().toByteArray()) }

                    // 3. 응답(문자열) & 원본 로그
                    val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("DBG", "RAW = $responseStr")

                    // 4. JSON 파싱 → List<Task>
                    val rootObj = JSONObject(responseStr)
                    val jsonArr = rootObj.getJSONArray("task")          // 서버가 'task' 배열 반환
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
                                    !selectedLocalDate.isAfter(end)   && // 선택일 ≤ end
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

    private fun parseTasks(json: String): List<Task> {
        val jsonObj = JSONObject(json)
        val jsonArray = jsonObj.getJSONArray("task")
        val tasks = mutableListOf<Task>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val task = Task(
                id = item.optInt("id"),
                teamName = item.optString("team_name"),
                taskName = item.optString("task_name"),
                taskStart = item.optString("task_start"),
                taskEnd = item.optString("task_end"),
                taskState = item.optBoolean("task_state"),
                taskTarget = item.optString("task_target"),
                userEmail = item.optString("user_email")
            )
            tasks.add(task)
        }
        return tasks
    }
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun setupCalendar() {
//        // 캘린더에서 날짜가 선택될 때 호출되는 리스너 설정
//        with(binding.mainView) {
//            myCalendar.setOnDateSelectedListener { selectedDate ->
//                // 선택된 날짜 표시
//                //tvSelectedDate.text = "선택된 날짜: $selectedDate"
//
//                // 선택된 날짜에 따른 추가 작업
//                // TODO: 선택된 날짜별 할 일 불러오기
//                // 날짜에 해당하는 태스크만 필터링
//
//                try {
//                    val inputFormatter = DateTimeFormatter.ofPattern("yyyy.M.d") // ex: "2025.7.18"
//                    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//                    val selectedLocalDate = LocalDate.parse(selectedDate, inputFormatter)
//
//                    Log.d("Debug", "selectedDate: $selectedLocalDate")
//
//                    val filteredTasks = sampleTasks.filter { task ->
//                        try {
//                            val hasStart = task.taskStart.isNotBlank()
//                            val hasEnd = task.taskEnd.isNotBlank()
//
//                            val startDate = if (hasStart)
//                                LocalDateTime.parse(task.taskStart, dateFormatter).toLocalDate()
//                            else null
//
//                            val endDate = if (hasEnd)
//                                LocalDateTime.parse(task.taskEnd, dateFormatter).toLocalDate()
//                            else null
//
//                            when {
//                                hasStart && hasEnd -> !selectedLocalDate.isBefore(startDate) && !selectedLocalDate.isAfter(endDate)
//                                hasStart -> selectedLocalDate == startDate
//                                hasEnd -> selectedLocalDate == endDate
//                                else -> false
//                            }
//                        } catch (e: Exception) {
//                            false
//                        }
//                    }
//
//                    Log.d("MainActivity", "Filtered tasks count: ${filteredTasks.size}")
//                    filteredTasks.forEach { task ->
//                        Log.d("MainActivity", "Task: id=${task.id}, taskStart=${task.taskStart}, taskEnd=${task.taskEnd}, title=${task.taskName}")
//                    }
//
//                    taskAdapter.updateData(filteredTasks)
//
//                } catch (e: Exception) {
//                    Log.e("MainActivity", "날짜 파싱 오류", e)
//                    taskAdapter.updateData(emptyList())
//                }
//
//            }
//        }
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
                // 체크박스 클릭 시 실행될 코드
                taskAdapter.updateTaskCompletion(task.id, isChecked)

                updateTaskCompletionStatus(task, isChecked)

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
                outRect.bottom = 32 // 아이템 간 16dp 간격
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
            Task(id = 1, teamName = "Team Alpha", taskName = "기획안 작성", taskStart = "2025-07-15 09:00:00", taskEnd = "2025-07-15 12:00:00", taskState = false, taskTarget = "프로젝트 A", userEmail = "user1@example.com"),
            Task(id = 2, teamName = "Team Alpha", taskName = "디자인 회의", taskStart = "2025-07-16 14:00:00", taskEnd = "2025-07-16 15:30:00", taskState = false, taskTarget = "프로젝트 A", userEmail = "user2@example.com"),
            Task(id = 3, teamName = "Team Beta", taskName = "개발 작업", taskStart = "2025-07-16 00:00:00", taskEnd = "2025-07-18 18:00:00", taskState = false, taskTarget = "프로젝트 B", userEmail = "user3@example.com"),
            Task(id = 4, teamName = "Team Beta", taskName = "기능 테스트", taskStart = "2025-07-16 00:00:00", taskEnd = "2025-07-17 23:59:59", taskState = false, taskTarget = "프로젝트 B", userEmail = "user4@example.com"),
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
        // 실제 구현에서는 데이터베이스나 서버 API 호출
        // 예: database.updateTaskStatus(taskId, isCompleted)
        // 또는: apiService.updateTask(taskId, isCompleted)

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
                    // 같은 날짜: 시·분만
                    "   ${start.format(timeFormatter)}\n~ ${end.format(timeFormatter)}"
                } else {
                    // 날짜 다르면 월·일만
                    "~ ${end.format(dateFormatter)}"
                }
            }
            else -> "         "
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
            //taskTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.todo_complete))
            taskTitle.paintFlags = taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            //taskProject.setTextColor(ContextCompat.getColor(itemView.context, R.color.todo_complete))
            //taskTime.setTextColor(ContextCompat.getColor(itemView.context, R.color.todo_complete))
        } else {
            // 미완료된 태스크: 원래 색상으로 복원
            //taskTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
            taskTitle.paintFlags = taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            //taskProject.setTextColor(Color.parseColor("#9A9A9A"))
            //taskTime.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
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
            // 실제 데이터 업데이트
            updateTaskCompletion(clickedTask.id, isChecked)
            // 콜백 호출
            onCheckboxClick(clickedTask, isChecked)
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

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        // 정렬: 완료 여부 → 마감일 빠른 순
        val sortedList = newList.sortedWith { a, b ->
            /* 1) 완료 상태: false(미완료) 먼저 */
            if (a.taskState != b.taskState)
                return@sortedWith a.taskState.compareTo(b.taskState)

            /* 2) 마감 임박한 일정이 위 */
            val endA = runCatching { LocalDateTime.parse(a.taskEnd, iso) }.getOrNull() ?: LocalDateTime.MAX
            val endB = runCatching { LocalDateTime.parse(b.taskEnd, iso) }.getOrNull() ?: LocalDateTime.MAX
            val diff = endA.compareTo(endB)
            if (diff != 0) return@sortedWith diff

            /* 3) (마감이 같다면) 시작 시각 빠른 것이 위 */
            val startA = runCatching { LocalDateTime.parse(a.taskStart, iso) }.getOrNull() ?: LocalDateTime.MAX
            val startB = runCatching { LocalDateTime.parse(b.taskStart, iso) }.getOrNull() ?: LocalDateTime.MAX
            startA.compareTo(startB)
        }

        taskList.addAll(sortedList)
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

// 2. 프로젝트 어댑터 클래스
class ProjectAdapter(
    private var projectList: MutableList<Project> = mutableListOf(),
    private val onProjectClick: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val projectButton: TextView = itemView.findViewById(R.id.project_name)

        fun bind(project: Project) {
            projectButton.text = project.name

            // 선택된 프로젝트 표시 (선택사항)
//            if (project.isSelected) {
//                projectButton.setBackgroundColor(
//                    ContextCompat.getColor(itemView.context, R.color.selected_project_color)
//                )
//            } else {
//                projectButton.background =
//                    ContextCompat.getDrawable(itemView.context, R.drawable.bg_project_button)
//            }

            projectButton.setOnClickListener {
                onProjectClick(project)
                // TODO: 프로젝트별 할 일 페이지 이동 : 이거까진 구현 힘들지도
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sidebar_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(projectList[position])
    }

    override fun getItemCount(): Int = projectList.size

    fun updateData(newList: List<Project>) {
        projectList.clear()
        projectList.addAll(newList)
        notifyDataSetChanged()
    }

    fun addProject(project: Project) {
        projectList.add(project)
        notifyItemInserted(projectList.size - 1)
    }

    fun removeProject(projectId: Int) {
        val index = projectList.indexOfFirst { it.id == projectId }
        if (index != -1) {
            projectList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun selectProject(projectId: Int) {
        projectList.forEachIndexed { index, project ->
            val newProject = project.copy(isSelected = project.id == projectId)
            projectList[index] = newProject
        }
        notifyDataSetChanged()
    }
}