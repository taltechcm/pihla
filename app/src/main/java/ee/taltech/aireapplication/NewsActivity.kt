package ee.taltech.aireapplication

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.face.ContactModel
import com.robotemi.sdk.listeners.OnBeWithMeStatusChangedListener
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.dto.NewsItem
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BaseActivity
import ee.taltech.aireapplication.helpers.CustomAsrListener
import ee.taltech.aireapplication.helpers.CustomGestureHandler
import ee.taltech.aireapplication.helpers.CustomOnGestureListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NewsActivity : BaseActivity(), CustomAsrListener, OnBeWithMeStatusChangedListener,
    CustomGestureHandler {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var newsButtonReturn: Button
    private var newsItems: List<NewsItem> = ArrayList()
    private var newsIndex = -1

    private lateinit var newsTextViewCategory: TextView
    private lateinit var newsTextViewTitle: TextView
    private lateinit var newsTextViewDescription: TextView
    private lateinit var newsButtonRead: Button

    private var ttsIsReadingNewsItem = false
    private var continueReading = true

    private lateinit var gestureDetector: GestureDetector

    private fun refreshTemiUi() {
        try {
            val activityInfo = packageManager
                .getActivityInfo(componentName, PackageManager.GET_META_DATA)
            Robot.getInstance().onStart(activityInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)


        newsButtonReturn = findViewById(R.id.newsButtonReturn)
        newsTextViewCategory = findViewById(R.id.newsTextViewCategory)
        newsTextViewTitle = findViewById(R.id.newsTextViewTitle)
        newsTextViewDescription = findViewById(R.id.newsTextViewDescription)
        newsButtonRead = findViewById(R.id.newsButtonRead)


        newsTextViewCategory.text = getString(R.string.please_wait)
        newsTextViewTitle.text = ""
        newsTextViewDescription.text = ""

        gestureDetector = GestureDetector(this, CustomOnGestureListener(this))
    }


    fun newsButtonReturnOnClick(view: View) {
        closeActivity()
    }


    private fun closeActivity() {
        app.robot.cancelAllTtsRequests()

        continueReading = false

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.button",
                message = "newsButtonReturnOnClick"
            )
        }

        newsButtonReturn.text = getString(R.string.Close)

        finish()
    }

    override fun onResume() {
        super.onResume()
        refreshTemiUi()

        //app.faceDetectionDisabled = true

        applicationScope.launch {
            newsItems = BackendApiKtorSingleton.getNews()
            newsIndex = 0
            updateNewsDisplay()
            readCurrentNews()
        }

        app.addCustomAsrListener(this)

        app.robot.addOnBeWithMeStatusChangedListener(this)


    }

    override fun onPause() {
        super.onPause()
        //app.faceDetectionDisabled = false

        app.removeCustomAsrListener(this)

        app.robot.removeOnBeWithMeStatusChangedListener(this)
    }

    private fun updateNewsDisplay() {
        newsTextViewCategory.text =
            newsItems[newsIndex].category.uppercase() + " (${newsIndex + 1}/${newsItems.size})"
        newsTextViewTitle.text = newsItems[newsIndex].title
        newsTextViewDescription.text = newsItems[newsIndex].description
    }

    fun newsButtonReadOnClick(view: View) {
        Log.d(TAG, "newsButtonReadOnClick $continueReading")
        startStopNewsReading()
    }

    private fun startStopNewsReading() {
        continueReading = !continueReading

        if (continueReading) {
            newsButtonRead.text = getString(R.string.StopNewsRead)
            readCurrentNews()
        } else {
            newsButtonRead.text = getString(R.string.StartNewsRead)

            app.robot.cancelAllTtsRequests()

            applicationScope.launch {
                app.speak(
                    sentence = "Uudiste lugemine peatatud!",
                    shown = false
                )
            }
        }
    }

    private fun readCurrentNews() {
        applicationScope.launch {
            delay(1000L)

            if (!continueReading) return@launch

            var i = 0
            while (wakeupWordDetected && i < 5) {
                Log.d(TAG, "Triggering speech delayed!")
                i++
                delay(1000L)
            }


            Log.d(TAG, "Triggering speech: " + newsItems[newsIndex].title)

            app.speak(
                sentence = newsItems[newsIndex].title + " " + newsItems[newsIndex].description,
                shown = false
            )

            ttsIsReadingNewsItem = true

            applicationScope.launch {
                BackendApiKtorSingleton.logEvent(
                    tag = "${TAG}.button",
                    message = "newsButtonReadOnClick - " + newsItems[newsIndex].title
                )
            }
        }
    }


    private fun ttsStatusChangeProcessor(ttsRequest: TtsRequest) {
        if (
            ttsIsReadingNewsItem &&
            !wakeupWordDetected &&
            continueReading &&
            ttsRequest.speech == newsItems[newsIndex].title + " " + newsItems[newsIndex].description &&
            ttsRequest.status == TtsRequest.Status.COMPLETED
        ) {
            ttsIsReadingNewsItem = false

            newsIndex++
            if (newsIndex == newsItems.size) {
                newsIndex = 0
            }
            updateNewsDisplay()

            if (continueReading) {
                readCurrentNews()
            }
        }
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
        ttsStatusChangeProcessor(ttsRequest)
    }


    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {
        super.onAsrResult(asrResult, sttLanguage)


        val triggers = HashMap<String, List<String>>()


        // TODO - define words just once, look into AsrService.phraseListGrammar
        triggers["stop"] = listOf("peatu", "peata", "seisa", "stop", "aitab")
        triggers["start"] = listOf("loe", "alusta", "start")
        triggers["next"] = listOf("järgmine", "edasi", "next")
        triggers["prev"] = listOf("eelmine", "prev")

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
            "stop", "start" -> startStopNewsReading()
            "close" -> closeActivity()
            "prev" -> skipNews(-1)
            "next" -> skipNews(1)
        }

        Log.d(TAG, "onAsrResult. trigger: '$trigger', result: $asrResult");
        if (trigger != "") app.robot.finishConversation()
    }

    override fun onCustomAsrResult(asrResult: String) {
        onAsrResult(asrResult, SttLanguage.ET_EE)
    }

    override fun onBeWithMeStatusChanged(status: String) {
        Log.d(TAG, "onBeWithMeStatusChanged: $status")

        ttsIsReadingNewsItem = false
        continueReading = false

        app.robot.cancelAllTtsRequests()

        newsButtonRead.text = getString(R.string.StartNewsRead)


    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event!!);
    }


    private fun skipNews(skipAmount: Int) {
        newsIndex += skipAmount
        if (newsIndex < 0) {
            newsIndex = newsItems.size - 1
        } else if (newsIndex >= newsItems.size) {
            newsIndex = 0
        }

        Log.d(TAG, "SkipNews $skipAmount, index $newsIndex")

        updateNewsDisplay()

        if (continueReading) {
            readCurrentNews()
        }

    }

    fun newsButtonPrevOnClick(view: View) {
        skipNews(-1)
    }

    fun newsButtonNextOnClick(view: View) {
        skipNews(1)
    }

    override fun onSwipeGesture(direction: String) {
        when (direction) {
            "left" -> skipNews(1)
            "right" -> skipNews(-1)
        }
    }

    // =========================================================== FACE =====================================================
    override fun onFaceRecognized(contactModelList: List<ContactModel>) {
        // do nothing
    }

}