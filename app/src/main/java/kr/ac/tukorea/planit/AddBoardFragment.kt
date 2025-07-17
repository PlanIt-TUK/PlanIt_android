package kr.ac.tukorea.planit

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.android.volley.*
import com.android.volley.toolbox.*
import org.json.JSONObject

class AddBoardFragment : Fragment(R.layout.fragment_add_board) {

    private val teamName = "TeamA" // TODO: 실제 로그인 데이터로 교체

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // + 새 보드 추가 버튼 클릭 시 다이얼로그 표시
        view.findViewById<Button>(R.id.btnAddTask).setOnClickListener {
            showAddBoardDialog()
        }
    }

    private fun showAddBoardDialog() {
        val d = Dialog(requireContext())
        d.setContentView(R.layout.dialog_add_board)
        d.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val titleIn = d.findViewById<EditText>(R.id.editTitle)
        val grid = d.findViewById<GridLayout>(R.id.colorGrid)
        var selIdx = 0

        repeat(grid.childCount) { i ->
            grid.getChildAt(i).setOnClickListener {
                selIdx = i
                repeat(grid.childCount) { j ->
                    grid.getChildAt(j).alpha = if (j == i) 1f else 0.3f
                }
            }
        }

        d.findViewById<Button>(R.id.btnAddTask).setOnClickListener {
            val name = titleIn.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(context, "보드명을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addBoard(name, selIdx)
            d.dismiss()
        }

        d.findViewById<Button>(R.id.btnCancel).setOnClickListener { d.dismiss() }
        d.findViewById<ImageButton>(R.id.btn_back).setOnClickListener { d.dismiss() }

        d.show()
    }

    private fun addBoard(name: String, color: Int) {
        val ctx = requireContext()
        val url = "http://56.155.134.194:8000/add_board"
        val body = JSONObject().apply {
            put("team_name", teamName)
            put("board_name", name)
            put("board_color", color)
            put("card_name", "")
            put("card_content", "")
        }

        val req = object : JsonObjectRequest(Method.POST, url, body,
            {
                Toast.makeText(ctx, "보드가 추가되었습니다", Toast.LENGTH_SHORT).show()
            },
            { e ->
                val txt = e.networkResponse?.data?.let { String(it) } ?: ""
                Log.e("VolleyError", txt, e)
                Toast.makeText(ctx, "보드 추가 실패: $txt", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
                val body = response.data?.toString(Charsets.UTF_8)?.trim()
                val json = if (body.isNullOrEmpty() || body == "null") JSONObject() else JSONObject(body)
                return Response.success(json, HttpHeaderParser.parseCacheHeaders(response))
            }
        }

        Volley.newRequestQueue(ctx).add(req)
    }
}
