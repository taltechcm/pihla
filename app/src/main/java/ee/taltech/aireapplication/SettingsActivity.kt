package ee.taltech.aireapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowMetrics
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.robotemi.sdk.permission.Permission
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.dto.MapLocation2Sync
import ee.taltech.aireapplication.dto.MapLocationSync
import ee.taltech.aireapplication.face.FaceActivity
import ee.taltech.aireapplication.helpers.AsrService
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BaseActivity
import ee.taltech.aireapplication.helpers.SettingsRepository
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import kotlin.system.exitProcess


class SettingsActivity : BaseActivity() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }


    private val password = "74000323"

    private val settingsPasswordValue: String
        get() = SettingsRepository.getString(
            this,
            "settingsPassword",
            "012345"
        )

    private var enteredPassword = ""

    private lateinit var settingsLayout: ConstraintLayout
    private lateinit var settingsConstraintLayoutPwdButtons: ConstraintLayout
    private lateinit var settingsPasswordTextView: TextView
    private lateinit var settingsButtonReturn: Button


    private lateinit var patrolCheckBoxSingleRun: CheckBox

    private lateinit var patrolCheckBoxUseRepeatMessage: CheckBox
    private lateinit var patrolEditTextRepeatMessage: EditText
    private lateinit var patrolEditTextRepeatMessageInterval: EditText

    private lateinit var patrolCheckBoxUseFaceDetection: CheckBox

    private lateinit var patrolCheckBoxUseArrivalMessage: CheckBox
    private lateinit var patrolEditTextArrivalMessage: EditText
    private lateinit var patrolCheckBoxUsePauseAfterArrival: CheckBox
    private lateinit var patrolEditTextArrivalPauseDuration: EditText

    private lateinit var patrolCheckBoxUseFaceDetectionCustomMessage: CheckBox
    private lateinit var patrolEditTextFaceDetectionCustomMessage: EditText

    private lateinit var faceDetectionPhrase0: EditText
    private lateinit var faceDetectionPhrase1: EditText
    private lateinit var faceDetectionPhrase2: EditText
    private lateinit var faceDetectionPhrase3: EditText
    private lateinit var faceDetectionPhrase4: EditText


    private lateinit var mainActivityButtonArticle1Visible: CheckBox
    private lateinit var mainActivityButtonArticle2Visible: CheckBox
    private lateinit var mainActivityButtonArticle3Visible: CheckBox

    private lateinit var mainActivityDisplayFaceReg: CheckBox

    private lateinit var mainActivityDisplayButtonVideo: CheckBox
    private lateinit var mainActivityDisplayButtonRepose: CheckBox
    private lateinit var  mainActivityDisplayButtonPatrol: CheckBox

    private lateinit var textViewRobotDetails: TextView

    private lateinit var activationPhrase: EditText

    private lateinit var robotNameOverride: EditText
    private lateinit var mapNameOverride: EditText
    private lateinit var personalizeFaceDetectionMessages: CheckBox

    private lateinit var settingsPassword: EditText


    private lateinit var settingsProgressBarUpdate: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsLayout = findViewById(R.id.settingsLayout)
        settingsConstraintLayoutPwdButtons = findViewById(R.id.settingsConstraintLayoutPwdButtons)
        settingsLayout.visibility = View.INVISIBLE
        settingsPasswordTextView = findViewById(R.id.settingsPasswordTextView)
        settingsPasswordTextView.text = ""
        settingsButtonReturn = findViewById(R.id.settingsButtonReturn)


        patrolCheckBoxSingleRun = findViewById(R.id.patrolCheckBoxSingleRun)

        patrolCheckBoxUseRepeatMessage = findViewById(R.id.patrolCheckBoxUseRepeatMessage)
        patrolEditTextRepeatMessage = findViewById(R.id.patrolEditTextRepeatMessage)
        patrolEditTextRepeatMessageInterval = findViewById(R.id.patrolEditTextRepeatMessageInterval)

        patrolCheckBoxUseFaceDetection = findViewById(R.id.patrolCheckBoxUseFaceDetection)

        patrolCheckBoxUseArrivalMessage = findViewById(R.id.patrolCheckBoxUseArrivalMessage)
        patrolEditTextArrivalMessage = findViewById(R.id.patrolEditTextArrivalMessage)
        patrolCheckBoxUsePauseAfterArrival = findViewById(R.id.patrolCheckBoxUsePauseAfterArrival)
        patrolEditTextArrivalPauseDuration = findViewById(R.id.patrolEditTextArrivalPauseDuration)
        patrolCheckBoxUseFaceDetectionCustomMessage =
            findViewById(R.id.patrolCheckBoxUseFaceDetectionCustomMessage)
        patrolEditTextFaceDetectionCustomMessage =
            findViewById(R.id.patrolEditTextFaceDetectionCustomMessage)

        faceDetectionPhrase0 = findViewById(R.id.faceDetectionPhrase0)
        faceDetectionPhrase1 = findViewById(R.id.faceDetectionPhrase1)
        faceDetectionPhrase2 = findViewById(R.id.faceDetectionPhrase2)
        faceDetectionPhrase3 = findViewById(R.id.faceDetectionPhrase3)
        faceDetectionPhrase4 = findViewById(R.id.faceDetectionPhrase4)

        mainActivityButtonArticle1Visible = findViewById(R.id.mainActivityButtonArticle1Visible)

        mainActivityButtonArticle2Visible = findViewById(R.id.mainActivityButtonArticle2Visible)
        mainActivityButtonArticle3Visible = findViewById(R.id.mainActivityButtonArticle3Visible)

        mainActivityDisplayFaceReg = findViewById(R.id.mainActivityDisplayFaceReg)

        mainActivityDisplayButtonVideo = findViewById(R.id.mainActivityDisplayButtonVideo)
        mainActivityDisplayButtonRepose = findViewById(R.id.mainActivityDisplayButtonRepose)
        mainActivityDisplayButtonPatrol= findViewById(R.id.mainActivityDisplayButtonPatrol)

        activationPhrase = findViewById(R.id.activationPhrase)


        robotNameOverride = findViewById(R.id.robotNameOverride)
        mapNameOverride = findViewById(R.id.mapNameOverride)
        personalizeFaceDetectionMessages = findViewById(R.id.personalizeFaceDetectionMessages)

        textViewRobotDetails = findViewById(R.id.textViewRobotDetails)

        settingsPassword = findViewById(R.id.settingsPassword)

        settingsProgressBarUpdate = findViewById(R.id.settingsProgressBarUpdate)
        settingsProgressBarUpdate.visibility = View.INVISIBLE

        loadSavedValues()

        fillRobotDetails()
    }

    private fun fillRobotDetails() {
        val pInfo: PackageInfo =
            this.packageManager.getPackageInfo(this.packageName, 0)

        val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics

        val height = windowMetrics.bounds.height()
        val width = windowMetrics.bounds.width()



        textViewRobotDetails.text = "ANDROID_ID: ${App.ANDROID_ID}\n" +
                "MAP_ID: ${App.MAP_ID}\n" +
                "MAP_NAME: ${App.MAP_NAME}\n" +
                "APP_NAME: ${App.APP_NAME}\n" +
                "APP_LAUNCH_ID: ${App.APP_LAUNCH_ID}\n" +
                "Version code: ${pInfo.longVersionCode}\n" +
                "Version name: ${pInfo.versionName}\n" +
                "Window: $width x $height \n" +
                "Pos: ${app.robot.getPosition()}\n" +
                "Multifloor: ${app.robot.isMultiFloorEnabled()}\n" +
                app.robot.getAllFloors().map { f -> f.toString() }.joinToString("\n") + "\n" +
                "Is real temi:${App.IS_REAL_TEMI}\n" +
                "FINGERPRINT:${Build.FINGERPRINT}\n" +
                "MODEL:${Build.MODEL}\n" +
                "MANUFACTURER:${Build.MANUFACTURER}\n" +
                "BRAND:${Build.BRAND}\n" +
                "DEVICE:${Build.DEVICE}\n" +
                "BOARD:${Build.BOARD}\n" +
                "HOST:${Build.HOST}\n" +
                "PRODUCT:${Build.PRODUCT}\n"

        app.robot.getAllFloors().forEach { f ->
            Log.d(TAG, "Floor: $f")
        }
    }

    fun activitySettingsOnClick(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun passwordNoOnClick(view: View) {
        val elem = view as Button

        enteredPassword += elem.text.toString()
        if (enteredPassword.length > 10) {
            enteredPassword =
                enteredPassword.substring(enteredPassword.length - 10, enteredPassword.length)
        }
        settingsPasswordTextView.text = enteredPassword

        if (enteredPassword.contains(password) || enteredPassword.contains(settingsPasswordValue)) {
            settingsLayout.visibility = View.VISIBLE
            settingsConstraintLayoutPwdButtons.visibility = View.INVISIBLE
            enteredPassword = ""
            settingsPasswordTextView.text = enteredPassword
        }
    }

    fun settingsButtonReturnOnClick(view: View) {
        settingsButtonReturn.text = getString(R.string.Close)
        finish()
    }

    override fun onPause() {
        super.onPause()
        saveValues()
    }

    private fun loadSavedValues() {
        patrolCheckBoxSingleRun.isChecked =
            SettingsRepository.getBoolean(
                this,
                "patrolCheckBoxSingleRun",
                resources.getBoolean(R.bool.patrolCheckBoxSingleRun)
            )
        patrolCheckBoxUseRepeatMessage.isChecked =
            SettingsRepository.getBoolean(
                this,
                "patrolCheckBoxUseRepeatMessage",
                resources.getBoolean(R.bool.patrolCheckBoxUseRepeatMessage)
            )
        patrolCheckBoxUseFaceDetection.isChecked =
            SettingsRepository.getBoolean(
                this,
                "patrolCheckBoxUseFaceDetection",
                resources.getBoolean(R.bool.patrolCheckBoxUseFaceDetection)
            )
        patrolCheckBoxUseArrivalMessage.isChecked =
            SettingsRepository.getBoolean(
                this,
                "patrolCheckBoxUseArrivalMessage",
                resources.getBoolean(R.bool.patrolCheckBoxUseArrivalMessage)
            )
        patrolCheckBoxUsePauseAfterArrival.isChecked =
            SettingsRepository.getBoolean(
                this,
                "patrolCheckBoxUsePauseAfterArrival",
                resources.getBoolean(R.bool.patrolCheckBoxUsePauseAfterArrival)
            )

        patrolEditTextRepeatMessage.setText(
            SettingsRepository.getLangString(
                this,
                "patrolEditTextRepeatMessage",
                getString(R.string.patrolEditTextRepeatMessage)
            )
        )
        patrolEditTextRepeatMessageInterval.setText(
            SettingsRepository.getInt(
                this,
                "patrolRepeatMessageInterval",
                5
            ).toString()
        )


        patrolEditTextArrivalMessage.setText(
            SettingsRepository.getLangString(
                this,
                "patrolEditTextArrivalMessage",
                getString(R.string.patrolEditTextArrivalMessage)
            )
        )
        patrolEditTextArrivalPauseDuration.setText(
            SettingsRepository.getInt(
                this,
                "patrolArrivalPauseDuration",
                5
            ).toString()
        )

        patrolCheckBoxUseFaceDetectionCustomMessage.isChecked =
            SettingsRepository.getBoolean(
                this,
                "patrolCheckBoxUseFaceDetectionCustomMessage",
                resources.getBoolean(R.bool.patrolCheckBoxUseFaceDetectionCustomMessage)
            )

        patrolEditTextFaceDetectionCustomMessage.setText(
            SettingsRepository.getLangString(
                this,
                "patrolEditTextFaceDetectionCustomMessage",
                getString(R.string.patrolEditTextFaceDetectionCustomMessage)
            )
        )


        faceDetectionPhrase0.setText(
            SettingsRepository.getLangString(
                this,
                "faceDetectionPhrase0",
                getString(R.string.face_recognized_0)
            )
        )
        faceDetectionPhrase1.setText(
            SettingsRepository.getLangString(
                this,
                "faceDetectionPhrase1",
                getString(R.string.face_recognized_1)
            )
        )
        faceDetectionPhrase2.setText(
            SettingsRepository.getLangString(
                this,
                "faceDetectionPhrase2",
                getString(R.string.face_recognized_2)
            )
        )
        faceDetectionPhrase3.setText(
            SettingsRepository.getLangString(
                this,
                "faceDetectionPhrase3",
                getString(R.string.face_recognized_3)
            )
        )
        faceDetectionPhrase4.setText(
            SettingsRepository.getLangString(
                this,
                "faceDetectionPhrase4",
                getString(R.string.face_recognized_4)
            )
        )


        mainActivityButtonArticle1Visible.isChecked =
            SettingsRepository.getBoolean(
                this,
                "mainActivityDisplayMenu",
                resources.getBoolean(R.bool.mainActivityButtonArticle1Visible)
            )

        mainActivityButtonArticle2Visible.isChecked =
            SettingsRepository.getBoolean(
                this,
                "mainActivityDisplaySchedule",
                resources.getBoolean(R.bool.mainActivityButtonArticle2Visible)
            )


        mainActivityButtonArticle3Visible.isChecked =
            SettingsRepository.getBoolean(
                this,
                "mainActivityDisplayVaria",
                resources.getBoolean(R.bool.mainActivityButtonArticle3Visible)
            )

        mainActivityDisplayFaceReg.isChecked =
            SettingsRepository.getBoolean(
                this,
                "mainActivityDisplayFaceReg",
                resources.getBoolean(R.bool.mainActivityDisplayFaceReg)
            )



        mainActivityDisplayButtonVideo.isChecked =
            SettingsRepository.getBoolean(
                this,
                "mainActivityDisplayButtonVideo",
                resources.getBoolean(R.bool.mainActivityDisplayButtonVideo)
            )

        mainActivityDisplayButtonRepose.isChecked =
            SettingsRepository.getBoolean(
                this,
                "mainActivityDisplayButtonRepose",
                resources.getBoolean(R.bool.mainActivityDisplayButtonRepose)
            )

        mainActivityDisplayButtonPatrol.isChecked =
            SettingsRepository.getBoolean(
                this,
                "mainActivityDisplayButtonPatrol",
                resources.getBoolean(R.bool.mainActivityDisplayButtonPatrol)
            )


        activationPhrase.setText(
            SettingsRepository.getLangString(
                this,
                "activationPhrase",
                getString(R.string.activation_phrase)
            )
        )



        robotNameOverride.setText(
            SettingsRepository.getString(
                this,
                "robotNameOverride",
                ""
            )
        )

        mapNameOverride.setText(
            SettingsRepository.getString(
                this,
                "mapNameOverride",
                ""
            )
        )



        personalizeFaceDetectionMessages.isChecked =
            SettingsRepository.getBoolean(
                this,
                "personalizeFaceDetectionMessages",
                resources.getBoolean(R.bool.personalizeFaceDetectionMessages)
            )

        settingsPassword.setText(
            SettingsRepository.getString(
                this,
                "settingsPassword",
                "012345"
            )
        )
    }

    private fun saveValues() {
        SettingsRepository.setBoolean(
            this,
            "patrolCheckBoxSingleRun",
            patrolCheckBoxSingleRun.isChecked
        )
        SettingsRepository.setBoolean(
            this,
            "patrolCheckBoxUseRepeatMessage",
            patrolCheckBoxUseRepeatMessage.isChecked
        )

        SettingsRepository.setInt(
            this,
            "patrolRepeatMessageInterval",
            patrolEditTextRepeatMessageInterval.text.toString().toInt(),
        )
        SettingsRepository.setBoolean(
            this,
            "patrolCheckBoxUseFaceDetection",
            patrolCheckBoxUseFaceDetection.isChecked
        )
        SettingsRepository.setBoolean(
            this,
            "patrolCheckBoxUseArrivalMessage",
            patrolCheckBoxUseArrivalMessage.isChecked
        )
        SettingsRepository.setBoolean(
            this,
            "patrolCheckBoxUsePauseAfterArrival",
            patrolCheckBoxUsePauseAfterArrival.isChecked
        )

        SettingsRepository.setLangString(
            this,
            "patrolEditTextRepeatMessage",
            patrolEditTextRepeatMessage.text.toString()
        )


        SettingsRepository.setLangString(
            this,
            "patrolEditTextArrivalMessage",
            patrolEditTextArrivalMessage.text.toString()
        )
        SettingsRepository.setInt(
            this,
            "patrolArrivalPauseDuration",
            patrolEditTextArrivalPauseDuration.text.toString().toInt()
        )

        SettingsRepository.setBoolean(
            this,
            "patrolCheckBoxUseFaceDetectionCustomMessage",
            patrolCheckBoxUseFaceDetectionCustomMessage.isChecked
        )
        SettingsRepository.setLangString(
            this,
            "patrolEditTextFaceDetectionCustomMessage",
            patrolEditTextFaceDetectionCustomMessage.text.toString()
        )

        SettingsRepository.setLangString(
            this,
            "faceDetectionPhrase0",
            faceDetectionPhrase0.text.toString()
        )
        SettingsRepository.setLangString(
            this,
            "faceDetectionPhrase1",
            faceDetectionPhrase1.text.toString()
        )
        SettingsRepository.setLangString(
            this,
            "faceDetectionPhrase2",
            faceDetectionPhrase2.text.toString()
        )
        SettingsRepository.setLangString(
            this,
            "faceDetectionPhrase3",
            faceDetectionPhrase3.text.toString()
        )
        SettingsRepository.setLangString(
            this,
            "faceDetectionPhrase4",
            faceDetectionPhrase4.text.toString()
        )


        SettingsRepository.setBoolean(
            this,
            "mainActivityDisplayMenu",
            mainActivityButtonArticle1Visible.isChecked
        )
        SettingsRepository.setBoolean(
            this,
            "mainActivityDisplaySchedule",
            mainActivityButtonArticle2Visible.isChecked
        )

        SettingsRepository.setBoolean(
            this,
            "mainActivityDisplayVaria",
            mainActivityButtonArticle3Visible.isChecked
        )


        SettingsRepository.setBoolean(
            this,
            "mainActivityDisplayFaceReg",
            mainActivityDisplayFaceReg.isChecked
        )

        SettingsRepository.setBoolean(
            this,
            "mainActivityDisplayButtonVideo",
            mainActivityDisplayButtonVideo.isChecked
        )

        SettingsRepository.setBoolean(
            this,
            "mainActivityDisplayButtonRepose",
            mainActivityDisplayButtonRepose.isChecked
        )

        SettingsRepository.setBoolean(
            this,
            "mainActivityDisplayButtonPatrol",
            mainActivityDisplayButtonPatrol.isChecked
        )

        SettingsRepository.setLangString(
            this,
            "activationPhrase",
            activationPhrase.text.toString()
        )

        SettingsRepository.setString(
            this,
            "robotNameOverride",
            robotNameOverride.text.toString()
        )

        SettingsRepository.setString(
            this,
            "mapNameOverride",
            mapNameOverride.text.toString()
        )
        SettingsRepository.setBoolean(
            this,
            "personalizeFaceDetectionMessages",
            personalizeFaceDetectionMessages.isChecked
        )

        SettingsRepository.setString(
            this,
            "settingsPassword",
            settingsPassword.text.toString()
        )
    }

    fun settingsButtonReposeClicked(view: View) {
        app.robot.repose()
        app.showLongToast(this, getString(R.string.ReposeInfo))
    }

    fun settingsCloseAppButtonOnClick(view: View) {
        val that = this

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button",
                message = "buttonEndAppOnClick"
            )

            // Stop any running services
            stopService(Intent(that, AsrService::class.java))

            // Clean up app resources
            app.endApp()

            // Remove task from recent apps
            moveTaskToBack(true)

            // Close all activities
            finishAffinity()
            finishAndRemoveTask()
            delay(100)
            android.os.Process.killProcess(android.os.Process.myPid())
            delay(100)
            exitProcess(0)

            //delay(1000L)

            // Force system to kill the process
            //exitProcess(0)
        }
    }


    // sync locations from robot to backend
    // and from backend to robot (get human readable names)
    fun buttonSettingsSyncLocationsOnClick(view: View) {
        syncLocationsToBackend()
    }


    private fun syncLocationsToBackend() {
        applicationScope.launch {
            val res = BackendApiKtorSingleton.syncMapLocationsToBackend2(
                mapIdCode = App.MAP_ID,
                mapName = App.MAP_NAME,
                floors = app.robot.getAllFloors()
            )

            if (res.status.isSuccess()) {
                app.showToast(app, "Locations synced to backend.\n" + res.bodyAsText())
                syncLocationsFromBackend()
            } else {
                app.showLongToast(app, "Locations NOT synced to backend!\n" + res.bodyAsText())
            }
        }
    }

    private suspend fun syncLocationsFromBackend() {
        val res = BackendApiKtorSingleton.syncMapLocationsFromBackend2()
        if (res.isNotEmpty()) {
            app.showToast(app, "Locations synced from backend.")
            saveLocations2(res)
        } else {
            app.showLongToast(app, "Locations NOT synced from backend!")
        }
    }

    private fun saveLocations2(locations: List<MapLocation2Sync>) {
        val jsonStr = Json.encodeToString(locations)
        val appSharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)
        val prefsEditor: SharedPreferences.Editor = appSharedPrefs.edit()
        prefsEditor.putString(App.MAP_ID, jsonStr)
        prefsEditor.commit()

        Log.d(TAG, "Locations saved to shared prefs. ${App.MAP_ID} $jsonStr")
    }

    private fun saveLocations(locations: List<MapLocationSync>) {
        val jsonStr = Json.encodeToString(locations)
        val appSharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)
        val prefsEditor: SharedPreferences.Editor = appSharedPrefs.edit()
        prefsEditor.putString(App.MAP_ID, jsonStr)
        prefsEditor.commit()

        Log.d(TAG, "Locations saved to shared prefs. ${App.MAP_ID} $jsonStr")
    }

    fun settingsButtonRightsOnClick(view: View) {
        //rewrite to get all possible perm
        app.robot.requestPermissions(
            listOf(
                Permission.SEQUENCE,
                Permission.MAP,
                Permission.SETTINGS,
                Permission.FACE_RECOGNITION,
                Permission.MEETINGS
            ), 1
        )

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.button",
                message = "settingsButtonRightsOnClick"
            )
        }
    }

    fun buttonSettingsUpdateOnClick(view: View) {
        val pInfo: PackageInfo =
            this.packageManager.getPackageInfo(this.packageName, 0)

        val that = this

        applicationScope.launch {
            val appVersion = BackendApiKtorSingleton.getLatestAppVersion() ?: return@launch

            Log.d(
                TAG,
                "Current version: ${pInfo.longVersionCode}, backend version ${appVersion.apkVersionCode}"
            )

            if (pInfo.longVersionCode >= appVersion.apkVersionCode) {
                app.showLongToast(
                    app,
                    "Current version ${pInfo.longVersionCode}. No newer version found - backend has ${appVersion.apkVersionCode}"
                )
                return@launch
            }

            app.showLongToast(
                app,
                "Current version ${pInfo.longVersionCode}. Downloading version ${appVersion.apkVersionCode}. Please wait!"
            )


            var fileLocation = that.getExternalFilesDir(null).toString() + "/app.apk"
            Log.d(TAG, "File location: $fileLocation")

            val file = File(fileLocation) //.createTempFile("", ".apk", )
            val fileOutputStream = FileOutputStream(file)

            settingsProgressBarUpdate.progress = 0
            settingsProgressBarUpdate.visibility = View.VISIBLE

            BackendApiKtorSingleton.downloadFileWithProgress(
                appVersion.url,
                fileOutputStream,
            ) { sent, length ->
                settingsProgressBarUpdate.max = length?.toInt() ?: 0
                settingsProgressBarUpdate.progress = sent.toInt()
            }

            fileOutputStream.flush()
            fileOutputStream.close()

            app.showLongToast(
                app,
                "Version ${appVersion.apkVersionCode} downloaded."
            )

            settingsProgressBarUpdate.visibility = View.INVISIBLE
            settingsProgressBarUpdate.progress = 0
            settingsProgressBarUpdate.max = 0


            val intent = Intent(Intent.ACTION_VIEW)
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            intent.setDataAndType(
                FileProvider.getUriForFile(
                    that,
                    "ee.taltech.aireapplication.provider",
                    file
                ), "application/vnd.android.package-archive"
            )

            val oldVmPolicy = StrictMode.getVmPolicy()

            val policy = StrictMode.VmPolicy.Builder()
                .penaltyLog()
                .build()
            StrictMode.setVmPolicy(policy)
            that.startActivity(intent)
            StrictMode.setVmPolicy(oldVmPolicy)
        }
    }

    fun settingsButtonFaceRegOnClick(view: View) {
        startActivity(Intent(this, FaceActivity::class.java))
    }

    fun settingsButtonFloorClicked(view: View) {
        startActivity(Intent(this, FloorActivity::class.java))
    }

}