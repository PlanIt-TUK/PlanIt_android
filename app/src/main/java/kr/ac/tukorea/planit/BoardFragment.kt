package kr.ac.tukorea.planit

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.Volley
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.Request
import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONException

class BoardFragment : Fragment(R.layout.fragment_board) {

    // ───────────────────────── helper ─────────────────────────
    /** null ⇒ "" 로 치환해서 JSONObject.put() 예외 방지 */
    private fun safe(v: String?): String = v ?: ""

    // ───────────────────────── properties ─────────────────────
    private var teamName: String = ""
    private var boardName: String = ""
    private var boardColor: Int  = 0

    // ───────────────────────── lifecycle ──────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            teamName   = it.getString("teamName" , "") ?: ""
            boardName  = it.getString("boardName", "") ?: ""
            boardColor = it.getInt   ("boardColor", 0)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadBoardCards(view)

        view.findViewById<View>(R.id.addTask)?.setOnClickListener {
            showAddCardDialog()
        }
    }

    // ───────────────────────── load / render ──────────────────
    private fun loadBoardCards(rootView: View) {
        val context = requireContext()

        val payload = JSONObject().apply {
            put("team_name" , safe(teamName))
            put("board_name", safe(boardName))
        }

        val queue = Volley.newRequestQueue(context)
        val url   = "http://56.155.134.194:8000/load_board"

        queue.add(
            JsonObjectRequest(
                Request.Method.POST, url, payload,
                { res ->
                    try {
                        renderCards(res.getJSONArray("board"), rootView)
                    } catch (e: JSONException) {
                        Toast.makeText(context, "파싱 오류", Toast.LENGTH_SHORT).show()
                        Log.e("BoardFragment", "JSON parse error", e)
                    }
                },
                { err ->
                    val body = err.networkResponse?.data?.let { String(it) } ?: ""
                    Log.e("VolleyError", "code=${err.networkResponse?.statusCode}, $body", err)
                    Toast.makeText(context, "불러오기 실패\n$body", Toast.LENGTH_LONG).show()
                }
            )
        )
    }

    private fun renderCards(arr: JSONArray, root: View) {
        val postList = root.findViewById<LinearLayout>(R.id.postList)
        postList.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val title   = obj.optString("card_name"   , "")
            val content = obj.optString("card_content", "")

            if (title.isBlank() && content.isBlank()) continue   // 빈 카드 skip

            val card = inflater.inflate(R.layout.item_card, postList, false)
            card.findViewById<TextView>(R.id.authorText ).text = obj.optString("author", "익명")
            card.findViewById<TextView>(R.id.timeText   ).text = "방금 전" // TODO created_at → 상대시간
            card.findViewById<TextView>(R.id.contentText).text = content
            postList.addView(card)
        }
    }

    // ───────────────────────── dialog ─────────────────────────
    private fun showAddCardDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_add_post)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val authorIn  = dialog.findViewById<EditText>(R.id.editTitle)
        val titleIn   = dialog.findViewById<EditText>(R.id.editStartDate)
        val contentIn = dialog.findViewById<EditText>(R.id.postContent)

        dialog.findViewById<ImageButton>(R.id.btn_back ).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener      { dialog.dismiss() }

        dialog.findViewById<Button>(R.id.btnAddTask).setOnClickListener {
            val author  = authorIn.text.toString().trim()
            val title   = titleIn.text.toString().trim()
            val content = contentIn.text.toString().trim()

            if (author.isBlank() || title.isBlank() || content.isBlank()) {
                Toast.makeText(requireContext(), "모든 항목을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendAddCardRequest(author, title, content)
            dialog.dismiss()
        }

        dialog.show()
    }

    // ───────────────────────── POST add card ──────────────────
    private fun sendAddCardRequest(author: String, title: String, content: String) {
        val context = requireContext()

        val payload = JSONObject().apply {
            put("team_name"   , safe(teamName))
            put("board_name"  , safe(boardName))
            put("board_color" , boardColor)
            put("card_name"   , safe(title))
            put("card_content", safe(content))
        }

        val queue = Volley.newRequestQueue(context)
        val url   = "http://56.155.134.194:8000/add_board"

        queue.add(
            JsonObjectRequest(
                Request.Method.POST, url, payload,
                { _ ->
                    Toast.makeText(context, "게시물이 추가되었습니다", Toast.LENGTH_SHORT).show()
                    loadBoardCards(requireView())      // 목록 갱신
                },
                { err ->
                    val body = err.networkResponse?.data?.let { String(it) } ?: ""
                    Log.e("VolleyError", "code=${err.networkResponse?.statusCode}, $body", err)
                    Toast.makeText(context, "에러 발생\n$body", Toast.LENGTH_LONG).show()
                }
            )
        )
    }
}
