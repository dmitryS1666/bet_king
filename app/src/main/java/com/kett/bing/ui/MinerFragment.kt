package com.kett.bing.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kett.bing.MainActivity
import com.kett.bing.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MinerFragment : Fragment() {

    private lateinit var buttons: List<ImageButton>
    private lateinit var currentScoreText: TextView
    private lateinit var currentScoreLabel: TextView
    private lateinit var timerText: TextView
    private lateinit var resetButton: ImageButton

    private var score = 0
    private var winIndices = setOf<Int>()
    private var correctGuesses = 0
    private var gameOver = false
    private var timer: CountDownTimer? = null
    private val gameDurationMillis = 15_000L
    private var cardFlipped = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_miner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Системные флаги (fullscreen и т.п.) обычно ставятся в Activity, но если хочешь — можешь вызвать через requireActivity()
        requireActivity().window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        requireActivity().window.navigationBarColor = Color.TRANSPARENT
        requireActivity().window.statusBarColor = Color.TRANSPARENT

        buttons = listOf(
            view.findViewById(R.id.cell0),
            view.findViewById(R.id.cell1),
            view.findViewById(R.id.cell2),
            view.findViewById(R.id.cell3),
        )

        currentScoreText = view.findViewById(R.id.currentScoreText)
        currentScoreLabel = view.findViewById(R.id.currentScoreLabel)
        timerText = view.findViewById(R.id.timerText)
        resetButton = view.findViewById<ImageButton>(R.id.finishButton)

        resetButton.setOnClickListener { finishGame() }

        startGame()

        val settingsButton: ImageButton = view.findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            if (gameOver) return@setOnClickListener

            if (!cardFlipped) {
                cardFlipped = true
                startTimer()
            }

            settingsButton.isEnabled = false

            // Открываем Settings в MainActivity
            (activity as? MainActivity)?.openSettingsFragment()
        }

        val homeButton: ImageView = view.findViewById(R.id.homeButton)
        homeButton.setOnClickListener {
            if (gameOver) return@setOnClickListener

            if (!cardFlipped) {
                cardFlipped = true
                startTimer()
            }

            homeButton.isEnabled = false

            // Открываем главный фрагмент в MainActivity
            (activity as? MainActivity)?.openMainFragment()
        }
    }

    private fun startGame() {
        winIndices = generateWinningCells()
        gameOver = false
        correctGuesses = 0
        score = 0
        cardFlipped = false

        currentScoreLabel.visibility = View.GONE
        currentScoreText.visibility = View.GONE
        timerText.text = "TIME:\n0:15"

        buttons.forEach {
            it.setImageDrawable(null)
            it.setBackgroundResource(R.drawable.cell_background)
        }

        for ((index, btn) in buttons.withIndex()) {
            btn.setOnClickListener {
                if (gameOver) return@setOnClickListener

                if (!cardFlipped) {
                    cardFlipped = true
                    startTimer()
                }

                flipCard(btn) {
                    if (winIndices.contains(index)) {
                        btn.setImageResource(R.drawable.ic_win)
                        score = if (score == 0) 200 else score * 2
                        correctGuesses++

                        currentScoreText.text = "$score"
                        currentScoreText.visibility = View.VISIBLE
                        currentScoreLabel.visibility = View.VISIBLE

                        if (correctGuesses == winIndices.size) {
                            gameOver = true
                            timer?.cancel()
                            showWinDialog(score)
                        }
                    } else {
                        btn.setImageResource(R.drawable.ic_lose)
                        gameOver = true
                        timer?.cancel()
                        score = 0
                        currentScoreText.text = "0"
                        currentScoreText.visibility = View.VISIBLE
                        currentScoreLabel.visibility = View.VISIBLE
                        showLoseDialog()
                    }
                }

                btn.setOnClickListener(null)
            }
        }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(gameDurationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                timerText.text = "TIME:\n0:$seconds"
            }

            override fun onFinish() {
                gameOver = true
                timerText.text = "TIME:\n0"

                when {
                    correctGuesses == 0 -> showLoseDialog()
                    correctGuesses == 1 -> showWinDialog(score)
                    else -> {}
                }
            }
        }.start()
    }

    private fun finishGame() {
        if (!gameOver && correctGuesses > 0) {
            gameOver = true
            timer?.cancel()
            showWinDialog(score)
        } else if (!gameOver) {
            timer?.cancel()
            showLoseDialog()
        }
    }

    private fun generateWinningCells(): Set<Int> {
        return (0..3).shuffled().take(2).toSet()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }

    private fun flipCard(view: View, onEnd: () -> Unit) {
        view.animate()
            .rotationY(90f)
            .setDuration(150)
            .withEndAction {
                onEnd()
                view.setBackgroundResource(R.drawable.select_cell_background)
                view.rotationY = -90f
                view.animate()
                    .rotationY(0f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun saveGameResult(win: Boolean, score: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        val prefs = requireActivity().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        if (win) {
            editor.putString("lastWinTime", timestamp)
            val totalScore = prefs.getInt("totalScore", 0) + score
            editor.putInt("totalScore", totalScore)

            // ✅ Добавляем в историю побед
            addGameResultToList("winTimes")
        } else {
            editor.putString("lastLoseTime", timestamp)

            // ✅ Добавляем в историю поражений
            addGameResultToList("loseTimes")
        }

        editor.putInt("lastGameScore", score)
        editor.apply()
    }

    private fun addGameResultToList(key: String) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy\nHH:mm", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        val prefs = requireActivity().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        existing.add(timestamp)

        // Оставляем только 50 последних записей
        val trimmed = existing.sortedDescending().take(50).toMutableSet()

        prefs.edit()
            .putStringSet(key, trimmed)
            .apply()
    }

    @SuppressLint("MissingInflatedId")
    private fun showWinDialog(score: Int) {
        saveGameResult(win = true, score = score)
        val intent = Intent(requireContext(), WinDialogActivity::class.java)
        intent.putExtra("score", score)
        startActivity(intent)
    }

    @SuppressLint("MissingInflatedId")
    private fun showLoseDialog() {
        saveGameResult(win = false, score = 0)
        val intent = Intent(requireContext(), LoseDialogActivity::class.java)
        startActivity(intent)
    }
}
