package ee.taltech.aireapplication.helpers

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.face.ContactModel
import com.robotemi.sdk.face.OnFaceRecognizedListener
import com.robotemi.sdk.voice.WakeupOrigin
import com.zeugmasolutions.localehelper.LocaleHelper
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegate
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegateImpl
import ee.taltech.aireapplication.App
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

open class BaseActivity : AppCompatActivity(), Robot.TtsListener, Robot.AsrListener,
    Robot.WakeupWordListener,
    OnFaceRecognizedListener {

    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }


    private val localeDelegate: LocaleHelperActivityDelegate = LocaleHelperActivityDelegateImpl()
    protected lateinit var app: App
    private lateinit var popupTextView: TextView
    private lateinit var popupWindow: PopupWindow

    private lateinit var animationOverlayWindow: PopupWindow
    private lateinit var animationOverlayStatusText: TextView

    protected var ttsStatus: TtsRequest.Status = TtsRequest.Status.COMPLETED
    protected var wakeupWordDetected = false

    override fun getDelegate() = localeDelegate.getAppCompatDelegate(super.getDelegate())

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(localeDelegate.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localeDelegate.onCreate(this)
        app = application as App


        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup, null)
        val animationView = inflater.inflate(R.layout.face_animation, null)

        popupView.setOnTouchListener { view, _ ->
            view.performClick()
            popupWindow.dismiss()
            true
        }

        animationView.setOnTouchListener { view, _ ->
            view.performClick()
            animationOverlayWindow.dismiss()
            true
        }

        val width = ConstraintLayout.LayoutParams.WRAP_CONTENT
        val height = ConstraintLayout.LayoutParams.WRAP_CONTENT
        val focusable = false

        popupWindow = PopupWindow(popupView, width, height, focusable)
        popupTextView = popupView.findViewById(R.id.popupTextView)
        popupTextView.text = ""


        animationOverlayWindow = PopupWindow(
            animationView,
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            false
        )
        animationOverlayStatusText = animationView.findViewById(R.id.textViewAnimationStatus)
        animationOverlayStatusText.text = ""
    }

    override fun onResume() {
        super.onResume()

        app.robot.addTtsListener(this)
        app.robot.addAsrListener(this)
        app.robot.addWakeupWordListener(this)
        app.robot.addOnFaceRecognizedListener(this)

        localeDelegate.onResumed(this)

        if (!App.IS_REAL_TEMI) {
            window.decorView.apply {
                // Hide both the navigation bar and the status bar.
                // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
                // a general rule, you should design your app to hide the status bar whenever you
                // hide the navigation bar.
                systemUiVisibility =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }

    }

    override fun onPause() {
        super.onPause()

        app.robot.removeTtsListener(this)
        app.robot.removeAsrListener(this)
        app.robot.removeWakeupWordListener(this)
        app.robot.removeOnFaceRecognizedListener(this)

        localeDelegate.onPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (popupWindow.isShowing) {
            popupWindow.dismiss()
        }
    }

    override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
        val context = super.createConfigurationContext(overrideConfiguration)
        return LocaleHelper.onAttach(context)
    }

    override fun getApplicationContext(): Context =
        localeDelegate.getApplicationContext(super.getApplicationContext())

    open fun updateLocale(locale: Locale) {
        localeDelegate.setLocale(this, locale)
    }

    fun showFaceAnimation(statusText: String? = null) {
        animationOverlayStatusText.text = statusText ?: ""
        animationOverlayWindow.showAtLocation(window.decorView, Gravity.NO_GRAVITY, 0, 0)
    }

    fun hideFaceAnimation() {
        if (animationOverlayWindow.isShowing) {
            animationOverlayWindow.dismiss()
            animationOverlayStatusText.text = ""
        }
    }

    fun updateFaceAnimationStatusText(statusText: String) {
        animationOverlayStatusText.text = statusText
    }


    fun showPopup(text: String) {
        if (popupWindow.isShowing) {
            popupWindow.dismiss()
        }

        popupTextView.text = text
        popupTextView.clearAnimation()

        popupWindow.showAtLocation(window.decorView, Gravity.BOTTOM, 0, 100)
    }


    fun hidePopup(duration: Long = 4000) {
        if (popupWindow.isShowing) {
            val animation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            animation.duration = duration
            animation.fillAfter = true
            popupTextView.startAnimation(animation)

            Handler(Looper.getMainLooper()).postDelayed({
                if (popupWindow.isShowing) {
                    popupTextView.clearAnimation()
                    popupWindow.dismiss()
                }
            }, duration)

        }
    }

    fun showPopup(text: String, duration: Long = 4000) {
        if (popupWindow.isShowing) {
            popupWindow.dismiss()
        }

        popupTextView.text = text
        popupTextView.clearAnimation()

        popupWindow.showAtLocation(
            window.decorView, Gravity.BOTTOM,
            32, 100
        )

        val animation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        animation.duration = duration
        animation.fillAfter = true
        popupTextView.startAnimation(animation)

        Handler(Looper.getMainLooper()).postDelayed({
            if (popupWindow.isShowing) {
                popupTextView.clearAnimation()
                popupWindow.dismiss()
            }
        }, duration)
    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        when (ttsRequest.status) {
            TtsRequest.Status.STARTED -> {
                showPopup(ttsRequest.speech, 4000L)
            }

            else -> {}
        }
    }

    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {
        wakeupWordDetected = false
    }

    override fun onWakeupWord(wakeupWord: String, direction: Int, origin: WakeupOrigin) {
        Log.d(TAG, "onWakeupWord: $wakeupWord")
    }


    // =========================================================== FACE =====================================================
    private var faceDetectionDebounce = false
    var faceDetectionDisabled = false

    // should load dynamically on every access
    private val faceStrings: List<String>
        get() =
            listOf(
                SettingsRepository.getLangString(
                    this,
                    "faceDetectionPhrase0",
                    getString(R.string.face_recognized_0)
                ),
                SettingsRepository.getLangString(
                    this,
                    "faceDetectionPhrase1",
                    getString(R.string.face_recognized_1)
                ),
                SettingsRepository.getLangString(
                    this,
                    "faceDetectionPhrase2",
                    getString(R.string.face_recognized_2)
                ),
                SettingsRepository.getLangString(
                    this,
                    "faceDetectionPhrase3",
                    getString(R.string.face_recognized_3)
                ),
                SettingsRepository.getLangString(
                    this,
                    "faceDetectionPhrase4",
                    getString(R.string.face_recognized_4)
                )
            )

    fun getFaceString(contactModelList: List<ContactModel>): String {
        var sentence = faceStrings[faceStrings.indices.random()]

        // TODO: check, that user id is in registered users
        try {
            contactModelList[0].userId.toInt()
            sentence = sentence.replace("<%name%>", "")
        } catch (e: NumberFormatException) {
            // is name usage turned on?
            sentence = if (SettingsRepository.getBoolean(
                    this,
                    "personalizeFaceDetectionMessages",
                    resources.getBoolean(R.bool.personalizeFaceDetectionMessages)
                )
            ) {
                sentence.replace("<%name%>", contactModelList[0].userId + "! ")
            } else {
                sentence.replace("<%name%>", "")
            }
        }

        sentence = sentence.trim()

        return sentence
    }

    override fun onFaceRecognized(contactModelList: List<ContactModel>) {
        if (contactModelList.isEmpty()) {
            Log.d(TAG, "onFaceRecognized: person left")
            return
        } else {

            if (faceDetectionDebounce || faceDetectionDisabled) {
                Log.d(
                    TAG,
                    "onFaceRecognized - debounce:$faceDetectionDebounce disabled:$faceDetectionDisabled count:${contactModelList.size}"
                )
                return
            }

            faceDetectionDebounce = true

            if (ttsStatus == TtsRequest.Status.COMPLETED) {
                Log.d(
                    TAG,
                    "Face Recognized count: ${contactModelList.size}, first userId: ${contactModelList[0].userId}, similarity: ${contactModelList[0].similarity}"
                )

                applicationScope.launch {
                    BackendApiKtorSingleton.logEvent(
                        tag = "$TAG.faceRecognized",
                        message = "Face Recognized count: ${contactModelList.size}, first userId: ${contactModelList[0].userId}, similarity: ${contactModelList[0].similarity}"
                    )
                }

                app.robot.speak(
                    TtsRequest.create(
                        getFaceString(contactModelList),
                        false,
                        TtsRequest.Language.ET_EE,
                        showAnimationOnly = false,
                        cached = false
                    )
                )
            } else {
                Log.w(TAG, "Skipped face recognition TTS, TTS not done")
            }

            applicationScope.launch {
                delay(10000L)
                faceDetectionDebounce = false
            }
        }
    }


}