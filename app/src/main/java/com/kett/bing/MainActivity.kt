    package com.kett.bing

    import android.annotation.SuppressLint
    import android.content.Intent
    import android.graphics.Color
    import android.os.Build
    import android.os.Bundle
    import android.view.View
    import android.view.WindowManager
    import androidx.annotation.RequiresApi
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
    import com.kett.bing.ui.MinerFragment
    import com.kett.bing.ui.SettingsFragment
    import com.kett.bing.ui.theme.KettBingTheme
    import java.util.Locale

    class MainActivity : AppCompatActivity() {
        @RequiresApi(Build.VERSION_CODES.R)
        @SuppressLint("CutPasteId")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            MusicPlayerManager.start(this)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.TRANSPARENT

            setContentView(R.layout.activity_main)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

            // Принудительно установить английскую локаль
            val locale = Locale("en")
            Locale.setDefault(locale)

            handleIntent(intent)

            if (savedInstanceState == null && !intent.getBooleanExtra("open_settings", false)) {
                openLoadingFragment()
            }

            hideSystemUI()
        }

        override fun onNewIntent(intent: Intent) {
            super.onNewIntent(intent)
            intent?.let {
                handleIntent(it)
            }
        }

        private fun handleIntent(intent: Intent) {
            when {
                intent.getBooleanExtra("open_settings", false) -> {
                    openSettingsFragment()
                }
                intent.getBooleanExtra("openMiner", false) -> {
                    openMinerFragment()
                }
                else -> {
                    openMainFragment()
                }
            }
        }

        fun openSettingsFragment() {
            openFragment(SettingsFragment())
        }

        fun openMinerFragment() {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragmentContainer, MinerFragment())
                .addToBackStack(null)
                .commit()
        }

        // Метод для замены фрагмента
        fun openFragment(fragment: Fragment) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        @SuppressLint("InlinedApi", "WrongConstant")
        private fun hideSystemUI() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller?.hide(
                    android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
                )
                controller?.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        )
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                    )
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    )
                }
            }
        }

        // Метод для отображения панели навигации
        private fun openLoadingFragment() {
            if (supportFragmentManager.findFragmentByTag("loading") == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.mainFragmentContainer, LoadingFragment(), "loading")
                    .commit()
            }
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