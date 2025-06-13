package com.kett.bing

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.kett.bing.ui.LoadingFragment
import com.kett.bing.ui.MainFragment
import com.kett.bing.ui.MinerActivity
import com.kett.bing.ui.theme.KettBingTheme
import java.util.Locale

class MainActivity : AppCompatActivity() {
    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MusicPlayerManager.start(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_main)

        // Принудительно установить английскую локаль
        val locale = Locale("en")
        Locale.setDefault(locale)

        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            openLoadingFragment()
        }

        hideSystemUI()
    }

    // Метод для замены фрагмента
    fun openFragment(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
    }

    // Метод для отображения панели навигации
    private fun openLoadingFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, LoadingFragment())
            .commit()
    }

    fun openMainFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, MainFragment())
            .commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        // Подождём, пока фрагмент реально заменится
        supportFragmentManager.executePendingTransactions()
        supportFragmentManager.findFragmentById(R.id.mainFragmentContainer)
    }

    override fun onStop() {
        super.onStop()
        MusicPlayerManager.stop()
    }

    override fun onStart() {
        super.onStart()
        MusicPlayerManager.start(this)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI() // Снова скрываем системные кнопки при возвращении
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KettBingTheme {
        Greeting("Android")
    }
}