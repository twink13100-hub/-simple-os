
package com.simpleos.launcher

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var web: WebView

    private val supportedLanguages = setOf("ru", "uk", "en")

    private fun getSystemLanguage(): String {

        val appName = applicationInfo
            .loadLabel(packageManager)
            .toString()
            .uppercase(Locale.ROOT)

        return when {
            appName.endsWith(" RU") -> "ru"
            appName.endsWith(" UA") -> "uk"
            appName.endsWith(" EN") -> "en"

            else -> {
                val locale = if (android.os.Build.VERSION.SDK_INT >= 24) {
                    resources.configuration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    resources.configuration.locale
                }

                when (locale.language.lowercase(Locale.ROOT)) {
                    "ru" -> "ru"
                    "uk", "ua" -> "uk"
                    "en" -> "en"
                    else -> "en"
                }
            }
        }
    }

    inner class LanguageBridge {

        @JavascriptInterface
        fun getLanguage(): String {
            return getSystemLanguage()
        }

        @JavascriptInterface
        fun getTranslations(code: String): String {

            val language = if (code in supportedLanguages) {
                code
            } else {
                "en"
            }

            return try {

                assets.open("languages/$language.json")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

            } catch (e: Exception) {

                Log.e("S1MPLE", "Cannot load $language.json", e)

                assets.open("languages/en.json")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }
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

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }

        web.addJavascriptInterface(
            LanguageBridge(),
            "S1MPLENative"
        )

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

    override fun onDestroy() {

        if (::web.isInitialized) {
            web.stopLoading()
            web.loadUrl("about:blank")
            web.removeJavascriptInterface("S1MPLENative")
            web.destroy()
        }

        super.onDestroy()
    }
}
