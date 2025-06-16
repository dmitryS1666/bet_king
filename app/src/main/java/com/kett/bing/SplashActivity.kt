package com.kett.bing

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.kett.bing.ui.LoadingFragment
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {

    private lateinit var bannerView: ImageView
    private lateinit var progressBar: ProgressBar
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
        setTheme(R.style.Theme_KettBing)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        bannerView = findViewById(R.id.bannerImageView)
        progressBar = findViewById(R.id.progressBar)

        clickBlocker = findViewById(R.id.clickBlocker)
        clickBlocker.visibility = View.VISIBLE // блокируем всё

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.splashRoot, LoadingFragment())
                .commit()
        }

        // теперь запускаем проверку интернета
        checkConnectionAndLoadBanner()
    }

    private fun checkConnectionAndLoadBanner() {
        Thread {
            val isConnected = checkInternetAccess()
            runOnUiThread {
                if (isConnected) {
                    val cached = prefs.getString("banner_json", null)
                    if (cached != null) {
                        showBanner(JSONObject(cached))
                    } else {
                        fetchBanner()
                    }
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

    private fun showBanner(json: JSONObject) {
        val action = json.optString("GoKing", null)
        val imageUrl = json.optString("kingImg", null)

        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .centerCrop()
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        if (!isActive || isFinishing || isDestroyed) return
                        clickBlocker.visibility = View.GONE // теперь можно кликать
                        bannerView.setImageDrawable(resource)
                        bannerView.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE

                        bannerView.setOnClickListener {
                            if (action?.startsWith("https://") == true) {
                                val intent = Intent(this@SplashActivity, BannerWebActivity::class.java)
                                intent.putExtra("url", action)
                                startActivity(intent)
                                finish()
                            } else {
                                finish()
                            }
                        }
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        if (!isActive || isFinishing || isDestroyed) return
                        goToMain()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        } else {
            goToMain()
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
}

