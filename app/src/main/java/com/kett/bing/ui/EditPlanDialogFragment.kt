package com.kett.bing.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.kett.bing.R
import java.util.Calendar
import java.util.Locale

class EditPlanDialogFragment : DialogFragment() {

    interface OnPlanEditedListener {
        fun onPlanEdited(level: String, date: String, sets: List<Int>, restTime: String)
    }

    var listener: OnPlanEditedListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_edit_plan, null)

        val dateEditText = view.findViewById<EditText>(R.id.dateEditText)

        // Инициализируем дату из EditText или текущей даты
        val calendar = Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("dd.MM.yy", Locale.getDefault())

        // Попытка установить дату из уже заполненного текста
        val dateText = dateEditText.text.toString()
        if (dateText.isNotEmpty()) {
            try {
                val date = dateFormat.parse(dateText)
                if (date != null) calendar.time = date
            } catch (_: Exception) { }
        }

        dateEditText.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val levelSpinner = view.findViewById<Spinner>(R.id.levelSpinner)
        val restEditText = view.findViewById<EditText>(R.id.restEditText)
        val addButton = view.findViewById<Button>(R.id.addButton)

        val set1 = view.findViewById<EditText>(R.id.set1)
        val set2 = view.findViewById<EditText>(R.id.set2)
        val set3 = view.findViewById<EditText>(R.id.set3)
        val set4 = view.findViewById<EditText>(R.id.set4)
        val set5 = view.findViewById<EditText>(R.id.set5)

        levelSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Beginner", "Medium", "Advanced")
        )

        // Получаем аргументы
        val args = arguments
        val level = args?.getString("level") ?: "Beginner"
        val date = args?.getString("date") ?: ""
        val setsString = args?.getString("sets") ?: ""
        val restTime = args?.getString("restTime") ?: ""

        // Устанавливаем данные в поля
        dateEditText.setText(date)
        restEditText.setText(restTime)

        // Устанавливаем выбранный уровень в Spinner
        val levelIndex = listOf("Beginner", "Medium", "Advanced").indexOf(level)
        if (levelIndex >= 0) {
            levelSpinner.setSelection(levelIndex)
        }

        // Парсим setsString (например: "4, 3, 3, 2, 2") в List<Int>
        val setsList = setsString.split(",").mapNotNull { it.trim().toIntOrNull() }

        // Заполняем EditText для сетов
        val setsEditTexts = listOf(set1, set2, set3, set4, set5)
        for (i in setsEditTexts.indices) {
            if (i < setsList.size) {
                setsEditTexts[i].setText(setsList[i].toString())
            } else {
                setsEditTexts[i].setText("")
            }
        }

        val closeButton: ImageButton = view.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            dismiss() // закрыть диалог
        }

        addButton.setOnClickListener {
            val level = levelSpinner.selectedItem.toString()
            val date = dateEditText.text.toString()
            val sets = setsEditTexts
                .mapNotNull { it.text.toString().trim().toIntOrNull() }
            val restTime = restEditText.text.toString()

            if (sets.isNotEmpty()) {
                listener?.onPlanEdited(level, date, sets, restTime)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Enter valid sets", Toast.LENGTH_SHORT).show()
            }
        }

        restEditText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                isUpdating = true

                val digits = s.toString().replace(":", "").take(4) // max MMSS

                val formatted = when {
                    digits.length <= 2 -> digits
                    else -> digits.substring(0, 2) + ":" + digits.substring(2)
                }

                restEditText.setText(formatted)
                restEditText.setSelection(formatted.length)

                isUpdating = false
            }
        })

        builder.setView(view)
        return builder.create()
    }
}
