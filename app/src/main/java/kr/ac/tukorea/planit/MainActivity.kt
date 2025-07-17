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
    private lateinit var binding: ActivityMainBinding
    // RecyclerView와 Adapter 변수 선언
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var sampleTasks: List<Task>

    private val addTaskLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                // TODO: 실제 서버 연동 시 최신 데이터 재로드
                loadSampleTasks()          // 데모용
                taskAdapter.updateData(sampleTasks)
            }
        }

    //navigation
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var navDrawer: View
    // 드로어 내 뷰들
    private lateinit var currentProjectName: TextView
    private lateinit var projectListContainer: LinearLayout
    private lateinit var project1: Button
    private lateinit var project2: Button
    private lateinit var btnAddProject: Button
    private lateinit var binding2: CalendarMainViewBinding

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding2 = CalendarMainViewBinding.inflate(layoutInflater)
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
        // onCreate 내부, btnAddTask 클릭 리스너를 교체
        binding.mainView.btnAddTask.setOnClickListener {
            // AddTaskActivity로 이동
            val intent = Intent(
                this,
                kr.ac.tukorea.planit.ui.add.AddTaskActivity::class.java
            )
            intent.putExtra("user_email", "example@gmail.com")
            addTaskLauncher.launch(intent)
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
        setupButtonListeners()

        // 샘플 데이터 로드
        //실제 백엔드와 연동시 주석처리해주시면 됩니다.
        loadSampleTasks()
        binding2.myCalendar.selectToday()

        binding2.myCalendar.setOnClickListener {
            val intent = Intent(this,
                kr.ac.tukorea.planit.ui.add.AddTaskActivity::class.java)
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
        currentProjectName = navDrawer.findViewById(R.id.current_project_name)
        projectListContainer = navDrawer.findViewById(R.id.project_list_container)
        project1 = navDrawer.findViewById(R.id.project1)
        project2 = navDrawer.findViewById(R.id.project2)
        btnAddProject = navDrawer.findViewById(R.id.btn_add_project)
    }

    private fun setupButtonListeners() {
        // 프로젝트 1 버튼 클릭 리스너
        project1.setOnClickListener {
            selectProject("프로젝트 1")
        }

        // 프로젝트 2 버튼 클릭 리스너
        project2.setOnClickListener {
            selectProject("프로젝트 2")
        }

        // 새 할 일 추가 버튼 클릭 리스너
        btnAddProject.setOnClickListener {
            showAddProjectDialog()
        }
    }

    private fun selectProject(projectName: String) {
        // 현재 프로젝트명 업데이트
        currentProjectName.text = projectName

        // 선택된 프로젝트에 따른 처리
        Toast.makeText(this, "$projectName 선택됨", Toast.LENGTH_SHORT).show()

        // 여기에 프로젝트 변경에 따른 메인 콘텐츠 업데이트 로직 추가
        updateMainContent(projectName)

        // 드로어 닫기
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun updateMainContent(projectName: String) {
        // 메인 콘텐츠 영역 업데이트
        val mainContent = findViewById<LinearLayout>(R.id.main_view)
        // TODO: 특정 프로젝트 메인 페이지로 이동??

        // 예시: 액션바 타이틀 변경
        supportActionBar?.title = projectName
    }

    private fun showAddProjectDialog() {
        // 새 프로젝트 추가 다이얼로그 표시
        Toast.makeText(this, "새 프로젝트 추가 기능", Toast.LENGTH_SHORT).show()

        // TODO: 새 프로젝트 추가

        drawerLayout.closeDrawer(GravityCompat.START)
    }

    // 동적으로 프로젝트 버튼 추가하는 함수
    fun addProjectButton(projectName: String) {
        val button = Button(this)
        button.text = projectName
        button.setTextColor(resources.getColor(R.color.gray, null))
        button.textSize = 15f
        button.background = resources.getDrawable(R.drawable.bg_project_button, null)
        button.typeface = resources.getFont(R.font.pretendard_semibold)

        // 마진 설정
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(
            resources.getDimensionPixelSize(R.dimen.dp_50),
            resources.getDimensionPixelSize(R.dimen.dp_12),
            resources.getDimensionPixelSize(R.dimen.dp_50),
            resources.getDimensionPixelSize(R.dimen.dp_12)
        )
        button.layoutParams = layoutParams

        // 클릭 리스너 설정
        button.setOnClickListener {
            selectProject(projectName)
        }

        // 새 할 일 추가 버튼 앞에 추가
        val addButtonIndex = projectListContainer.indexOfChild(btnAddProject)
        projectListContainer.addView(button, addButtonIndex)
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
        // 캘린더에서 날짜가 선택될 때 호출되는 리스너 설정
        with(binding.mainView) {
            myCalendar.setOnDateSelectedListener { selectedDate ->
                // 선택된 날짜 표시
                //tvSelectedDate.text = "선택된 날짜: $selectedDate"

                // 선택된 날짜에 따른 추가 작업
                // TODO: 선택된 날짜별 할 일 불러오기
                // 날짜에 해당하는 태스크만 필터링

                try {
                    val inputFormatter = DateTimeFormatter.ofPattern("yyyy.M.d") // ex: "2025.7.18"
                    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val selectedLocalDate = LocalDate.parse(selectedDate, inputFormatter)

                    Log.d("Debug", "selectedDate: $selectedLocalDate")

                    val filteredTasks = sampleTasks.filter { task ->
                        try {
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
        updateTaskCompletionStatus(task.id, isChecked)

        // 완료/미완료 상태에 따른 메시지 표시
        val message = if (isChecked) "태스크 완료: ${task.taskName}" else "태스크 미완료: ${task.taskName}"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 태스크 완료 상태를 업데이트하는 메서드 (실제 데이터 저장)
     * @param taskId 업데이트할 태스크 ID
     * @param isCompleted 새로운 완료 상태
     */
    private fun updateTaskCompletionStatus(taskId: Int, isCompleted: Boolean) {
        // 실제 구현에서는 데이터베이스나 서버 API 호출
        // 예: database.updateTaskStatus(taskId, isCompleted)
        // 또는: apiService.updateTask(taskId, isCompleted)
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
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
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

        // 정렬: 완료 여부 → 마감일 빠른 순
        val sortedList = newList.sortedWith(compareBy<Task> { it.taskState }  // false(미완료) → true(완료)
            .thenBy {
                try {
                    LocalDateTime.parse(it.taskEnd, formatter)
                } catch (e: Exception) {
                    LocalDateTime.MAX // 파싱 실패 시 가장 뒤로
                }
            }
        )

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