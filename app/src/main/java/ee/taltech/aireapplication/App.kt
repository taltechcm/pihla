package ee.taltech.aireapplication

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.constants.Mode
import com.robotemi.sdk.exception.OnSdkExceptionListener
import com.robotemi.sdk.exception.SdkException
import com.robotemi.sdk.face.ContactModel
import com.robotemi.sdk.face.OnContinuousFaceRecognizedListener
import com.robotemi.sdk.face.OnFaceRecognizedListener
import com.robotemi.sdk.listeners.OnBeWithMeStatusChangedListener
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.map.MapDataModel
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.listener.OnReposeStatusChangedListener
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.voice.WakeupOrigin
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.zeugmasolutions.localehelper.LocaleHelper
import com.zeugmasolutions.localehelper.LocaleHelperApplicationDelegate
import ee.taltech.aireapplication.AnalyzeActivity.Companion
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BroadcastReceiverInActivity
import ee.taltech.aireapplication.helpers.C
import ee.taltech.aireapplication.helpers.CustomAsrListener
import ee.taltech.aireapplication.helpers.SettingsRepository
import ee.taltech.aireapplication.locations.LocationsRepository
import kotlinx.coroutines.MainScope
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.sqrt

/* AsrListener, ConversationViewAttachesListener, NlpListener, OnBatteryStatusChangedListener, OnBeWithMeStatusChangedListener, OnConstraintBeWithStatusChangedListener,
OnContinuousFaceRecognizedListener, OnConversationStatusChangedListener, OnCurrentPositionChangedListener, OnDetectionDataChangedListener, OnDetectionStateChangedListener,
OnDisabledFeatureListUpdatedListener, OnDistanceToDestinationChangedListener, OnDistanceToLocationChangedListener, OnFaceRecognizedListener, OnGoToLocationStatusChangedListener,
OnGreetModeStateChangedListener, OnLoadFloorStatusChangedListener, OnLoadMapStatusChangedListener, OnLocationsUpdatedListener, OnMovementStatusChangedListener,
OnMovementVelocityChangedListener, OnPrivacyModeStateChangedListener, OnReposeStatusChangedListener, OnRequestPermissionResultListener, OnRobotLiftedListener, OnRobotReadyListener,
OnSdkExceptionListener, OnSequencePlayStatusChangedListener, OnTelepresenceEventChangedListener, OnTelepresenceStatusChangedListener, OnTtsVisualizerWaveFormDataChangedListener,
OnUserInteractionChangedListener, OnUsersUpdatedListener TtsListener, WakeUpWordListener
 */

class App : Application(), OnSdkExceptionListener, OnGoToLocationStatusChangedListener,
    OnReposeStatusChangedListener, OnRobotReadyListener, OnCurrentPositionChangedListener,
    //OnFaceRecognizedListener,
    //OnContinuousFaceRecognizedListener,
    Robot.WakeupWordListener, Robot.AsrListener {

    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        var IS_REAL_TEMI = !isEmulator

        // id-s used to identify robot, app and launch session
        lateinit var ANDROID_ID: String
        lateinit var MAP_ID: String
        lateinit var MAP_NAME: String
        lateinit var APP_NAME: String
        val APP_LAUNCH_ID = System.currentTimeMillis().toString()

        var applicationScope = MainScope()

        private val isEmulator: Boolean
            get() = (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.HARDWARE.contains("goldfish")
                    || Build.HARDWARE.contains("ranchu")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.PRODUCT.contains("sdk_google")
                    || Build.PRODUCT.contains("google_sdk")
                    || Build.PRODUCT.contains("sdk")
                    || Build.PRODUCT.contains("sdk_x86")
                    || Build.PRODUCT.contains("sdk_gphone64_arm64")
                    || Build.PRODUCT.contains("vbox86p")
                    || Build.PRODUCT.contains("emulator")
                    || Build.PRODUCT.contains("simulator");

    }

    private val broadcastReceiver = BroadcastReceiverInActivity(TAG) { asrIntent ->
        receiveCustomAsrIntent(
            asrIntent!!
        )
    }
    private val broadcastReceiverIntentFilter = IntentFilter().apply { addAction(C.INTENT_ASR) }

    private val localeAppDelegate = LocaleHelperApplicationDelegate()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(localeAppDelegate.attachBaseContext(base))
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeAppDelegate.onConfigurationChanged(this)
    }

    // app variables
    lateinit var robot: Robot
    var locationsRepository: LocationsRepository? = null


    var doOnce = true

    //var faceDetectionDebounce = false
    //var faceDetectionDisabled = false


    var createOnce = true

    @Volatile
    var mapDataModel: MapDataModel? = null

    //VARIABLES
    var speechLanguage = TtsRequest.Language.ET_EE
    //var ttsStatus: TtsRequest.Status = TtsRequest.Status.COMPLETED

    private var previousPosition: Position? = null
    private var distanceTravelled: Double = 0.0
    private var distanceUpdateTimestamp: Long = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()

        // declare app variables
        robot = Robot.getInstance()
        ANDROID_ID = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        if (ANDROID_ID.isEmpty()) {
            ANDROID_ID = UUID.randomUUID().toString()
        }
        APP_NAME = getString(R.string.app_name_short)

        Log.d(TAG, "APP create. Emulator: $isEmulator")

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)



    }

    // this only works on emulator
    // https://developer.android.com/reference/android/app/Application#onTerminate()
    override fun onTerminate() {
        //removeRobotListeners() //doesn't always work, should add welcome and end screens to app instead

        robot.stopFaceRecognition()

        Log.w(TAG, "app terminate")
        // should we really do it here?
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(tag = TAG, message = "onTerminate")
        }

        super.onTerminate()
    }

    private fun addListeners() {
        robot.addOnSdkExceptionListener(this)
        robot.addOnGoToLocationStatusChangedListener(this)
        robot.addOnReposeStatusChangedListener(this)
        robot.addOnRobotReadyListener(this)
        //robot.addOnFaceRecognizedListener(this)
        // robot.addOnContinuousFaceRecognizedListener(this)

        robot.addWakeupWordListener(this)
        robot.addAsrListener(this)
        robot.addOnCurrentPositionChangedListener(this)

    }

    private fun removeListeners() {
        robot.removeOnSdkExceptionListener(this)
        robot.removeOnGoToLocationStatusChangedListener(this)
        robot.removeOnReposeStatusChangedListener(this)
        robot.removeOnRobotReadyListener(this)
        //robot.removeOnFaceRecognizedListener(this)
        // robot.removeOnContinuousFaceRecognizedListener(this)

        robot.removeWakeupWordListener(this)
        robot.removeAsrListener(this)
        robot.removeOnCurrentPositionChangedListener(this)

    }

    fun startApp() {
        Log.d(TAG, "startApp, adding listeners")

        addListeners()

        //robot.setMode(Mode.GREET)
        //addedListeners = true

        // load map info
        mapDataModel = robot.getMapData()
        MAP_ID = mapDataModel?.mapId ?: "-"
        MAP_NAME = mapDataModel?.mapName ?: "-"


        var mapNameOverride = SettingsRepository.getString(
            this,
            "mapNameOverride",
            ""
        )

        var robotNameOverride = SettingsRepository.getString(
            this,
            "robotNameOverride",
            ""
        )

        if (mapNameOverride.isNotEmpty()) {
            MAP_NAME = mapNameOverride
            MAP_ID = mapNameOverride
        }

        if (robotNameOverride.isNotEmpty()) {
            ANDROID_ID = robotNameOverride
        }
    }

    fun onAppStart() {
        Log.d(TAG, "onAppStart")
        robot.setKioskModeOn(true)
        robot.startFaceRecognition(withSdkFaces = true)
        robot.setMultiFloorEnabled(true)

    }

    fun endApp() {
        Log.d(TAG, "endApp")

        robot.stopFaceRecognition()

        removeListeners()

        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(broadcastReceiver)

        robot.setKioskModeOn(false)
        createOnce = true
        robot.setMode(Mode.DEFAULT)
    }


    // robot callbacks
    override fun onSdkError(sdkException: SdkException) {
        Log.e(TAG, sdkException.message)
        Log.e(TAG, "${sdkException.code}")
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.onSdkError",
                message = sdkException.message
            )
        }
    }

    override fun onGoToLocationStatusChanged(
        //https://github.com/robotemi/sdk/wiki/Locations#onGoToLocationStatusChangedListener
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        when (descriptionId) {
            1006 -> {
                robot.repose()
            }
        }
    }

    override fun onReposeStatusChanged(status: Int, description: String) {
        // https://github.com/robotemi/sdk/wiki/Locations#onReposeStatusChangedListener
        when (status) {
            0 -> "idle"
            1 -> "reposing required"
            2 -> "reposing start"
            3 -> "reposing going"
            4 -> "reposing complete"
            5 -> "reposing obstacle detected"
            6 -> "reposing abort"
        }
    }

    override fun onRobotReady(isReady: Boolean) {
        Log.d(TAG, "onRobotReady")
        if (isReady) {
            if (doOnce) {
                doOnce = false
                onAppStart()
            }
            locationsRepository = LocationsRepository(applicationContext, robot.locations)
        }
    }

    // TODO: update these values after settings activity has been visited
    /*
    private val faceStrings: List<String> by lazy {
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
    }

*/



    /*
    override fun onFaceRecognized(contactModelList: List<ContactModel>) {

        if (contactModelList.isEmpty()) {
            Log.d(TAG, "onFaceRecognized: person left")
            return
        } else {

            if (faceDetectionDebounce || faceDetectionDisabled) {
                Log.d(
                    TAG,
                    "onFaceRecognized: debounce $faceDetectionDebounce disabled $faceDetectionDisabled count ${contactModelList.size}"
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


                robot.speak(
                    TtsRequest.create(
                        getFaceString(contactModelList),
                        false,
                        TtsRequest.Language.ET_EE,
                        showAnimationOnly = false,
                        cached = false
                    )
                )
            } else {
                Log.w(TAG, "Skipped face recognition TTS")
            }

            applicationScope.launch {
                delay(10000L)
                faceDetectionDebounce = false
            }


        }
    }
    */


    override fun getApplicationContext(): Context =
        LocaleHelper.onAttach(super.getApplicationContext())

    fun speak(sentence: String, shown: Boolean) {
        robot.speak(TtsRequest.create(sentence, false, speechLanguage, shown, true))
    }

    private fun isPackageExist(context: Context, targetPackage: String): Boolean {
        val pm: PackageManager = context.packageManager
        try {
            pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
        return true
    }

    fun showToast(context: Context, message: String): Toast {
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast.show()
        return toast
    }

    fun showLongToast(context: Context, message: String): Toast {
        val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
        toast.show()
        return toast
    }


    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {
        Log.d(TAG, "onAsrResult $asrResult, lang: ${sttLanguage.name}")

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG", message = "onAsrResult $asrResult, lang: ${sttLanguage.name}"
            )
        }
    }


// ================== speech =======================

    private val customAsrListeners = CopyOnWriteArraySet<CustomAsrListener>()

    @UiThread
    fun addCustomAsrListener(asrListener: CustomAsrListener) {
        customAsrListeners.add(asrListener)
    }

    @UiThread
    fun removeCustomAsrListener(asrListener: CustomAsrListener) {
        customAsrListeners.remove(asrListener)
    }

    private fun receiveCustomAsrIntent(intent: Intent) {
        val txt = intent.getStringExtra(C.INTENT_ASR_TEXT) ?: ""
        showToast(this, txt)
        customAsrListeners.forEach { listener ->
            listener.onCustomAsrResult(txt)
        }
    }

    override fun onCurrentPositionChanged(position: Position) {
        previousPosition?.apply {
            var curTime = System.currentTimeMillis()
            val x2 = this.x.toDouble() - position.x.toDouble()
            val y2 = this.y.toDouble() - position.y.toDouble()
            val distance = sqrt(x2 * x2 + y2 * y2)
            distanceTravelled += distance

            if (((curTime - distanceUpdateTimestamp) > (60 * 1000)) && (distanceTravelled > 1.0)) {

                val msg =
                    "In ${(curTime - distanceUpdateTimestamp) / 1000.0} seconds travelled $distanceTravelled meters"
                Log.d(TAG, msg)

                val distanceTravelledCopy = distanceTravelled

                applicationScope.launch {
                    BackendApiKtorSingleton.logEvent(
                        tag = "$TAG.distance",
                        message = msg,
                        doubleValue = distanceTravelledCopy
                    )
                }

                distanceTravelled = 0.0
                distanceUpdateTimestamp = curTime
            }
        }
        previousPosition = position
    }

    override fun onWakeupWord(wakeupWord: String, direction: Int, origin: WakeupOrigin) {
        Log.d(TAG, "onWakeupWord: $wakeupWord")
    }

    /*
    override fun onContinuousFaceRecognized(contactModelList: List<ContactModel>) {
        Log.d(TAG, "onContFace ${contactModelList.size}");
    }
    */

}