package ee.taltech.aireapplication.webview

import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import com.robotemi.sdk.SttLanguage
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.R
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BaseActivity
import ee.taltech.aireapplication.helpers.C
import ee.taltech.aireapplication.helpers.CustomAsrListener
import kotlinx.coroutines.launch
import java.util.Locale


class WebViewActivity : BaseActivity(), CustomAsrListener {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var webView: WebView

    //private var backTimer: AtomicInteger = AtomicInteger(30)

    private lateinit var buttonCloseWebView: Button

    //private val localReceiverIntentFilter = IntentFilter()
    //private val localReceiver = BroadcastReceiverInWebViewActivity()
    private var showInput = false

    private lateinit var myUrl: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        buttonCloseWebView = findViewById(R.id.buttonCloseArticle)
        webView = findViewById(R.id.articleVebview)

        buttonCloseWebView.text = getString(R.string.Return)// + " " + backTimer.get()

        //localReceiverIntentFilter.addAction(C.INTENT_TIMER_SERVICE_TICK)

        myUrl = intent
            .getStringExtra(C.EXTRA_WEBVIEW_LAUNCH_URL)!!

        initializeWebView()
    }


    private fun initializeWebView() {
        val url = intent
            .getStringExtra(C.EXTRA_WEBVIEW_LAUNCH_URL)!! +
                "?culture=" + Locale.ENGLISH //getDefault().toLanguageTag()


        showInput = intent
            .getBooleanExtra(C.EXTRA_WEBVIEW_SHOW_INPUT, false)

        webView.settings.javaScriptEnabled = true
        // set the link handler
        webView.webViewClient = MyWebViewClient(this)

        //EventLogger.LogEvent(this, TAG, "webpage $url")

        webView.loadUrl(url)
    }


    private class MyWebViewClient(val activity: WebViewActivity) : WebViewClient() {
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


    fun buttonCloseWebViewOnClick(view: View) {
        closeActivity()
    }

    private fun closeActivity() {
        app.robot.cancelAllTtsRequests()

        buttonCloseWebView.text = getString(R.string.Close)

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.button", message = "closeButtonOnClick"
            )
        }

        finish()
    }

    override fun onResume() {
        super.onResume()
        if (showInput) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        }

        app.addCustomAsrListener(this)
    }

    override fun onPause() {
        super.onPause()

        app.removeCustomAsrListener(this)
    }

    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {
        super.onAsrResult(asrResult, sttLanguage)
        val triggers = HashMap<String, List<String>>()

        triggers["start"] = listOf("alusta", "start", "analüüs")
        triggers["close"] = listOf("sulge", "tagasi")


        var trigger = ""

        triggers.forEach { (key, value) ->
            if (trigger == "") {
                value.forEach { word ->
                    if (trigger == "" && asrResult.contains(word, ignoreCase = true)) {
                        trigger = key
                    }
                }
            }
        }


        when (trigger) {


            "close" -> {


                closeActivity()
            }
        }
    }


    override fun onCustomAsrResult(asrResult: String) {
        onAsrResult(asrResult, SttLanguage.ET_EE)
    }

}