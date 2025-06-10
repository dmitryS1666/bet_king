package com.kett.bing.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.kett.bing.R
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment : Fragment() {

    private lateinit var barChart: BarChart
    private lateinit var percentText: TextView
    private lateinit var dateRange: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        barChart = view.findViewById(R.id.barChart)
        percentText = view.findViewById(R.id.percentText)
        dateRange = view.findViewById(R.id.dateRange)

        showChart()

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun showChart() {
        val trainingDates = getTrainingDates().sorted()

        val today = Calendar.getInstance()

        val showOnlyToday = trainingDates.isEmpty() ||
                trainingDates.maxOrNull()?.let {
                    val lastDate = Calendar.getInstance().apply { time = it }
                    val diffDays = ((today.timeInMillis - lastDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                    diffDays > 2
                } ?: true

        if (showOnlyToday) {
            // Показываем только текущий день с нулевыми данными
            val onlyToday = listOf(today.time)
            buildChart(onlyToday)
            percentText.text = "0%"
            val todayFormatted = SimpleDateFormat("d MMMM", Locale.getDefault()).format(today.time)
            dateRange.text = "Today - $todayFormatted"
            return
        }

        // Начинаем с минимальной даты из данных минус 2 дня
        val startDate = Calendar.getInstance().apply {
            time = trainingDates.minOrNull() ?: today.time
            add(Calendar.DAY_OF_YEAR, -2)
        }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        var completedCount = 0
        var totalDays = 0

        val calendar = startDate.clone() as Calendar
        val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())

        while (!calendar.after(today)) {
            val date = calendar.time
            val dateLabel = dateFormat.format(date)
            labels.add(dateLabel)

            val isDone = trainingDates.any { isSameDay(it, date) }
            val value = if (isDone) 1f else 0f
            if (isDone) completedCount++

            entries.add(BarEntry(totalDays.toFloat(), value))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            totalDays++
        }

        val dataSet = BarDataSet(entries, "Done")
        dataSet.color = Color.YELLOW
        dataSet.valueTextColor = Color.TRANSPARENT
        dataSet.valueTextSize = 0f

        val data = BarData(dataSet)
        barChart.data = data

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.WHITE

        val leftAxis = barChart.axisLeft
        leftAxis.setDrawLabels(false)
        leftAxis.setDrawGridLines(true)
        leftAxis.enableGridDashedLine(10f, 10f, 0f)

        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setScaleEnabled(false)
        barChart.setTouchEnabled(false)
        barChart.invalidate()

        val percent = if (totalDays > 0) (completedCount * 100) / totalDays else 0
        percentText.text = "$percent%"

        val todayFormatted = SimpleDateFormat("d MMMM", Locale.getDefault()).format(today.time)
        dateRange.text = "Today - $todayFormatted"
    }

    private fun buildChart(dates: List<Date>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())

        for ((index, date) in dates.withIndex()) {
            labels.add(dateFormat.format(date))
            entries.add(BarEntry(index.toFloat(), 0f))
        }

        val dataSet = BarDataSet(entries, "Done")
        dataSet.color = Color.YELLOW
        dataSet.valueTextColor = Color.TRANSPARENT
        dataSet.valueTextSize = 0f

        val data = BarData(dataSet)
        barChart.data = data

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.WHITE

        val leftAxis = barChart.axisLeft
        leftAxis.setDrawLabels(false)
        leftAxis.setDrawGridLines(true)
        leftAxis.enableGridDashedLine(10f, 10f, 0f)

        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setScaleEnabled(false)
        barChart.setTouchEnabled(false)
        barChart.invalidate()
    }

    private fun getTrainingDates(): List<Date> {
        val prefs = requireContext().getSharedPreferences("WorkoutStats", android.content.Context.MODE_PRIVATE)
        val allEntries = prefs.all

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val result = mutableListOf<Date>()

        for ((key, _) in allEntries) {
            try {
                val date = format.parse(key)
                if (date != null) result.add(date)
            } catch (_: Exception) {
                // Пропускаем некорректные ключи
            }
        }

        return result
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(date1) == fmt.format(date2)
    }
}
