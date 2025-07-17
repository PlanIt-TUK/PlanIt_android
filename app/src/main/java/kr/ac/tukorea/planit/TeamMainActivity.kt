package kr.ac.tukorea.planit

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.Fragment
import kr.ac.tukorea.planit.CalenderBoardFragment

class TeamMainActivity : AppCompatActivity() {

    /** 탭 제목 3개 */
    private val tabTitles = listOf("캘린더", "게시판", "보드리스트")

    /** 각 탭에 대응하는 Fragment */
    private val fragments by lazy {
        listOf<Fragment>(
            CalenderBoardFragment(), // ① 캘린더
            BoardFragment().apply {                      // ② 게시판(고정 보드)
                arguments = bundleOf(
                    "teamName"  to "TeamA",
                    "boardName" to "게시판",
                    "boardColor" to 3
                )
            },
            AddBoardFragment()                           // ③ 보드 리스트 + 새 보드 추가
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_main)

        // 시스템 바(inset) 패딩 유지(선택)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tabLayout)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        //탑바 세팅 버튼
        findViewById<ImageButton>(R.id.btnSetting).setOnClickListener{
            val intent=Intent(this, ProjectManagerActivity::class.java)
            startActivity(intent)
        }

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        /* 탭 3개 추가 */
        tabTitles.forEach { title ->
            tabLayout.addTab(tabLayout.newTab().setText(title))
        }

        /* 초기 화면: 캘린더Fragment */
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragments[0])
            .commit()

        /* 탭 클릭 시 Fragment 교체 */
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragments[tab.position])
                    .commit()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
}
