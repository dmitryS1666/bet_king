package com.kett.bing.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kett.bing.MainActivity
import com.kett.bing.R

class WinDialogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.win_dialog)

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

        val score = intent.getIntExtra("score", 0)

        val fireworksImage = findViewById<ImageView>(R.id.fireworksImage)
        val kingImage = findViewById<ImageView>(R.id.kingImage)
        val scoreText = findViewById<TextView>(R.id.scoreText)
        val menuButton = findViewById<ImageButton>(R.id.menuButton)
        val bonusGameButton = findViewById<ImageButton>(R.id.bonusGameButton)

        val gameType = intent.getStringExtra("gameType") ?: "match3"
        if (gameType == "match3") {
            bonusGameButton.visibility = View.VISIBLE
        } else {
            bonusGameButton.visibility = View.GONE
        }

        scoreText.text = "TOTAL\nPOINTS: $score"

        // Кнопка назад в меню
        menuButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // Обработка нажатия на кнопку Bonus Game
        bonusGameButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("openMiner", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // Анимация фейерверков
        fireworksImage.visibility = View.VISIBLE
        fireworksImage.alpha = 0f
        fireworksImage.animate()
            .alpha(1f)
            .setDuration(1000)
            .setInterpolator(AccelerateInterpolator())
            .start()

        kingImage.animate().cancel()

        kingImage.visibility = View.VISIBLE

        kingImage.translationX = 200f
        kingImage.translationY = 200f
        kingImage.alpha = 0f

        kingImage.visibility = View.VISIBLE
        kingImage.alpha = 1f
        kingImage.translationX = 0f
        kingImage.translationY = 0f

        // Вибрация
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }
}
