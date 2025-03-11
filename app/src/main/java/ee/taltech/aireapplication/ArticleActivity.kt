package ee.taltech.aireapplication

import android.content.pm.PackageManager
import android.icu.text.CaseMap.Title
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.face.ContactModel
import com.robotemi.sdk.voice.WakeupOrigin
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.NewsActivity.Companion
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BaseActivity
import ee.taltech.aireapplication.helpers.CustomAsrListener
import kotlinx.coroutines.launch
import java.util.Date

class ArticleActivity : BaseActivity(), CustomAsrListener, SeekBar.OnSeekBarChangeListener {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
        const val MAX_ZOOM = 500
        const val MIN_ZOOM = 100
        const val ZOOM_STEP = 50
    }

    private lateinit var buttonCloseArticle: Button
    private lateinit var articleWebView: WebView
    private lateinit var textViewArticleTitle: TextView
    private lateinit var seekBarZoom: SeekBar

    private lateinit var plainText: String
    private lateinit var displayTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        buttonCloseArticle = findViewById(R.id.buttonCloseArticle)
        articleWebView = findViewById(R.id.articleWebView)
        textViewArticleTitle = findViewById(R.id.textViewArticleTitle)
        seekBarZoom = findViewById(R.id.seekBarZoom)

        seekBarZoom.min = MIN_ZOOM
        seekBarZoom.max = MAX_ZOOM

        seekBarZoom.setOnSeekBarChangeListener(this)
    }

    override fun onResume() {
        super.onResume()

        //app.faceDetectionDisabled = true

        applicationScope.launch {
            var articleTitle = intent.getStringExtra("article")
            val article = BackendApiKtorSingleton.getArticle(articleTitle!!, Date())
            if (article == null) {
                app.showLongToast(app, getString(R.string.article_not_found))
                closeActivity()
            } else {
                plainText = article.plainText
                displayTitle = article.displayTitle

                textViewArticleTitle.text = article.displayTitle
                initializeWebView(article.displayText)

                app.speak(
                    sentence = article.displayTitle + "\n\n" + article.plainText,
                    shown = false
                )
            }
        }

        app.addCustomAsrListener(this)
    }

    override fun onPause() {
        super.onPause()
        //app.faceDetectionDisabled = false

        app.removeCustomAsrListener(this)
    }

    private fun refreshTemiUi() {
        try {
            val activityInfo = packageManager
                .getActivityInfo(componentName, PackageManager.GET_META_DATA)
            Robot.getInstance().onStart(activityInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    fun buttonCloseArticleOnClick(view: View) {
        closeActivity()
    }

    private fun changeWebViewTextZoom(zoomChange: Int) {
        var zoom = articleWebView.settings.textZoom + zoomChange
        if (zoom < MIN_ZOOM) {
            zoom = MIN_ZOOM
        } else if (zoom > MAX_ZOOM) {
            zoom = MAX_ZOOM
        }
        articleWebView.settings.textZoom = zoom
        seekBarZoom.progress = articleWebView.settings.textZoom
        Log.d(TAG, "Zoom: ${articleWebView.settings.textZoom}")
    }

    fun buttonScalePlusOnClick(view: View) {
        changeWebViewTextZoom(ZOOM_STEP)

    }

    fun buttonScaleMinusOnClick(view: View) {
        changeWebViewTextZoom(-ZOOM_STEP)
    }


    private fun closeActivity() {
        app.robot.cancelAllTtsRequests()

        buttonCloseArticle.text = getString(R.string.Close)

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.button", message = "closeButtonOnClick"
            )
        }
        finish()
    }

    private fun initializeWebView(content: String) {
        articleWebView.settings.javaScriptEnabled = false


        articleWebView.setInitialScale(300)

        seekBarZoom.progress = articleWebView.settings.textZoom

        // set the link handler
        articleWebView.webViewClient = MyWebViewClient(this)

        //EventLogger.LogEvent(this, TAG, "webpage $url")

        articleWebView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
    }


    private class MyWebViewClient(val activity: ArticleActivity) : WebViewClient() {
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


    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {
        super.onAsrResult(asrResult, sttLanguage)

        val triggers = HashMap<String, List<String>>()


        triggers["close"] = listOf("sulge", "tagasi")
        triggers["zoom_in"] = listOf("suurem", "suuremaks", "in")
        triggers["zoom_out"] = listOf("väiksem", "väiksemaks", "out")
        triggers["again"] = listOf("uuesti", "again")

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
                app.robot.cancelAllTtsRequests()

                applicationScope.launch {
                    BackendApiKtorSingleton.logEvent(
                        tag = "${TAG}.onAsrResult",
                        message = "closing activity"
                    )
                }
                finish()
            }

            "zoom_in" -> changeWebViewTextZoom(ZOOM_STEP)
            "zoom_out" -> changeWebViewTextZoom(-ZOOM_STEP)
            "again" ->  app.speak(
                sentence = displayTitle + "\n\n" + plainText,
                shown = false
            )
        }

        Log.d(TAG, "onAsrResult. trigger: '$trigger', result: $asrResult");
        if (trigger == "close") app.robot.finishConversation()
    }

    override fun onCustomAsrResult(asrResult: String) {
        onAsrResult(asrResult, SttLanguage.ET_EE)
    }


    // override base activity to avoid text overlay display
    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        val speech = ttsRequest.speech.substring(
            0,
            if (ttsRequest.speech.length < 20) ttsRequest.speech.length else 20
        )

        Log.d(
            TAG,
            "onTtsStatusChanged. ttsRequest status: ${ttsRequest.status} speech: $speech"
        )

        ttsStatus = ttsRequest.status
    }

    // =========================================================== Seekbar ===========================================================
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (seekBar == seekBarZoom) {
            articleWebView.settings.textZoom = progress
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }


    // =========================================================== FACE =====================================================
    override fun onFaceRecognized(contactModelList: List<ContactModel>) {
        // do nothing
    }
}