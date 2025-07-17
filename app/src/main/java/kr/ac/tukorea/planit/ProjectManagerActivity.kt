package kr.ac.tukorea.planit

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import androidx.core.view.WindowCompat
import kr.ac.tukorea.planit.databinding.ActivityProjectManagerBinding
import android.graphics.Color
import android.view.Gravity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import android.widget.Toast




class ProjectManagerActivity : ComponentActivity() {
    private lateinit var binding: ActivityProjectManagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityProjectManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 프로젝트 초대 버튼 클릭 이벤트

        binding.btnInvite.setOnClickListener {
            val email = binding.editInviteEmail.text.toString()
            val projectId = intent.getStringExtra("projectId") ?: ""

            if (email.isNotBlank()) {
                inviteMember(email, projectId)  // ✅ projectId 추가
            } else {
                Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 👉 로그인한 사용자 이메일 가져오기
        val userEmail = intent.getStringExtra("userEmail") ?: ""

        // 👉 현재 프로젝트 ID 가져오기
        val projectId = intent.getStringExtra("projectId") ?: ""

        // ✅ 서버에 팀장 여부를 요청
        checkIsLeader(userEmail, projectId)
        fetchTeamMembers(projectId)


        val currentUserEmail = intent.getStringExtra("email") ?: ""

        val deleteButton = findViewById<Button>(R.id.btnDeleteProject)

        if (currentUserEmail == leaderEmail) {
            deleteButton.visibility = View.VISIBLE
        } else {
            deleteButton.visibility = View.GONE
        }

        binding.btnDeleteProject.setOnClickListener {
            val pid = intent.getStringExtra("projectId") ?: return@setOnClickListener
            deleteProject(pid)
        }
    }


    private fun fetchTeamMembers(projectId: String) {
        Thread {
            try {
                val url = URL("http://56.155.134.194:5000/get_team_members")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.doOutput = true

                val jsonInput = JSONObject()
                jsonInput.put("project_id", projectId)

                connection.outputStream.use {
                    it.write(jsonInput.toString().toByteArray(Charsets.UTF_8))
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val membersArray = JSONArray(response)

                val memberList = mutableListOf<TeamMember>()
                for (i in 0 until membersArray.length()) {
                    val obj = membersArray.getJSONObject(i)
                    val name = obj.getString("name")
                    val email = obj.getString("email")
                    memberList.add(TeamMember(name, email))
                }

                runOnUiThread {
                    displayTeamMembers(memberList)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun displayTeamMembers(memberList: List<TeamMember>) {
        val container = binding.layoutMemberList
        container.removeAllViews()

        val isLeader = binding.btnDeleteProject.visibility == View.VISIBLE
        val currentUserEmail = intent.getStringExtra("email") ?: ""

        // 팀장을 먼저 추가
        val leader = memberList.find { it.email == leaderEmail }
        leader?.let {
            container.addView(createMemberView(it.name, "팀장", true, false))
        }

        // 팀장을 제외한 나머지 팀원 추가
        memberList.filter { it.email != leaderEmail }.forEach { member ->
            val showRemove = isLeader  // 팀장일 경우에만 버튼 보이게
            val isCurrentUser = (member.email == currentUserEmail) // 자기 자신은 내보내기 못하게

            container.addView(
                createMemberView(
                    member.name,
                    "팀원",
                    false,
                    showRemove && !isCurrentUser,
                    member.email
                )
            )
        }
    }

    private fun createMemberView(
        name: String,
        role: String,
        isLeader: Boolean,
        showRemoveButton: Boolean,
        email: String = ""
    ): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 20, 0, 20)
            gravity = Gravity.CENTER_VERTICAL
        }

        val profileImage = ImageView(this).apply {
            setImageResource(R.drawable.ic_member)
            layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                marginEnd = 24
            }
        }

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameText = TextView(this).apply {
            text = name
            textSize = 16f
            setTextColor(Color.parseColor("#4B5563"))
            typeface = ResourcesCompat.getFont(this@ProjectManagerActivity, R.font.pretendard_medium)
        }

        val roleText = TextView(this).apply {
            text = role
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@ProjectManagerActivity, R.color.gray))
            typeface = ResourcesCompat.getFont(this@ProjectManagerActivity, R.font.pretendard_medium)
        }

        textContainer.addView(nameText)
        textContainer.addView(roleText)

        layout.addView(profileImage)
        layout.addView(textContainer)

        if (isLeader) {
            val star = ImageView(this).apply {
                setImageResource(R.drawable.ic_star)
                layoutParams = LinearLayout.LayoutParams(40, 40)
            }
            layout.addView(star)
        }

        if (showRemoveButton) {
            val removeBtn = Button(this).apply {
                text = "내보내기"
                setOnClickListener {
                    removeTeamMember(email)
                }
            }
            layout.addView(removeBtn)
        }

        return layout
    }


    private fun checkIsLeader(userEmail: String, projectId: String) {
        thread {
            try {
                val url = URL("http://56.155.134.194:5000/is_leader")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                }

                val jsonBody = JSONObject().apply {
                    put("email", userEmail)
                    put("project_id", projectId)
                }

                connection.outputStream.use {
                    it.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                }

                val responseJson = JSONObject(
                    connection.inputStream.bufferedReader().use { it.readText() }
                )
                val isLeader = responseJson.getBoolean("is_leader")

                runOnUiThread {
                    if (isLeader) {
                        leaderEmail = userEmail // ✅ 팀장 이메일 저장
                        binding.btnDeleteProject.visibility = View.VISIBLE
                    } else {
                        binding.btnDeleteProject.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "팀장 여부 확인 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun removeTeamMember(email: String) {
        val projectId = intent.getStringExtra("projectId") ?: return

        thread {
            try {
                val url = URL("http://56.155.134.194:5000/remove_member")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                }

                val jsonBody = JSONObject().apply {
                    put("email", email)
                    put("project_id", projectId)
                }

                connection.outputStream.use {
                    it.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                }

                runOnUiThread {
                    Toast.makeText(this, "팀원을 내보냈습니다", Toast.LENGTH_SHORT).show()
                    fetchTeamMembers(projectId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "팀원 내보내기 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteProject(projectId: String) {
        thread {
            try {
                val url = URL("http://56.155.134.194:5000/delete_project")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                }

                val jsonBody = JSONObject().apply {
                    put("project_id", projectId)
                }

                connection.outputStream.use {
                    it.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                }

                runOnUiThread {
                    Toast.makeText(this, "프로젝트가 삭제되었습니다", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "프로젝트 삭제 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun inviteMember(email: String, projectId: String) {
        thread {
            try {
                val url = URL("http://56.155.134.194:5000/invite_member")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                }

                val jsonBody = JSONObject().apply {
                    put("email", email)
                    put("project_id", projectId)
                }

                connection.outputStream.use {
                    it.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }

                runOnUiThread {
                    Toast.makeText(this, "초대 성공: $email", Toast.LENGTH_SHORT).show()
                    binding.editInviteEmail.text.clear()
                    fetchTeamMembers(projectId) // 리스트 갱신
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "초대 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private var leaderEmail: String = ""


}

