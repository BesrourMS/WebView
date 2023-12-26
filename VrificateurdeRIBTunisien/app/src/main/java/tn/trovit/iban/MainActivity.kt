package tn.trovit.iban

import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.journeyapps.barcodescanner.CaptureActivity

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    var wv: WebView? = null

    private lateinit var progressBar: ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_main)


        // Web view Implementation
        wv = findViewById(R.id.webView)
        wv?.addJavascriptInterface(WebAppInterface(this), "Android")

        setWebContentsDebuggingEnabled(true)


        progressBar = findViewById(R.id.progressBar)
        // Choose your desired color (e.g., red in this case)
        val color = ContextCompat.getColor(this, R.color.colorPrimary)
        wv?.clearCache(true)
        wv?.clearHistory()
        wv?.settings?.javaScriptEnabled = true
        wv?.settings?.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        wv?.settings?.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        wv?.settings?.domStorageEnabled = true
        wv?.settings?.allowFileAccess = true
        wv?.settings?.javaScriptCanOpenWindowsAutomatically = true

        wv?.webChromeClient = WebChromeClient()

        wv?.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return if ((url.startsWith("http://") || url.startsWith("https://") || url.startsWith("tel:"))) {
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    true
                } else {
                    false
                }

            }

        }

        wv?.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                // Update the progress bar during page loading
                progressBar.indeterminateDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                //progressBar.progressDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                progressBar.progress = progress
                if (progress == 100) {
                    // Hide the progress bar when the page is fully loaded
                    progressBar.visibility = ProgressBar.GONE
                    // Make the WebView visible
                    //wv?.setVisibility(WebView.VISIBLE)
                    wv?.visibility = View.VISIBLE
                }
            }

            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d("MyApplication", "${message.message()} -- From line " +
                        "${message.lineNumber()} of ${message.sourceId()}")
                return true
            }


        }
        if (savedInstanceState == null) {
            wv?.loadUrl("file:///android_asset/web/index.html")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            // QR code scanned successfully
            val qrCodeData = result.contents
            Toast.makeText(this, qrCodeData, Toast.LENGTH_SHORT).show()
            this.WebAppInterface(this).handleQRCodeResult(qrCodeData)
        } else {
            // QR code scan failed
            Toast.makeText(this, "QR code scan failed", Toast.LENGTH_SHORT).show()
        }
    }

    inner class WebAppInterface(private val activity: AppCompatActivity) {

        // Show Android share dialog
        @JavascriptInterface
        fun shareContent(shareText: String) {
            // Implement share functionality
            // You can use Android's Intent to share content
            // This example opens the share dialog with plain text
            // You can customize it based on your needs
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            activity.startActivity(Intent.createChooser(sharingIntent, "Share via"))
        }

        @JavascriptInterface
        fun startQRScanner() {
            // Start QR code scanner using ZXing
            IntentIntegrator(activity).initiateScan()
        }

        @JavascriptInterface
        fun handleQRCodeResult(qrCodeData: String) {
            // Pass the QR code result to JavaScript
            val jsCode = "handleQRCodeResult('$qrCodeData')"
            wv?.post {
                wv?.evaluateJavascript(jsCode, null)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK && wv?.canGoBack() == true) {
            wv?.goBack()
            return true
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        wv?.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        wv?.restoreState(savedInstanceState)
    }

}

