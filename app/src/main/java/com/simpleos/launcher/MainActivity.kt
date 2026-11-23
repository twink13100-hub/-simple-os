
package com.simpleos.launcher

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    private lateinit var web: WebView

    private fun getSystemLanguage(): String {
        val appName = applicationInfo
            .loadLabel(packageManager)
            .toString()
            .uppercase()

        return when {
            appName.endsWith(" RU") -> "ru"
            appName.endsWith(" UA") -> "ua"
            appName.endsWith(" EN") -> "en"
            else -> "en"
        }
    }

    inner class LanguageBridge {
        @JavascriptInterface
        fun getLanguage(): String {
            return getSystemLanguage()
        }
    }

    @Suppress("DEPRECATION")
    private fun enableFullscreen() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableFullscreen()

        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true

        web.addJavascriptInterface(LanguageBridge(), "S1MPLENative")

        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()

        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            enableFullscreen()
        }
    }
}
