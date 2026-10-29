package com.matedesk.pro

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Base64
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import android.widget.Toast
import java.nio.charset.StandardCharsets

/**
 * Thin Android WebView host for the local MateDesk HTML app.
 * The only page loaded into this WebView is the app's bundled asset.
 */
class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var pendingText: String? = null
    private var pendingFileName: String = "catatan-matedesk.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 23, 37)
        window.navigationBarColor = Color.rgb(16, 23, 37)

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(16, 23, 37))
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true // Needed to load bundled file:///android_asset/index.html.
                allowContentAccess = true // Lets the browser consume user-selected content URIs.
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                builtInZoomControls = false
                displayZoomControls = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
            }

            // Safe here because this bridge is exposed only to the bundled, local app UI.
            addJavascriptInterface(MateDeskBridge(), "MateDeskAndroid")

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    view: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams
                ): Boolean {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = filePathCallback
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(
                            Intent.EXTRA_ALLOW_MULTIPLE,
                            fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                        )
                    }
                    return try {
                        startActivityForResult(intent, REQUEST_OPEN_FILE)
                        true
                    } catch (_: Exception) {
                        fileChooserCallback?.onReceiveValue(null)
                        fileChooserCallback = null
                        Toast.makeText(this@MainActivity, "Pemilih berkas tidak tersedia.", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    // Keep navigation inside the bundled app asset only.
                    if (uri.scheme == "file" && uri.toString().startsWith("file:///android_asset/")) return false
                    if (uri.scheme == "http" || uri.scheme == "https") {
                        openExternalUrl(uri.toString())
                        return true
                    }
                    // Block other schemes and local file paths from being loaded as pages.
                    return true
                }
            }

            if (savedInstanceState == null) {
                loadUrl("file:///android_asset/index.html")
            } else {
                restoreState(savedInstanceState)
                if (url.isNullOrBlank()) loadUrl("file:///android_asset/index.html")
            }
        }

        setContentView(webView)
        enableImmersiveDesktop()
    }

    @Suppress("DEPRECATION")
    private fun enableImmersiveDesktop() {
        // Edge gestures can temporarily reveal system bars; this is not kiosk mode.
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.post {
                runCatching { webView.evaluateJavascript("window.MateDeskRefreshAndroidApps && window.MateDeskRefreshAndroidApps();", null) }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveDesktop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Android API; retained for broad device compatibility.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OPEN_FILE) {
            val callback = fileChooserCallback ?: return
            fileChooserCallback = null
            if (resultCode != RESULT_OK || data == null) {
                callback.onReceiveValue(null)
                return
            }
            val uris = mutableListOf<Uri>()
            data.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) uris += clip.getItemAt(i).uri
            }
            if (uris.isEmpty()) data.data?.let(uris::add)
            callback.onReceiveValue(uris.takeIf { it.isNotEmpty() }?.toTypedArray())
            return
        }

        if (requestCode == REQUEST_SAVE_TEXT) {
            val content = pendingText
            pendingText = null
            if (resultCode != RESULT_OK || data?.data == null || content == null) return
            try {
                contentResolver.openOutputStream(data.data!!)?.use { out ->
                    out.write(content.toByteArray(StandardCharsets.UTF_8))
                } ?: throw IllegalStateException("Tidak dapat membuka tujuan penyimpanan")
                Toast.makeText(this, "Catatan berhasil disimpan.", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, "Gagal menyimpan berkas.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openExternalUrl(rawUrl: String) {
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return
        if (uri.scheme != "http" && uri.scheme != "https") {
            Toast.makeText(this, "Hanya tautan HTTP/HTTPS yang diizinkan.", Toast.LENGTH_SHORT).show()
            return
        }
        runOnUiThread {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: Exception) {
                Toast.makeText(this, "Tidak ada aplikasi untuk membuka tautan ini.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Lists apps with a launcher activity that are visible to this app under package-visibility rules. */
    private fun queryLaunchableApps(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
        val seen = HashSet<String>()
        return matches
            .filter { it.activityInfo != null && it.activityInfo.packageName != packageName }
            .filter { seen.add(it.activityInfo.packageName) }
            .sortedBy { runCatching { it.loadLabel(packageManager).toString() }.getOrDefault(it.activityInfo.packageName).lowercase() }
    }

    private fun drawableToDataUri(drawable: Drawable): String {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val oldBounds = Rect(drawable.bounds)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        drawable.bounds = oldBounds
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return "data:image/png;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun windowingCapabilitiesJson(): String {
        val freeformAdvertised = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)
        val pictureInPicture = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        return JSONObject().apply {
            put("sdkInt", Build.VERSION.SDK_INT)
            put("release", Build.VERSION.RELEASE ?: "")
            put("manufacturer", Build.MANUFACTURER ?: "")
            put("model", Build.MODEL ?: "")
            put("freeformAdvertised", freeformAdvertised)
            put("pictureInPictureAdvertised", pictureInPicture)
        }.toString()
    }

    private fun installedAppsJson(): String {
        val result = JSONArray()
        queryLaunchableApps().forEach { info ->
            val activityInfo = info.activityInfo ?: return@forEach
            val appPackage = activityInfo.packageName ?: return@forEach
            val item = JSONObject()
            item.put("packageName", appPackage)
            item.put("label", runCatching { info.loadLabel(packageManager).toString().trim() }
                .getOrDefault(appPackage).ifBlank { appPackage })
            item.put("iconDataUri", runCatching { drawableToDataUri(info.loadIcon(packageManager)) }.getOrDefault(""))
            result.put(item)
        }
        return result.toString()
    }

    private fun launchAndroidAppInternal(appPackage: String, preferWindow: Boolean) {
        val launchIntent = packageManager.getLaunchIntentForPackage(appPackage)
        if (launchIntent == null) {
            dispatchNativeLaunchResult(appPackage, false, "Aplikasi tidak memiliki aktivitas peluncur atau tidak lagi tersedia.")
            return
        }
        try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (preferWindow && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                startActivity(launchIntent)
                dispatchNativeLaunchResult(
                    appPackage,
                    true,
                    "Mode jendela eksperimental memerlukan Android 7.0 atau lebih baru. Aplikasi diminta dibuka secara normal."
                )
            } else if (preferWindow) {
                val metrics = resources.displayMetrics
                val width = metrics.widthPixels.coerceAtLeast(1)
                val height = metrics.heightPixels.coerceAtLeast(1)
                val bounds = Rect(
                    (width * 0.08f).toInt(),
                    (height * 0.08f).toInt(),
                    (width * 0.84f).toInt(),
                    (height * 0.84f).toInt()
                )
                val options = ActivityOptions.makeBasic().setLaunchBounds(bounds)
                startActivity(launchIntent, options.toBundle())
                dispatchNativeLaunchResult(
                    appPackage,
                    true,
                    "Permintaan peluncuran dikirim dengan petunjuk ukuran jendela. Sistem dapat mengabaikan petunjuk ini."
                )
            } else {
                startActivity(launchIntent)
                dispatchNativeLaunchResult(appPackage, true, "Permintaan peluncuran aplikasi dikirim ke sistem Android.")
            }
        } catch (_: Exception) {
            // If the experimental bounds hint is unsupported, try an ordinary launch.
            if (preferWindow) {
                try {
                    startActivity(launchIntent)
                    dispatchNativeLaunchResult(
                        appPackage,
                        true,
                        "Aplikasi diminta dibuka dalam mode normal karena mode jendela tidak dapat diminta."
                    )
                    return
                } catch (_: Exception) {
                    // Fall through to a clear failure result.
                }
            }
            dispatchNativeLaunchResult(appPackage, false, "Android tidak dapat membuka aplikasi ini. Periksa apakah aplikasi masih terpasang dan dapat diluncurkan.")
        }
    }

    private fun dispatchNativeLaunchResult(appPackage: String, success: Boolean, message: String) {
        runOnUiThread {
            if (!::webView.isInitialized) return@runOnUiThread
            val js = "window.MateDeskNativeLaunchResult && window.MateDeskNativeLaunchResult(" +
                JSONObject.quote(appPackage) + "," + success + "," + JSONObject.quote(message) + ");"
            webView.evaluateJavascript(js, null)
            if (!success) Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private inner class MateDeskBridge {
        @JavascriptInterface
        fun getInstalledApps(): String {
            return runCatching { installedAppsJson() }.getOrDefault("[]")
        }

        @JavascriptInterface
        fun getWindowingCapabilities(): String {
            return runCatching { windowingCapabilitiesJson() }.getOrDefault("{}")
        }

        @JavascriptInterface
        fun launchAndroidApp(packageName: String, preferWindow: Boolean): Boolean {
            // Only accept package names and launchable packages visible to this app.
            if (!PACKAGE_NAME.matches(packageName) || packageName == this@MainActivity.packageName) return false
            val isKnownLaunchable = queryLaunchableApps().any { it.activityInfo?.packageName == packageName }
            if (!isKnownLaunchable) return false
            runOnUiThread { launchAndroidAppInternal(packageName, preferWindow) }
            return true // The request is queued; final result is reported back to JavaScript.
        }

        @JavascriptInterface
        fun openExternalUrl(url: String) {
            this@MainActivity.openExternalUrl(url)
        }

        @JavascriptInterface
        fun saveTextFile(fileName: String, content: String, mimeType: String?) {
            runOnUiThread {
                pendingText = content
                pendingFileName = fileName.substringAfterLast('/').substringAfterLast('\\').take(100).ifBlank { "catatan-matedesk.txt" }
                val safeMime = mimeType?.substringBefore(';')?.takeIf { it.startsWith("text/") } ?: "text/plain"
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = safeMime
                    putExtra(Intent.EXTRA_TITLE, pendingFileName)
                }
                try {
                    startActivityForResult(intent, REQUEST_SAVE_TEXT)
                } catch (_: Exception) {
                    pendingText = null
                    Toast.makeText(this@MainActivity, "Dialog penyimpanan tidak tersedia.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = null
        webView.stopLoading()
        webView.removeJavascriptInterface("MateDeskAndroid")
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private val PACKAGE_NAME = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*$")
        private const val REQUEST_OPEN_FILE = 4201
        private const val REQUEST_SAVE_TEXT = 4202
    }
}
