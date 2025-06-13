package com.kett.bing.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kett.bing.MainActivity
import com.kett.bing.R

class MinerActivity : AppCompatActivity() {
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
    private var cardFlipped = false // чтобы начать таймер только после первого клика

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_miner)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT

        buttons = listOf(
            findViewById(R.id.cell0),
            findViewById(R.id.cell1),
            findViewById(R.id.cell2),
            findViewById(R.id.cell3),
        )

        currentScoreText = findViewById(R.id.currentScoreText)
        currentScoreLabel = findViewById(R.id.currentScoreLabel)

        timerText = findViewById(R.id.timerText)
        resetButton = findViewById<ImageButton>(R.id.finishButton)

        resetButton.setOnClickListener { finishGame() }

        startGame()

        val settingsButton: ImageButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("open_settings", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        findViewById<ImageView>(R.id.homeButton).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
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
                    else -> {} // уже отработал showWinDialog при 2 победах
                }
            }
        }.start()
    }

    private fun resetGame() {
        timer?.cancel()
        startGame()
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

    override fun onDestroy() {
        super.onDestroy()
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

    @SuppressLint("MissingInflatedId")
    private fun showWinDialog(score: Int) {
        val intent = Intent(this, WinDialogActivity::class.java)
        intent.putExtra("score", score)
        startActivity(intent)
    }

    @SuppressLint("MissingInflatedId")
    private fun showLoseDialog() {
        val intent = Intent(this, LoseDialogActivity::class.java)
        startActivity(intent)
    }
}
