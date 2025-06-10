package com.kett.bing.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kett.bing.databinding.FragmentWorkoutBinding

class WorkoutFragment : Fragment() {

    private var currentRound = 0
    private val roundViews = mutableListOf<TextView>()
    private var isTimerRunning = false
    private var countDownTimer: CountDownTimer? = null

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    private var sequence = mutableListOf<Int>() // будет установлено позже
    private var timeLeftInMillis: Long = 0L     // тоже будет задано позже
    private lateinit var level: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        level = prefs.getString("level", "Beginner") ?: "Beginner"

        // Получаем текущую дату в формате "dd.MM"
        val currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM"))

        // Пробуем загрузить индивидуальный план
        val plan = WorkoutPlanConstants.individualPlanMap[currentDate]

        if (plan != null) {
            level = plan.level
            sequence = plan.sets.toMutableList()
            timeLeftInMillis = convertTimeStringToMillis(plan.restTime)
        } else {
            sequence = WorkoutPlanConstants.setsMap[level]?.toMutableList() ?: mutableListOf(4, 3, 3, 2, 2)
            val timeStr = WorkoutPlanConstants.restTimeMap[level] ?: "01:00"
            timeLeftInMillis = convertTimeStringToMillis(timeStr)
        }

        renderSequence()
        updateCounter()

        binding.plusButton.setOnClickListener {
            sequence[currentRound]++
            updateCounter()
        }

        binding.minusButton.setOnClickListener {
            if (sequence[currentRound] > 1) {
                sequence[currentRound]--
                updateCounter()
            }
        }

        binding.readyButton.setOnClickListener {
            if (currentRound <= sequence.lastIndex) {
                startTimer()
                toggleButtonVisibility()
            }
        }

        binding.stopButton.setOnClickListener {
            stopTimer()
            toggleButtonVisibility()
        }

//        val calendarSection: View = view.findViewById(R.id.calendarIcon)
//        calendarSection.setOnClickListener {
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.mainFragmentContainer, CalendarFragment())
//                .addToBackStack(null)
//                .commit()
//        }
    }

    private fun convertTimeStringToMillis(timeStr: String): Long {
        val parts = timeStr.split(":")
        val minutes = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val seconds = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (minutes * 60 + seconds) * 1000L
    }

    private fun toggleButtonVisibility() {
        if (isTimerRunning) {
            binding.readyButton.visibility = View.GONE
            binding.stopButton.visibility = View.VISIBLE

            // Показываем только таймер, скрываем кнопки счётчика
            binding.timerText.visibility = View.VISIBLE
            binding.counterText.visibility = View.GONE
            binding.plusButton.visibility = View.GONE
            binding.minusButton.visibility = View.GONE
        } else {
            binding.readyButton.visibility = View.VISIBLE
            binding.stopButton.visibility = View.GONE

            // Скрываем таймер, показываем счётчик
            binding.timerText.visibility = View.GONE
            binding.counterText.visibility = View.VISIBLE
            binding.plusButton.visibility = View.VISIBLE
            binding.minusButton.visibility = View.VISIBLE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveWorkoutResult(level: String, totalReps: Int) {
        val prefs = requireActivity().getSharedPreferences("WorkoutStats", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val today = java.time.LocalDate.now().toString() // формат: yyyy-MM-dd
        val record = "$level:$totalReps"

        editor.putString(today, record) // сохраняем как: "2025-05-14" -> "Beginner:24"
        editor.apply()
    }

    private fun startTimer() {
        updateTimer() // Обновить отображение перед стартом

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimer()
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onFinish() {
                timeLeftInMillis = 60000
                isTimerRunning = false
                currentRound++

                if (currentRound <= sequence.lastIndex) {
                    updateCounter()
                    toggleButtonVisibility()
                } else {
                    binding.timerText.text = "DONE"
                    binding.timerText.visibility = View.VISIBLE
                    binding.counterText.visibility = View.GONE
                    binding.plusButton.visibility = View.GONE
                    binding.minusButton.visibility = View.GONE
                    binding.readyButton.visibility = View.GONE
                    binding.stopButton.visibility = View.GONE
                }
                saveWorkoutResult(level, sequence.sum())
            }
        }.start()

        isTimerRunning = true
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        timeLeftInMillis = 60000 // Сброс таймера на начало
        updateTimer() // Обновить отображение
        toggleButtonVisibility()
    }

    private fun updateTimer() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val time = String.format("%02d:%02d", minutes, seconds)
        binding.timerText.text = time
    }

    private fun renderSequence() {
        binding.sequenceLayout.removeAllViews()
        roundViews.clear()

        for ((index, count) in sequence.withIndex()) {
            val textView = TextView(requireContext()).apply {
                text = count.toString()
                textSize = 24f
                setPadding(12, 0, 12, 0)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
            binding.sequenceLayout.addView(textView)
            roundViews.add(textView)
        }
    }

    @SuppressLint("ResourceType")
    private fun updateCounter() {
        binding.counterText.text = sequence[currentRound].toString()

        roundViews.forEachIndexed { index, textView ->
            textView.text = sequence[index].toString()
            if (index == currentRound) {
                textView.setTypeface(null, Typeface.BOLD)
                textView.setTextColor(Color.parseColor("#FFD700"))
            } else {
                textView.setTypeface(null, Typeface.NORMAL)
                textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }
    }
}
