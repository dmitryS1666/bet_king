package com.kett.bing.ui

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.kett.bing.MainActivity
import com.kett.bing.R
import java.text.SimpleDateFormat
import java.util.Locale

class InfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.homeButton).setOnClickListener {
            (activity as? MainActivity)?.openMainFragment()
        }

        val prefs = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val container = view.findViewById<LinearLayout>(R.id.gameResultsContainer)

        val winTimes = prefs.getStringSet("winTimes", emptySet()) ?: emptySet()
        val loseTimes = prefs.getStringSet("loseTimes", emptySet()) ?: emptySet()

        val combined = mutableListOf<Pair<String, String>>() // Pair<type, timestamp>

        winTimes.forEach { combined.add("win" to it) }
        loseTimes.forEach { combined.add("lose" to it) }

        // сортировка по дате убыванию (если формат dd.MM.yyyy\nHH:mm)
        val sortedCombined = combined.sortedByDescending {
            val cleanTime = it.second.replace("\n", " ")
            try {
                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).parse(cleanTime)
            } catch (e: Exception) {
                null
            }
        }

        if (sortedCombined.isEmpty()) {
            container.addView(createStatRow("none", "—"))
        } else {
            sortedCombined.forEach { (type, timestamp) ->
                container.addView(createStatRow(type, timestamp))
            }
        }
    }


    fun Int.dp(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    @RequiresApi(Build.VERSION_CODES.P)
    private fun createStatRow(type: String, timestamp: String): LinearLayout {
        val context = requireContext()

        val row = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65.dp(context)
            ).apply {
                setMargins(0, 0, 0, 10.dp(context)) // отступы между карточками
            }
            orientation = LinearLayout.HORIZONTAL
            setPadding(12.dp(context), 0, 12.dp(context), 0)
            background = context.getDrawable(R.drawable.stat_bg)
        }

        val icon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                130.dp(context),
                30.dp(context)
            ).apply {
                setMargins(10.dp(context), 0, 0, 0)
                gravity = Gravity.CENTER_VERTICAL
            }
            setImageResource(if (type == "win") R.drawable.win_text else R.drawable.lose_text)
        }

        val timeView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                60.dp(context),
                1f
            )
            text = timestamp.replace(" ", "\n") // чтобы отображался как "19.06.2025\n17:35"
            textSize = 25f
            lineHeight = 65
            setTextColor(Color.parseColor("#FA8800"))
            gravity = Gravity.CENTER
        }

        row.addView(icon)
        row.addView(timeView)

        return row
    }

}
