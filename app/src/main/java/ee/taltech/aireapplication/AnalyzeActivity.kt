package ee.taltech.aireapplication

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.robotemi.sdk.*
import com.robotemi.sdk.Robot.*
import com.robotemi.sdk.Robot.Companion.getInstance
import com.robotemi.sdk.TtsRequest.Companion.create
import com.robotemi.sdk.exception.OnSdkExceptionListener
import com.robotemi.sdk.exception.SdkException
import com.robotemi.sdk.face.ContactModel
import com.robotemi.sdk.face.OnContinuousFaceRecognizedListener
import com.robotemi.sdk.face.OnFaceRecognizedListener
import com.robotemi.sdk.listeners.*
import com.robotemi.sdk.permission.OnRequestPermissionResultListener
import com.robotemi.sdk.permission.Permission
import com.robotemi.sdk.voice.WakeupOrigin
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BaseActivity
import ee.taltech.aireapplication.helpers.CustomAsrListener
import kotlinx.coroutines.launch

private data class ImportantData(
    val Age: Int,
    val Gender: Int
)

class AnalyzeActivity : BaseActivity(), TtsListener, OnRequestPermissionResultListener,
    OnSdkExceptionListener, OnRobotReadyListener, OnLocationsUpdatedListener,
    OnFaceRecognizedListener, OnContinuousFaceRecognizedListener,
    CustomAsrListener {

    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var places: List<String>
    private lateinit var robot: Robot

    private lateinit var analyzeButton: Button
    private lateinit var returnButton: Button
    private lateinit var dialogText: TextView
    private lateinit var progressBar: ProgressBar

    private var increment = 0
    private var DataList = mutableListOf<ImportantData>()
    private var scanning = false

    private var retryIncrement = 0

    private fun refreshTemiUi() {
        try {
            val activityInfo = packageManager
                .getActivityInfo(componentName, PackageManager.GET_META_DATA)
            Robot.getInstance().onStart(activityInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }


    // ACTIVITY STATES
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyze)

        robot = getInstance()

        analyzeButton = findViewById(R.id.analyzeButton)
        returnButton = findViewById(R.id.analyzeBackButton)
        dialogText = findViewById(R.id.dialogText)
        progressBar = findViewById(R.id.progressBar)

        app = application as App


    }

    override fun onResume() {
        super.onResume()
        //robot.hideTopBar()

        refreshTemiUi()

        robot.addOnFaceRecognizedListener(this)
        robot.addOnContinuousFaceRecognizedListener(this)
        robot.addOnRobotReadyListener(this)
        robot.addTtsListener(this)

        app.addCustomAsrListener(this)
    }

    override fun onPause() {
        super.onPause()

        robot.removeOnFaceRecognizedListener(this)
        robot.removeOnContinuousFaceRecognizedListener(this)
        robot.removeOnRobotReadyListener(this)
        robot.removeTtsListener(this)

        app.removeCustomAsrListener(this)
    }


    override fun onDestroy() {
        super.onDestroy()
        // robot.startFaceRecognition(withSdkFaces = true)
    }

    // LISTENERS
    override fun onRobotReady(isReady: Boolean) {
        //places = robot.locations
        if (isReady) {
            //robot.hideTopBar()
            if (!robot.isKioskModeOn()) {
                robot.setKioskModeOn(true)
            }

        }

    }

    override fun onSdkError(sdkException: SdkException) {
        speak("Error")
    }

    override fun onRequestPermissionResult(
        permission: Permission,
        grantResult: Int,
        requestCode: Int
    ) {
    }

    override fun onLocationsUpdated(locations: List<String>) {
        places = robot.locations
    }

    // FUNCTIONS
    fun speak(sentence: Any?) {
        val sentenceString = sentence?.toString() ?: ""
        robot.speak(create(sentenceString, false, TtsRequest.Language.SYSTEM, true, false))
    }

    fun shortToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun log(tag: String, message: String) {
        Log.d(tag, message)
    }

    //https://github.com/robotemi/sdk/blob/master/sample/src/main/java/com/robotemi/sdk/sample/MainActivity.kt
    private fun requestAll() {
        val permissions: MutableList<Permission> = ArrayList()
        for (permission in Permission.values()) {
            if (robot.checkSelfPermission(permission) == Permission.GRANTED) {
                //printLog("You already had $permission permission.")
                continue
            }
            permissions.add(permission)
        }
        robot.requestPermissions(permissions, 0) //REQUEST_CODE_NORMAL
    }

    private fun requestPermissionIfNeeded(permission: Permission, requestCode: Int): Boolean {
        if (robot.checkSelfPermission(permission) == Permission.GRANTED) {
            return false
        }
        robot.requestPermissions(listOf(permission), requestCode)
        return true
    }

    // VIEWS

    fun backButtonOnClick(view: View) {
        finish()
    }

    fun kioskButtonOnClick(view: View) {
        val boolean = robot.isKioskModeOn()
        if (boolean) {
            robot.setKioskModeOn(false)
            Toast.makeText(this, "kiosk mode OFF", Toast.LENGTH_SHORT).show()
        } else {
            robot.setKioskModeOn(true)
            Toast.makeText(this, "kiosk mode ON", Toast.LENGTH_SHORT).show()
        }
    }

    fun permissionsButtonOnClick(view: View) {
        val permissions: MutableList<Permission> = ArrayList()
        for (permission in Permission.values()) {
            if (robot.checkSelfPermission(permission) == Permission.GRANTED) {
                continue
            }
            permissions.add(permission)
        }
        robot.requestPermissions(permissions, 1)
    }

    fun speakButtonOnClick(view: View) {
        robot.speak(create("Hello", false, TtsRequest.Language.EN_US, true, false))
    }

    override fun onContinuousFaceRecognized(contactModelList: List<ContactModel>) {

        Log.d("onContinuousFaceRecognized", contactModelList.joinToString("\n"))

        if (contactModelList.isNullOrEmpty()) {
            return
        }
        if (!scanning) {
            return
        }
        if (increment < 1) {

        }

        var gender = 0
        gender = if (contactModelList[0].gender == "male") {
            0
        } else {
            1
        }

        if (increment < 9) {
            DataList.add(ImportantData(contactModelList[0].age, gender))
            progressBar.progress += 10
            increment += 1
        } else if (increment == 9) {
            progressBar.progress += 10



            scanning = false

            // robot.stopFaceRecognition()

            robot.stopMovement()
            increment == 0
            //SCANSTOP


            var averageAge = 0
            var averageGender = 0
            DataList.forEach {
                averageAge += it.Age
                averageGender += it.Gender
            }

            var finalAge = 0
            var finalGender = ""


            finalAge = averageAge / 9
            if (app.speechLanguage == TtsRequest.Language.ET_EE) {
                if (averageGender >= 5) {
                    finalGender = "nais"
                } else {
                    finalGender = "mees"
                }
                robot.speak(
                    create(
                        "Sa oled $finalAge aastane ja ilmselt $finalGender" + "soost.",
                        false,
                        TtsRequest.Language.ET_EE,
                        false,
                        true
                    )
                )

            } else {
                if (averageGender >= 5) {
                    finalGender = "female"
                } else {
                    finalGender = "male"
                }
                robot.speak(
                    create(
                        "You are $finalAge years old and probably $finalGender.",
                        false,
                        TtsRequest.Language.EN_US,
                        false,
                        true
                    )
                )

            }

            //DataList.clear()


        }
    }

    override fun onFaceRecognized(contactModelList: List<ContactModel>) {

        if (contactModelList.isNullOrEmpty()) {
            if (scanning) {
                retryIncrement -= 1
                if (retryIncrement > 1) {
                    return
                }
                scanning = false
                robot.stopMovement()
                //SCANSTOP
                robot.speak(
                    create(
                        getString(R.string.scanningfailed),
                        false,
                        app.speechLanguage,
                        false,
                        true
                    )
                )
                //DataList.clear() //NEEDS TO BE INSIDE FUNCTION OR THIS GLITCH WILL HAPPEN AGAIN
                Log.d("TEST3", "lost")

                Log.d("TEST3", "STOP")


            }
        } else {
            //
            Log.d("TEST3", "found")


        }
    }

    fun closeButtonOnClick(view: View) {
        robot.setKioskModeOn(false)
        finish()
    }

    fun analyzeButtonOnClick(view: View) {
        doAnalyze()
    }

    fun doAnalyze() {
        if (scanning) {
            scanning = false
            increment = 0
            progressBar.progress = 0
            robot.stopMovement()
            //SCANSTOP
        } else {
            DataList.clear()

            increment = 0
            progressBar.progress = 0
            scanning = true
            //SCANSTART
            robot.constraintBeWith()

            Log.d("test3", "START")

            retryIncrement = 5
        }
        robot.speak(create(getString(R.string.scan), false, app.speechLanguage, false, true))
        //  robot.startFaceRecognition(withSdkFaces = false)
    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        when (ttsRequest.status) {
            TtsRequest.Status.STARTED -> {
                dialogText.text = ttsRequest.speech
            }

            TtsRequest.Status.COMPLETED -> {
                dialogText.text == ""
            }

            else -> {}
        }
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
            "start" -> {
                doAnalyze()
            }

            "close" -> {
                app.robot.cancelAllTtsRequests()

                applicationScope.launch {
                    BackendApiKtorSingleton.logEvent(
                        tag = "${TAG}.onAsrResult",
                        message = "closing activity"
                    )
                }

                returnButton.text = getString(R.string.Close)

                finish()
            }
        }
    }


    override fun onCustomAsrResult(asrResult: String) {
        onAsrResult(asrResult, SttLanguage.ET_EE)
    }

}
