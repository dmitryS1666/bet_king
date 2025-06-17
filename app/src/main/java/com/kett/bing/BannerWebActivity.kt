package com.kett.bing

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kett.bing.ui.LoadingFragment

class BannerWebActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var noInternetLayout: View
    private lateinit var webContainer: FrameLayout
    private lateinit var reloadButton: ImageButton
    private val FILE_CHOOSER_REQUEST_CODE = 1000
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalSystemUiVisibility = 0

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("SetJavaScriptEnabled", "WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MusicPlayerManager.stop()
        val showLoader = intent.getBooleanExtra("showLoader", true)

        if (savedInstanceState == null && showLoader) {
            supportFragmentManager.beginTransaction()
                .add(R.id.webContainer, LoadingFragment(), "loading")
                .commit()
        }

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )

        // Убираем отступы под системные окна
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Прячем статус бар и навигацию
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.let { controller ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ — можно использовать WindowInsets.Type
                controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
            } else {
                // Для более старых версий — используем WindowInsetsCompat constants
                controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            }
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_banner_web)

        findViewById<View>(R.id.webContainer).setOnApplyWindowInsetsListener { view, insets ->
            insets
        }

        noInternetLayout = findViewById(R.id.noInternetLayout)
        webContainer = findViewById(R.id.webContainer)
        webView = findViewById(R.id.bannerWebView)
        reloadButton = noInternetLayout.findViewById(R.id.reloadButton)

        setupWebView()

        val url = intent.getStringExtra("url") ?: run {
            finish()
            return
        }

        if (isOnline()) {
            showWebView()
            webView.loadUrl(url)
        } else {
            showNoInternet()
        }

        reloadButton.setOnClickListener {
            if (isOnline()) {
                showWebView()
                webView.reload()
            } else {
                Toast.makeText(this, "No internet", Toast.LENGTH_SHORT).show()
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (::webView.isInitialized && webView.canGoBack()) {
                Log.d("BannerWebActivity", "Going back in history")
                webView.goBack()
            } else {
                Log.d("BannerWebActivity", "No history, finishing activity")
                finish()  // вместо super.onBackPressed()
            }
        }
    }

    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = true
            allowContentAccess = true
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@BannerWebActivity.filePathCallback?.onReceiveValue(null)
                this@BannerWebActivity.filePathCallback = filePathCallback

                val intent = fileChooserParams?.createIntent()
                return try {
                    if (intent != null) {
                        startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
                    }
                    true
                } catch (e: Exception) {
                    this@BannerWebActivity.filePathCallback = null
                    false
                }
            }

            override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback

                val decor = window.decorView as FrameLayout
                originalSystemUiVisibility = decor.systemUiVisibility
                decor.addView(view, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))

                decor.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )

                actionBar?.hide() // если есть
            }

            override fun onHideCustomView() {
                val decor = window.decorView as FrameLayout
                customView?.let {
                    decor.removeView(it)
                }

                customView = null
                decor.systemUiVisibility = originalSystemUiVisibility
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                actionBar?.show() // если есть
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.proceed() // игнорируем ошибки SSL
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    showNoInternet()
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                if (request?.isForMainFrame == true) {
                    showNoInternet()
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val loadingFragment = supportFragmentManager.findFragmentByTag("loading")
                if (loadingFragment != null) {
                    supportFragmentManager.beginTransaction()
                        .remove(loadingFragment)
                        .commitAllowingStateLoss()
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            val results = if (resultCode == RESULT_OK && data != null) {
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            } else null
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }
    }

    private fun showNoInternet() {
        webView.visibility = View.GONE
        noInternetLayout.visibility = View.VISIBLE
    }

    private fun showWebView() {
        webView.visibility = View.VISIBLE
        noInternetLayout.visibility = View.GONE
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService<ConnectivityManager>()
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            Log.d("BannerWebActivity", "Going back in history")
            webView.goBack()
        } else {
            Log.d("BannerWebActivity", "No history, finishing activity")
            super.onBackPressed()
        }
    }
}
