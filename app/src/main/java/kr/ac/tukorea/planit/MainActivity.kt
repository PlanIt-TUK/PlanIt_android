package kr.ac.tukorea.planit

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

// 1. 태스크 데이터 클래스
data class Task(
    val id: Int,
    val time: String,
    val title: String,
    val project: String,
    val isCompleted: Boolean = false
)

class MainActivity : AppCompatActivity() {

    private val TAG = this::class.simpleName
    private lateinit var binding: CalendarMainViewBinding
    // RecyclerView와 Adapter 변수 선언
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = CalendarMainViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupCalendar()
        // RecyclerView 초기화 및 설정
        setupRecyclerView()

        // 샘플 데이터 로드
        //실제 백엔드와 연동시 주석처리해주시면 됩니다.
        loadSampleTasks()

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
        val sampleTasks = listOf(
            Task(1, "09:00", "프로젝트 미팅", "프로젝트 A"),
            Task(2, "10:30", "코드 리뷰", "프로젝트 B"),
            Task(3, "14:00", "디자인 검토", "프로젝트 A"),
            Task(4, "16:00", "개발 작업", "프로젝트 C"),
            Task(5, "18:00", "문서 작성", "프로젝트 B", true) // 완료된 태스크
        )

        taskAdapter.updateData(sampleTasks)
    }

    /**
     * 태스크 아이템 클릭 시 실행되는 메서드
     * @param task 클릭된 Task 객체
     */
    private fun handleTaskClick(task: Task) {
        // 태스크 상세 정보 표시 또는 편집 화면으로 이동
        Toast.makeText(
            this,
            "태스크 선택: ${task.title}",
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
        val message = if (isChecked) "태스크 완료: ${task.title}" else "태스크 미완료: ${task.title}"
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
    fun addNewTask(title: String, time: String, project: String) {
        val newTask = Task(
            id = System.currentTimeMillis().toInt(),
            time = time,
            title = title,
            project = project
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
        taskTime.text = task.time
        taskTitle.text = task.title
        taskProject.text = task.project
        taskCheckBox.isChecked = task.isCompleted

        // 완료 상태에 따른 UI 변경
        updateUIForCompletionState(task.isCompleted)

        // 아이템 클릭 리스너 설정 (체크박스 제외)
        itemView.setOnClickListener {
            onItemClick(task)
        }

        // 체크박스 클릭 리스너 설정 (리스너 재설정)
        taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            // 현재 상태와 다를 때만 콜백 호출
            if (isChecked != task.isCompleted) {
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
        taskList.addAll(newList)
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
            taskList[index] = taskList[index].copy(isCompleted = isCompleted)
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
            if (task.isCompleted) index else null
        }.reversed() // 뒤에서부터 삭제

        completedIndices.forEach { index ->
            taskList.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}