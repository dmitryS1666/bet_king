package com.kett.bing.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.kett.bing.R
import java.text.SimpleDateFormat
import java.util.*

class PlanFragment : Fragment() {

    private lateinit var levelGroup: RadioGroup
    private lateinit var beginnerButton: RadioButton
    private lateinit var mediumButton: RadioButton
    private lateinit var advancedButton: RadioButton
    private lateinit var tableLayout: TableLayout

    private var currentLevel: String = "Beginner"

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_plan, container, false)

        val _sharedPreferences = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val isFirstTimeOnPlan = _sharedPreferences.getBoolean("isFirstTimeOnPlanFragment", true)

        if (isFirstTimeOnPlan) {
            Toast.makeText(requireContext(), "You can edit the plan by clicking on any row", Toast.LENGTH_LONG).show()
            _sharedPreferences.edit().putBoolean("isFirstTimeOnPlanFragment", false).apply()
        }

        levelGroup = view.findViewById(R.id.levelGroup)
        beginnerButton = view.findViewById(R.id.beginner)
        mediumButton = view.findViewById(R.id.medium)
        advancedButton = view.findViewById(R.id.advanced)
        tableLayout = view.findViewById(R.id.planTable)

        val sharedPreferences = requireContext().getSharedPreferences("UserData", 0)
        currentLevel = sharedPreferences.getString("level", "Beginner") ?: "Beginner"

        when (currentLevel) {
            "Beginner" -> beginnerButton.isChecked = true
            "Medium" -> mediumButton.isChecked = true
            "Advanced" -> advancedButton.isChecked = true
        }

        showPlan(currentLevel)

        levelGroup.setOnCheckedChangeListener { _, checkedId ->
            currentLevel = when (checkedId) {
                R.id.beginner -> "Beginner"
                R.id.medium -> "Medium"
                R.id.advanced -> "Advanced"
                else -> "Beginner"
            }
            showPlan(currentLevel)
        }

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun showPlan(level: String) {
        tableLayout.removeAllViews()

        val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Добавляем шапку
        val header = layoutInflater.inflate(R.layout.table_header, tableLayout, false)
        tableLayout.addView(header)

        // Разделитель после шапки
        val headerDivider = View(requireContext()).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                4
            )
            setBackgroundColor(0x22000000)
        }
        tableLayout.addView(headerDivider)

        for (i in 0 until 10) {
            val date = dateFormat.format(calendar.time)
            val isRestDay = i % 3 == 2

            val row = layoutInflater.inflate(R.layout.table_row, tableLayout, false) as TableRow

            val levelText = row.getChildAt(0) as TextView
            val dateText = row.getChildAt(1) as TextView
            val setsText = row.getChildAt(2) as TextView
            val restText = row.getChildAt(3) as TextView
            val statusText = row.getChildAt(4) as TextView

            if (isRestDay) {
                levelText.text = "Rest day"
                dateText.text = "------"
                setsText.text = "------"
                restText.text = "------"
                statusText.text = "------"

                levelText.setTextAppearance(R.style.PlanCellRest)
                levelText.setBackgroundResource(R.drawable.cell_rest_bg)
                row.setBackgroundResource(R.drawable.row_bg)
            } else {
                levelText.text = level
                dateText.text = date

                levelText.setBackgroundResource(R.drawable.cell_level_bg)
                row.setBackgroundResource(R.drawable.row_bg)
                statusText.setTextAppearance(R.style.StatusWork)

                // 🔥 Обработка нажатия — открыть форму редактирования
                row.setOnClickListener {
                    showEditPlanDialog(
                        level = level,
                        date = date,
                        sets = setsText.text.toString(),
                        restTime = restText.text.toString()
                    )
                }
            }

            tableLayout.addView(row)

            val divider = View(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    4
                )
                setBackgroundColor(0x22000000)
            }
            tableLayout.addView(divider)

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun showEditPlanDialog(level: String, date: String, sets: String, restTime: String) {
        val dialog = EditPlanDialogFragment().apply {
            arguments = Bundle().apply {
                putString("level", level)
                putString("date", date)
                putString("sets", sets)
                putString("restTime", restTime)
            }
            listener = object : EditPlanDialogFragment.OnPlanEditedListener {
                override fun onPlanEdited(level: String, date: String, sets: List<Int>, restTime: String) {
                    showPlan(currentLevel)
                }
            }
        }

        dialog.show(parentFragmentManager, "EditPlanDialog")
    }
}
