
package com.simpleos.launcher

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.TextView
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var desktopWeb: WebView
    private var browserWeb: WebView? = null
    private var browserPanel: LinearLayout? = null
    private var addressBar: EditText? = null

    private val supportedLanguages = setOf("ru", "uk", "en")

    private val prefs by lazy {
        getSharedPreferences("simpleos_data", MODE_PRIVATE)
    }

    private fun getSystemLanguage(): String {
        val appName = applicationInfo.loadLabel(packageManager)
            .toString().uppercase(Locale.ROOT)

        if (appName.endsWith(" RU")) return "ru"
        if (appName.endsWith(" UA")) return "uk"
        if (appName.endsWith(" EN")) return "en"

        val locale = if (Build.VERSION.SDK_INT >= 24) {
            resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }

        return when (locale.language.lowercase(Locale.ROOT)) {
            "ru" -> "ru"
            "uk", "ua" -> "uk"
            else -> "en"
        }
    }

    inner class NativeBridge {

        @JavascriptInterface
        fun getLanguage(): String = getSystemLanguage()

        @JavascriptInterface
        fun getTranslations(code: String): String {
            val language = if (code in supportedLanguages) code else "en"

            return try {
                assets.open("languages/$language.json")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            } catch (e: Exception) {
                Log.e("S1MPLE", "Translation loading failed", e)
                "{}"
            }
        }

        @JavascriptInterface
        fun getMemoryInfo(): String {
            return try {
                val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val memory = ActivityManager.MemoryInfo()
                manager.getMemoryInfo(memory)

                val stat = StatFs(filesDir.absolutePath)
                val storageTotal = stat.totalBytes
                val storageFree = stat.availableBytes

                JSONObject().apply {
                    put("totalRam", memory.totalMem)
                    put("availableRam", memory.availMem)
                    put("lowMemory", memory.lowMemory)
                    put("storageTotal", storageTotal)
                    put("storageFree", storageFree)
                    put("storageUsed", storageTotal - storageFree)
                }.toString()
            } catch (e: Exception) {
                Log.e("S1MPLE", "Memory info failed", e)
                "{}"
            }
        }

        @JavascriptInterface
        fun loadVirtualState(): String {
            return prefs.getString("virtual_state", "") ?: ""
        }

        @JavascriptInterface
        fun saveVirtualState(json: String) {
            try {
                JSONObject(json)
                prefs.edit().putString("virtual_state", json).apply()
            } catch (e: Exception) {
                Log.e("S1MPLE", "Virtual state save failed", e)
            }
        }

        @JavascriptInterface
        fun openBrowser(url: String) {
            runOnUiThread {
                showBrowser(normalizeUrl(url))
            }
        }

        @JavascriptInterface
        fun closeBrowser() {
            runOnUiThread {
                hideBrowser()
            }
        }

        @JavascriptInterface
        fun openExternal(url: String) {
            runOnUiThread {
                openExternalUrl(normalizeUrl(url))
            }
        }
    }

    private fun normalizeUrl(value: String): String {
        val input = value.trim()
        if (input.isEmpty()) return "https://www.google.com"

        return if (input.startsWith("http://") ||
            input.startsWith("https://")) {
            input
        } else if (input.contains(".") && !input.contains(" ")) {
            "https://$input"
        } else {
            "https://www.google.com/search?q=" +
                Uri.encode(input)
        }
    }

    private fun openExternalUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("S1MPLE", "Cannot open external browser", e)
        }
    }

    private fun makeButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.rgb(37, 49, 70))
                cornerRadius = 22f
            }
            setPadding(12, 4, 12, 4)
            setOnClickListener { onClick() }
        }
    }

    private fun showBrowser(url: String) {
        if (browserPanel != null) {
            browserPanel?.visibility = View.VISIBLE
            browserWeb?.loadUrl(url)
            addressBar?.setText(url)
            return
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(9, 13, 24))
            elevation = 20f
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(6, 8, 6, 8)
            setBackgroundColor(Color.rgb(17, 25, 43))
        }

        val back = makeButton("←") {
            if (browserWeb?.canGoBack() == true) browserWeb?.goBack()
        }

        val forward = makeButton("→") {
            if (browserWeb?.canGoForward() == true) browserWeb?.goForward()
        }

        val refresh = makeButton("↻") {
            browserWeb?.reload()
        }

        val close = makeButton("✕") {
            hideBrowser()
        }

        val external = makeButton("↗") {
            val current = browserWeb?.url ?: addressBar?.text?.toString()
            if (!current.isNullOrBlank()) openExternalUrl(current)
        }

        val address = EditText(this).apply {
            setSingleLine(true)
            textSize = 13f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            hint = "Search or enter address"
            setPadding(10, 4, 10, 4)
            setBackgroundColor(Color.rgb(32, 43, 62))
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, _, _ ->
                val target = normalizeUrl(text.toString())
                browserWeb?.loadUrl(target)
                true
            }
        }

        addressBar = address

        toolbar.addView(back, LinearLayout.LayoutParams(42, 44))
        toolbar.addView(forward, LinearLayout.LayoutParams(42, 44))
        toolbar.addView(refresh, LinearLayout.LayoutParams(42, 44))
        toolbar.addView(
            address,
            LinearLayout.LayoutParams(0, 44, 1f).apply {
                setMargins(4, 0, 4, 0)
            }
        )
        toolbar.addView(external, LinearLayout.LayoutParams(42, 44))
        toolbar.addView(close, LinearLayout.LayoutParams(42, 44))

        val browser = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!url.isNullOrBlank()) address.setText(url)
                }
            }
            webChromeClient = WebChromeClient()
        }

        browserWeb = browser

        panel.addView(
            toolbar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        panel.addView(
            browser,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        root.addView(panel, params)
        browserPanel = panel
        address.setText(url)
        browser.loadUrl(url)
    }

    private fun hideBrowser() {
        browserPanel?.visibility = View.GONE
    }

    @Suppress("DEPRECATION")
    private fun enableFullscreen() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableFullscreen()

        root = FrameLayout(this)

        desktopWeb = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(NativeBridge(), "S1MPLENative")
        }

        root.addView(
            desktopWeb,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)
        desktopWeb.loadUrl("file:///android_asset/index.html")
    }

    override fun onBackPressed() {
        if (browserPanel?.visibility == View.VISIBLE) {
            if (browserWeb?.canGoBack() == true) {
                browserWeb?.goBack()
            } else {
                hideBrowser()
            }
        } else {
            super.onBackPressed()
        }
    }

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableFullscreen()
    }

    override fun onDestroy() {
        browserWeb?.apply {
            stopLoading()
            destroy()
        }

        desktopWeb.stopLoading()
        desktopWeb.removeJavascriptInterface("S1MPLENative")
        desktopWeb.destroy()

        super.onDestroy()
    }
}
