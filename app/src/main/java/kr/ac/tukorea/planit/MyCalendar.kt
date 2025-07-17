package kr.ac.tukorea.planit

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import kr.ac.tukorea.planit.databinding.CalendarDayLayoutBinding
import kr.ac.tukorea.planit.databinding.MyCalendarBinding
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class MyCalendar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = "MyCalendar"
    private val binding: MyCalendarBinding =
        MyCalendarBinding.inflate(LayoutInflater.from(context), this, true)

    private var selectedDate: LocalDate? = null
    @RequiresApi(Build.VERSION_CODES.O)
    private var currentMonth = YearMonth.now()

    private var onDateSelectedListener: ((String) -> Unit)? = null

    fun setOnDateSelectedListener(listener: (String) -> Unit) {
        onDateSelectedListener = listener
    }

    // 선택된 날짜를 "yyyy년 MM월 dd일" 형태로 반환
    // 달력 클릭 후 받는 연월일 형태를 수정하려면 이 함수를 수정
    @RequiresApi(Build.VERSION_CODES.O)
    fun getSelectedDateFormatted(): String {
        return selectedDate?.let {
            "${it.year}.${it.monthValue}.${it.dayOfMonth}"
        } ?: "${currentMonth.year}.${currentMonth.monthValue}"
    }

    init {
        setupCalendar()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupCalendar() {
        with(binding) {
            // 현재 연월 표시
            updateYearMonthText()

            // 이전 달 버튼(<) 클릭 리스너
            btnPrevMonth.setOnClickListener {
                currentMonth = currentMonth.minusMonths(1)
                mcCustom.smoothScrollToMonth(currentMonth)
                updateYearMonthText()
            }

            // 다음 달 버튼(>) 클릭 리스너
            btnNextMonth.setOnClickListener {
                currentMonth = currentMonth.plusMonths(1)
                mcCustom.smoothScrollToMonth(currentMonth)
                updateYearMonthText()
            }

            //햄버거 메뉴 클릭 리스너
            iconHamberger.setOnClickListener{
                // TODO: 햄버거 메뉴 클릭시 사이드바 불러오기
            }

            // 오늘 날짜 이전, 이후 연월은 100개월 전까지 표시
            val startMonth = currentMonth.minusMonths(100)
            val endMonth = currentMonth.plusMonths(100)

            // 지정된 첫 번째 요일이 시작 위치에 오는 주간 요일 값
            // 실행 시 일요일이 먼저 표시됨
            val daysOfWeek: List<DayOfWeek> = daysOfWeek()

            mcCustom.setup(startMonth, endMonth, daysOfWeek.first())
            mcCustom.scrollToMonth(currentMonth)
            // 달력 스크롤 시
            mcCustom.monthScrollListener = { month ->
                Log.d(TAG, "## [스크롤 리스너] mouthScrollListener: $month")
                currentMonth = month.yearMonth
                //여기에 날짜에 따라 할 일 변경
                updateYearMonthText()
            }

            // 일~토 텍스트가 표시되는 상단 뷰
            mcCustom.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
                override fun create(view: View) = MonthViewContainer(view)
                override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                    if (container.titlesContainer.tag == null) {
                        container.titlesContainer.tag = data.yearMonth
                        container.titlesContainer.children.map { it as TextView }
                            .forEachIndexed { index, textView ->
                                val dayOfWeek = daysOfWeek[index]
                                val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                textView.text = title

                                // 일요일은 빨간색, 토요일은 파란색으로 한글 글자색 설정
                                // 현재 코드에서 이렇게 설정해도 이번 달 외의 날짜들은 회색으로 표시된다
                                //저희 프로젝트에선 활용하지 않으니 아래 코드는 주석처리 하였습니다.
//                                when (dayOfWeek) {
//                                    DayOfWeek.SUNDAY -> textView.setTextColor(Color.RED)
//                                    DayOfWeek.SATURDAY -> textView.setTextColor(Color.BLUE)
//                                    else -> textView.setTextColor(Color.BLACK)
//                                }
                            }
                    }
                }
            }

            // 날짜가 표시되는 뷰
            mcCustom.dayBinder = object : MonthDayBinder<DayViewContainer> {
                override fun create(view: View) = DayViewContainer(view)
                override fun bind(container: DayViewContainer, data: CalendarDay) {
                    container.textView.text = data.date.dayOfMonth.toString()

                    // 오늘 날짜 가져오기
                    val today = LocalDate.now()
                    val isFutureDate = data.date.isAfter(today)

                    // 텍스트 색상 설정
                    when {
//                        isFutureDate -> {
//                            // 미래 날짜는 항상 회색으로 표시
//                            container.textView.setTextColor(Color.GRAY)
//                        }
                        data.position == DayPosition.MonthDate -> {

                            // 현재 월에 속한 과거 또는 오늘 날짜는 요일에 따라 색상 설정
//                            when (data.date.dayOfWeek) {
//                                DayOfWeek.SUNDAY -> container.textView.setTextColor(Color.RED)
//                                DayOfWeek.SATURDAY -> container.textView.setTextColor(Color.BLUE)
//                                else -> container.textView.setTextColor(Color.BLACK)
//                            }
                            container.textView.setTextColor(Color.BLACK)
                        }

                        else -> {
                            // 이전/다음 달의 날짜는 회색
                            container.textView.setTextColor(Color.GRAY)
                        }
                    }

                    container.textView.background = null

                    // 선택된 날짜 스타일 적용 (미래 날짜가 아닌 경우만)
//                    if (selectedDate == data.date && !isFutureDate)
                    if (selectedDate == data.date) {
//                        // 원형 배경 설정
//                        container.textView.background = GradientDrawable().apply {
//                            shape = GradientDrawable.OVAL
//                            //colors.xml에서 불러오는게 안돼 임의로 했습니다.
//                            setColor(Color.parseColor("#2D6EEF"))
//                        }
                        //ui 디자인에 맞게 재설계한 부분입니다.
                        container.textView.apply {
                            background = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                setColor(ContextCompat.getColor(context, R.color.blue))
                                cornerRadius = 8f.dpToPx()
                            }
                            //이걸로 크기를 조절해서 ui에 맞추려고 했는데 왜인지 오류가 나는것같네요...
//                            layoutParams = layoutParams.apply {
//                                width = 15.dpToPx()
//                                height = 15.dpToPx()
//                            }
                        }

                        // 선택된 날짜는 흰색 텍스트
                        container.textView.setTextColor(ContextCompat.getColor(context, R.color.background))
                        container.textView.gravity = Gravity.CENTER
                    }

                    // 날짜 클릭 리스너 - 미래 날짜는 선택 불가
                    container.textView.setOnClickListener {
                        // 현재 월에 속한 과거 또는 오늘 날짜만 선택 가능
//                        if (data.position == DayPosition.MonthDate && !isFutureDate) {
//                            if (selectedDate != data.date) {
//                                val oldDate = selectedDate
//                                selectedDate = data.date
//
//                                // 이전 선택된 날짜 갱신
//                                oldDate?.let { date ->
//                                    mcCustom.notifyDateChanged(date)
//                                }
//
//                                // 새로 선택된 날짜 갱신 후 콜백에 전달
//                                mcCustom.notifyDateChanged(data.date)
//                                onDateSelectedListener?.invoke(getSelectedDateFormatted())
//                            }
//                        }
                        //이전 코드 : 오늘 이후 날짜 접근 불가 -> 오늘 이후 날짜 접근 가능(접근시 아래 텍스트 바뀜
                        if (selectedDate != data.date) {
                            val oldDate = selectedDate
                            selectedDate = data.date

                            // 이전 선택된 날짜 갱신
                            oldDate?.let { date ->
                                mcCustom.notifyDateChanged(date)
                            }

                            // 새로 선택된 날짜 갱신 후 콜백에 전달
                            mcCustom.notifyDateChanged(data.date)
                            onDateSelectedListener?.invoke(getSelectedDateFormatted())
                        }
                    }
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun selectToday() {
        val today = LocalDate.now()
        val oldDate = selectedDate
        selectedDate = today
        oldDate?.let { date -> binding.mcCustom.notifyDateChanged(date) }
        binding.mcCustom.notifyDateChanged(today)
        onDateSelectedListener?.invoke(getSelectedDateFormatted())
    }

    private fun updateYearMonthText() {
        binding.tvYearMonth.text = "${currentMonth.year}년 ${currentMonth.monthValue}월"
    }

    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
    fun Float.dpToPx(): Float = this * Resources.getSystem().displayMetrics.density
}

class DayViewContainer(view: View) : ViewContainer(view) {
    val textView = CalendarDayLayoutBinding.bind(view).calendarDayText
}

class MonthViewContainer(view: View) : ViewContainer(view) {
    val titlesContainer = view as ViewGroup
}

