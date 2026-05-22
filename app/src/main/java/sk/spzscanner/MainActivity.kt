package sk.spzscanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    // Callback pre WebChromeClient keď stránka požiada o kameru
    private var pendingPermissionRequest: PermissionRequest? = null

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingPermissionRequest?.grant(pendingPermissionRequest?.resources)
            } else {
                pendingPermissionRequest?.deny()
                Toast.makeText(this, "Kamera zamietnutá", Toast.LENGTH_SHORT).show()
            }
            pendingPermissionRequest = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        setupWebView()
        webView.loadUrl("file:///android_asset/spz-scanner.html")
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false  // dôležité pre getUserMedia
            allowFileAccess = true
            domStorageEnabled = true                  // localStorage pre históriu
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webChromeClient = object : WebChromeClient() {

            // Povolenie kamery pre getUserMedia
            override fun onPermissionRequest(request: PermissionRequest) {
                val camPermission = PermissionRequest.RESOURCE_VIDEO_CAPTURE

                if (request.resources.contains(camPermission)) {
                    when {
                        // Už máme povolenie na úrovni OS
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            request.grant(request.resources)
                        }
                        // Treba požiadať OS
                        else -> {
                            pendingPermissionRequest = request
                            requestCameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    }
                } else {
                    request.deny()
                }
            }

            // Konzolové logy z JS viditeľné v Logcat
            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                android.util.Log.d("SPZ_WebView", "${msg.message()} [${msg.sourceId()}:${msg.lineNumber()}]")
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                android.util.Log.e("SPZ_WebView", "Error: ${error.description} url=${request.url}")
            }
        }
    }

    // Back button – naviguj späť v WebView namiesto zatvoriť appku
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
