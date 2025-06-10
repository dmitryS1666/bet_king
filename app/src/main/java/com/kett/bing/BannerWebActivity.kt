package com.kett.bing

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService

class BannerWebActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var webViewBundle: Bundle? = null
    private lateinit var noInternetLayout: View
    private lateinit var webContainer: FrameLayout

    @SuppressLint("SetJavaScriptEnabled", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MusicPlayerManager.stop()
        setContentView(R.layout.activity_banner_web)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )

        webView = findViewById(R.id.bannerWebView)
        noInternetLayout = findViewById(R.id.noInternetLayout)
        webContainer = findViewById(R.id.webContainer)

        setupWebView()

        val url = intent.getStringExtra("url") ?: run {
            finish()
            return
        }

        if (isOnline()) {
            webView.loadUrl(url)
            showWebView()
        } else {
            showNoInternet()
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
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                // Реализуй здесь выбор файлов при необходимости
                return false
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                Log.d("BannerWebActivity", "Клик по ссылке: $url")

                // Обработка кастомных диплинков
                when {
                    url.startsWith("dln://") -> {
                        val newUrl = url.replace("dln://", "https://")
                        openExternalBrowser(newUrl)
                        return true
                    }
                    url.startsWith("tg://") ||
                            url.startsWith("viber://") ||
                            url.startsWith("whatsapp://") ||
                            url.startsWith("privat24:") ||
                            url.startsWith("https://diia.app") -> {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(this@BannerWebActivity, "Lost", Toast.LENGTH_SHORT).show()
                        }
                        return true
                    }
                    url.startsWith("http://") || url.startsWith("https://") -> {
                        // Открываем в текущем WebView
                        return false
                    }
                    else -> return false
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    showNoInternet()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        if (!isOnline()) showNoInternet()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
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

    private fun openExternalBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
