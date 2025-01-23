package ee.taltech.aireapplication.games

import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.R
import ee.taltech.aireapplication.dto.WebLink
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BaseActivity
import ee.taltech.aireapplication.helpers.C
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

class WebLinkActivity : BaseActivity(), SeekBar.OnSeekBarChangeListener {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var buttonClose: Button
    private lateinit var webView: WebView
    private lateinit var seekBarZoomBy: SeekBar
    private lateinit var seekBarTextZoom: SeekBar
    private lateinit var textViewZoomBy: TextView
    private lateinit var textViewTextZoom: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_link)

        buttonClose = findViewById(R.id.buttonCloseWebLink)
        webView = findViewById(R.id.webLinkWebView)
        seekBarZoomBy = findViewById(R.id.seekBarZoomBy)
        seekBarTextZoom = findViewById(R.id.seekBarTextZoom)
        textViewZoomBy = findViewById(R.id.textViewZoomBy)
        textViewTextZoom = findViewById(R.id.textViewTextZoom)

        seekBarZoomBy.setOnSeekBarChangeListener(this)
        seekBarTextZoom.setOnSeekBarChangeListener(this)

        initializeWebView()
    }

    fun buttonCloseWebLinkOnClick(view: View) {
        closeActivity()
    }

    private fun closeActivity() {
        app.robot.cancelAllTtsRequests()

        buttonClose.text = getString(R.string.Close)

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.button", message = "closeButtonOnClick"
            )
        }

        finish()
    }


    private fun initializeWebView() {
        val webLinkStr = intent.getStringExtra(C.EXTRA_GAMEVIEW_SETTINGS)
        val webLink = Json.decodeFromString<WebLink>(webLinkStr!!)

        val url =
            C.URL_IFRAME_VIEW +
                    webLink.id +
                    "?culture=" + Locale.ENGLISH //getDefault().toLanguageTag()



        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true


        // https://developer.android.com/reference/android/webkit/WebSettings.html#setLoadWithOverviewMode(boolean)
        webView.settings.loadWithOverviewMode = webLink.loadWithOverviewMode

        // https://developer.android.com/reference/android/webkit/WebSettings.html#setUseWideViewPort%28boolean%29
        webView.settings.useWideViewPort = webLink.useWideViewPort

        // https://developer.android.com/reference/android/webkit/WebSettings.LayoutAlgorithm
        // https://chromium.googlesource.com/chromium/src/+/HEAD/android_webview/docs/web-page-layout.md
        // TEXT_AUTOSIZING - disables text zoom
        webView.settings.layoutAlgorithm =  WebSettings.LayoutAlgorithm.valueOf(webLink.layoutAlgorithm)


        // https://developer.android.com/reference/android/webkit/WebSettings#setSupportZoom(boolean)
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = webLink.builtInZoomControls
        webView.settings.displayZoomControls = webLink.displayZoomControls


        // set the link handler
        webView.webViewClient = MyWebViewClient(this)


        if (webLink.textZoom != null) {
            webView.settings.textZoom = webLink.textZoom
        }

        if (webLink.zoomFactor != null) {
            webView.setInitialScale(webLink.zoomFactor.toInt())
        }

        textViewTextZoom.text = webView.settings.textZoom.toString()
        seekBarTextZoom.progress = webView.settings.textZoom


        textViewZoomBy.text = webView.z.toString()
        seekBarZoomBy.progress = webView.z.toInt()

        webView.loadUrl(url)
    }


    private class MyWebViewClient(val activity: WebLinkActivity) : WebViewClient() {
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            //EventLogger.LogEvent(activity, TAG, "webclient link $url")

            if (url != null && url.contains("/temi/close")) {
                Log.d(TAG, "Closing, url: $url")
                activity.finish()
            }
            // do not allow to open  browser app, stay in web view
            return false
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler,
            error: SslError?
        ) {
            handler.proceed() // Ignore SSL certificate errors
        }


    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (seekBar == seekBarZoomBy) {
            //webView.zoomBy(progress / 1f)
            webView.setInitialScale(progress)

            textViewZoomBy.text = progress.toString()
        } else
            if (seekBar == seekBarTextZoom) {
                webView.settings.textZoom = progress
                textViewTextZoom.text = progress.toString()
            }
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }

}