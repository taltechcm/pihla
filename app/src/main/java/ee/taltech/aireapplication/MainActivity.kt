package ee.taltech.aireapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.permission.Permission
import ee.taltech.aireapplication.helpers.BaseActivity
import ee.taltech.aireapplication.helpers.C
import ee.taltech.aireapplication.locations.LocationsActivity
import ee.taltech.aireapplication.webview.WebViewActivity
import com.zeugmasolutions.localehelper.Locales
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.face.FaceActivity
import ee.taltech.aireapplication.games.GamesActivity
import ee.taltech.aireapplication.helpers.AsrService
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.CustomAsrListener
import ee.taltech.aireapplication.helpers.SettingsRepository
import ee.taltech.aireapplication.locations.PatrolActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


//test declaring robot instead of repeated use of app.robot

class MainActivity : BaseActivity(), CustomAsrListener {

    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var buttonArticle1: Button
    private lateinit var buttonArticle2: Button
    private lateinit var buttonArticle3: Button

    private lateinit var buttonRegisterFace: Button
    private lateinit var buttonVideo: Button
    private lateinit var reposeButton: Button

    // call in onResume and onRobotReady
    private fun refreshTemiUi() {
        try {
            val activityInfo = packageManager
                .getActivityInfo(componentName, PackageManager.GET_META_DATA)
            Robot.getInstance().onStart(activityInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }


    // app lifecycles
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "create")


        checkAllAndroidAndRobotPrivileges()

        buttonArticle1 = findViewById(R.id.buttonArticle1)
        buttonArticle2 = findViewById(R.id.buttonArticle2)
        buttonArticle3 = findViewById(R.id.buttonArticle3)

        buttonRegisterFace = findViewById(R.id.buttonRegisterFace)
        buttonVideo = findViewById(R.id.buttonVideo)
        reposeButton = findViewById(R.id.reposeButton)
    }


    private fun startAsrService() {
        if (permissionsGranted(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.RECORD_AUDIO
            )
        ) {
            // do not start service immediately, rights might be still missing
            val serviceIntent = Intent(this, AsrService::class.java)
            startForegroundService(serviceIntent)
        } else {
            Log.w(TAG, "Asr service not started, permissions are missing!")
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d(TAG, "resume")

        refreshTemiUi()

        if (app.createOnce) {
            app.createOnce = false
            app.startApp()

            // https://stackoverflow.com/questions/45225531/set-locale-during-app-startup
            // this is causing MainActivity restart
            // updateLocale(Locales.Estonian)
        }

        app.addCustomAsrListener(this)




        buttonArticle1.visibility = if (SettingsRepository.getBoolean(
                this,
                "mainActivityDisplayMenu",
                resources.getBoolean(R.bool.mainActivityButtonArticle1Visible)
            )
        ) View.VISIBLE else View.INVISIBLE

        buttonArticle2.visibility = if (SettingsRepository.getBoolean(
                this,
                "mainActivityDisplaySchedule",
                resources.getBoolean(R.bool.mainActivityButtonArticle2Visible)
            )
        ) View.VISIBLE else View.INVISIBLE


        buttonArticle3.visibility = if (SettingsRepository.getBoolean(
                this,
                "mainActivityButtonArticle3",
                resources.getBoolean(R.bool.mainActivityButtonArticle3Visible)
            )
        ) View.VISIBLE else View.INVISIBLE

        buttonRegisterFace.visibility = if (SettingsRepository.getBoolean(
                this,
                "mainActivityDisplayFaceReg",
                resources.getBoolean(R.bool.mainActivityDisplayFaceReg)
            )
        ) View.VISIBLE else View.INVISIBLE


        buttonVideo.visibility = if (SettingsRepository.getBoolean(
                this,
                "mainActivityDisplayButtonVideo",
                resources.getBoolean(R.bool.mainActivityDisplayButtonVideo)
            )
        ) View.VISIBLE else View.INVISIBLE

        reposeButton.visibility = if (SettingsRepository.getBoolean(
                this,
                "mainActivityDisplayButtonRepose",
                resources.getBoolean(R.bool.mainActivityDisplayButtonRepose)
            )
        ) View.VISIBLE else View.INVISIBLE



        app.faceDetectionDisabled = false
    }


    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        app.removeCustomAsrListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")

        app.removeCustomAsrListener(this)

        //val serviceIntent = Intent(this, AsrService::class.java)
        //stopService(serviceIntent)
    }


    // custom functions


    fun buttonGuideOnClick(view: View) {
        val intent = Intent(this, LocationsActivity::class.java)
        startActivity(intent)

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button",
                message = "buttonGuideOnClick"
            )
        }

        applicationScope.launch {
            delay(500L)
            app.speak(getString(R.string.Guide), false)

        }
    }

    fun buttonEEOnClick(view: View) {
        updateLocale(Locales.Estonian)

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button",
                message = "buttonEEOnClick"
            )
        }
    }

    fun buttonENOnClick(view: View) {
        updateLocale(Locales.English)
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button",
                message = "buttonENOnClick"
            )
        }
    }

    fun buttonRUOnClick(view: View) {}

    /*
    fun buttonGame2OnClick(view: View) {
        startActivityWebView("https://sisuloome.e-koolikott.ee/node/23250")

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button",
                message = "buttonGame2OnClick"
            )
        }

        applicationScope.launch {
            delay(500L)
            app.speak(getString(R.string.FlowerGameN), false)

        }
    }
     */

    fun buttonGamesOnClick(view: View) {
        startActivity(Intent(this, GamesActivity::class.java))
    }

    fun buttonPatrolOnClick(view: View) {
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button",
                message = "buttonPatrolOnClick"
            )
        }

        val intent = Intent(this, PatrolActivity::class.java)
        startActivity(intent)

        applicationScope.launch {
            delay(500L)
            app.speak(getString(R.string.Patrol), false)

        }
    }

    fun buttonVideoOnClick(view: View) {
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button",
                message = "buttonVideoOnClick"
            )
        }

        val intent = Intent(this, IFrameActivity::class.java)
        val html =
            "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/INO7_MeU394?si=hSrIzoLBTAQdE4tL\" title=\"Title\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen sandbox=\"allow-forms allow-scripts allow-pointer-lock allow-same-origin allow-top-navigation\"></iframe>"

        intent.putExtra(C.EXTRA_WEBVIEW_LAUNCH_URL, html)
        startActivity(intent)

        applicationScope.launch {
            delay(500L)
            app.speak(getString(R.string.VideoN), false)

        }
    }

    fun buttonGame1OnClick(view: View) {

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button",
                message = "buttonGame1OnClick"
            )
        }

        val intent = Intent(this, AnalyzeActivity::class.java)
        startActivity(intent)

        applicationScope.launch {
            delay(500L)
            app.speak(getString(R.string.Game1), false)

        }
    }

    private fun startActivityWebView(url: String, showInput: Boolean = false) {
        val intent = Intent(this, WebViewActivity::class.java)

        intent.putExtra(C.EXTRA_WEBVIEW_LAUNCH_URL, url)
        intent.putExtra(C.EXTRA_WEBVIEW_SHOW_INPUT, showInput)

        Log.d(TAG, "Launching webview to: $url")
        startActivity(intent)
    }

    fun reposeButtonOnClick(view: View) {

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button",
                message = "reposeButtonOnClick"
            )
        }
        app.robot.repose()
        app.showLongToast(this, getString(R.string.ReposeInfo))
    }

    fun buttonNewsOnClick(view: View) {
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button",
                message = "buttonNewsOnClick"
            )
        }

        val intent = Intent(this, NewsActivity::class.java)
        startActivity(intent)

        applicationScope.launch {
            delay(500L)
            app.speak(getString(R.string.News), false)

        }

    }


    private fun launchActivity(
        cls: Class<*>,
        speakStr: String?,
        logTAG: String,
        logMessage: String,
        intentExtraName: String? = null,
        intentExtraValue: String? = null
    ) {
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = logTAG,
                message = logMessage
            )
        }

        val intent = Intent(this, cls)
        if (intentExtraName != null && intentExtraValue != null) {
            intent.putExtra(intentExtraName, intentExtraValue)
        }
        startActivity(intent)

        if (speakStr != null) {
            applicationScope.launch {
                delay(500L)
                app.speak(speakStr, false)

            }
        }

    }

    // =============================== voice commands ==========================

    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {

        val triggers = HashMap<String, List<String>>()

        triggers["locations"] = listOf("juht", "maja", "juhi")
        triggers["patrol"] = listOf("ringi", "sõit", "patrull")
        triggers["news"] = listOf("uudis", "loe")

        //triggers["age"] = listOf("arva", "vanus")
        triggers["game"] = listOf("meele", "mäng", "lahutus", "lõnu")


        triggers["menu"] = listOf("menüü")
        triggers["schedule"] = listOf("ajakava", "kava")
        triggers["varia"] = listOf("varia", "vaaria")

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
            "locations" -> launchActivity(
                cls = LocationsActivity::class.java,
                speakStr = getString(R.string.Guide),
                logTAG = "$TAG.onAsrResult",
                logMessage = "launch ${LocationsActivity.javaClass.simpleName}"
            )

            "patrol" -> launchActivity(
                cls = PatrolActivity::class.java,
                speakStr = getString(R.string.Patrol),
                logTAG = "$TAG.onAsrResult",
                logMessage = "launch ${PatrolActivity.javaClass.simpleName}"
            )

            "news" -> launchActivity(
                cls = NewsActivity::class.java,
                speakStr = getString(R.string.News),
                logTAG = "$TAG.onAsrResult",
                logMessage = "launch ${NewsActivity.javaClass.simpleName}"
            )

            /*
            "age" -> launchActivity(
                cls = AnalyzeActivity::class.java,
                speakStr = getString(R.string.Game1),
                logTAG = "$TAG.onAsrResult",
                logMessage = "launch AnalyzeActivity"
            )
            */

            "menu" ->
                if (buttonArticle1.visibility == View.VISIBLE) {
                    launchActivity(
                        cls = ArticleActivity::class.java,
                        speakStr = null,
                        logTAG = "$TAG.onAsrResult",
                        logMessage = "launch Menu",
                        intentExtraName = "article",
                        intentExtraValue = "menu"
                    )
                }

            "schedule" -> if (buttonArticle2.visibility == View.VISIBLE) {
                launchActivity(
                    cls = ArticleActivity::class.java,
                    speakStr = null,
                    logTAG = "$TAG.onAsrResult",
                    logMessage = "launch Schedule",
                    intentExtraName = "article",
                    intentExtraValue = "schedule"
                )
            }


            "varia" -> if (buttonArticle3.visibility == View.VISIBLE) {
                launchActivity(
                    cls = ArticleActivity::class.java,
                    speakStr = null,
                    logTAG = "$TAG.onAsrResult",
                    logMessage = "launch Varia",
                    intentExtraName = "article",
                    intentExtraValue = "varia"
                )
            }

            "game" ->
                launchActivity(
                    cls = GamesActivity::class.java,
                    speakStr = getString(R.string.FlowerGameN),
                    logTAG = "$TAG.onAsrResult",
                    logMessage = "launch Game",
                    intentExtraName = null,
                    intentExtraValue = null
                )
        }

        app.robot.finishConversation()
    }

    fun imageButtonSettings(view: View) {
        launchActivity(
            SettingsActivity::class.java,
            speakStr = null,
            logTAG = "$TAG.button",
            logMessage = "launch SettingsActivity"
        )
    }

    override fun onCustomAsrResult(asrResult: String) {
        onAsrResult(asrResult, SttLanguage.ET_EE)
    }

    fun buttonArticle1OnClick(view: View) {

        launchActivity(
            ArticleActivity::class.java,
            speakStr = null,
            logTAG = "$TAG.button",
            logMessage = "launch ArticleActivity",
            intentExtraName = "article",
            intentExtraValue = "menu"
        )
    }

    fun buttonArticle2OnClick(view: View) {
        launchActivity(
            ArticleActivity::class.java,
            speakStr = null,
            logTAG = "$TAG.button",
            logMessage = "launch ArticleActivity",
            intentExtraName = "article",
            intentExtraValue = "schedule"
        )
    }

    fun buttonArticle3OnClick(view: View) {
        launchActivity(
            ArticleActivity::class.java,
            speakStr = null,
            logTAG = "$TAG.button",
            logMessage = "launch ArticleActivity",
            intentExtraName = "article",
            intentExtraValue = "varia"
        )
    }

    fun buttonRegisterFaceOnClick(view: View) {
        launchActivity(
            FaceActivity::class.java,
            speakStr = null,
            logTAG = "$TAG.button",
            logMessage = "launch FaceActivity",
        )
    }


    private fun checkAllAndroidAndRobotPrivileges() {
        checkInstallPrivilege()
        val permissionsNeeded = ArrayList<String>()
        permissionsNeeded.addAll(retrievePermissionsFromManifest(applicationContext))
        if (!App.IS_REAL_TEMI) {
            permissionsNeeded.remove("android.permission.REQUEST_INSTALL_PACKAGES")
            permissionsNeeded.remove("android.permission.READ_EXTERNAL_STORAGE")
            permissionsNeeded.remove("android.permission.WRITE_EXTERNAL_STORAGE")
        }

        handleAppPermissions(permissionsNeeded)
        checkRobotPermissions()
    }


    private fun permissionsGranted(vararg permissions: String): Boolean {
        permissions.forEach { permission ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }


    // ============================= check robot permissions ===============================
    private fun checkRobotPermissions() {
        val robotPermissions = listOf(
            Permission.SEQUENCE,
            Permission.MAP,
            Permission.SETTINGS,
            Permission.FACE_RECOGNITION,
            Permission.MEETINGS
        )

        val missingRobotPermissions =
            robotPermissions.filter { permission -> app.robot.checkSelfPermission(permission) == Permission.DENIED }
        if (missingRobotPermissions.isNotEmpty()) {
            Log.w(TAG, "Requesting missing robot permissions. $missingRobotPermissions")
            app.robot.requestPermissions(
                missingRobotPermissions, 1234
            )
        }
    }

    // ============================= check app install right ===============================

    private val unknownAppSourceDialog: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                Log.d("TAG", "Unknown app source was granted")
            } else {
                Log.w("TAG", "Unknown app source not granted")
                app.showLongToast(
                    this,
                    "Unknown app source not granted. Needed for internal updates!"
                )
            }
        }

    private fun checkInstallPrivilege() {
        if (!app.packageManager.canRequestPackageInstalls()) {
            Log.w(TAG, "canRequestPackageInstalls false")
            val unknownAppSourceIntent = Intent()
                .setAction(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse(String.format("package:%s", packageName)))
            unknownAppSourceDialog.launch(unknownAppSourceIntent)
        } else {
            Log.d(TAG, "canRequestPackageInstalls true, no action needed")
        }
    }

    // ============================= check android privileges ===============================

    private val requestPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { isGrantedMap: Map<String, Boolean> ->
            isGrantedMap.forEach { (permission: String, isGranted: Boolean) ->
                if (isGranted) {
                    Log.d(TAG, "Permission '$permission' was granted by the user")
                } else {
                    Log.w(TAG, "Permission '$permission' was DENIED by the user")
                    app.showLongToast(
                        this,
                        "Permission '$permission' is needed for app work. Please allow."
                    )
                }
            }
            startAsrService()
        }

    private fun retrievePermissionsFromManifest(context: Context): Array<String> {
        return try {
            applicationContext
                .packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions!!
        } catch (e: PackageManager.NameNotFoundException) {
            emptyArray<String>()
        }
    }


    private fun handleAppPermissions(permissions: Collection<String>) {
        val missingPermissions = arrayListOf<String>()
        permissions.forEach { permission ->
            when {
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // You can already use the API that requires the permission. Attach locationListener etc.
                    Log.d(TAG, "Permission '$permission' is already granted")
                }

                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, permission
                ) -> {
                    Log.w(TAG, "Permission rationale is needed for: '$permission'")
                    showAndHandlePermissionRationale(permission)
                }

                else -> {
                    Log.w(TAG, "Permission '$permission' not yet granted")
                    missingPermissions.add(permission)
                }
            }
        }


        // filter and request all the missing permissions
        if (missingPermissions.isNotEmpty()) {
            val missingPermissionsToAskFor =
                missingPermissions.filter { permission -> shouldAksForPermission(permission) }
            if (missingPermissionsToAskFor.isNotEmpty()) {
                Log.d(TAG, "Requesting missing permissions")
                requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
            } else {
                startAsrService()
            }
        } else {
            startAsrService()
        }
    }

    private fun shouldAksForPermission(permission: String): Boolean {
        if (!permission.startsWith("android.permission.")) return false

        try {
            val permissionInfo =
                packageManager.getPermissionInfo(permission, PackageManager.GET_META_DATA)

            val levelStr = when (permissionInfo.protection) {
                PermissionInfo.PROTECTION_NORMAL -> "PROTECTION_NORMAL"
                PermissionInfo.PROTECTION_DANGEROUS -> "PROTECTION_DANGEROUS"
                PermissionInfo.PROTECTION_INTERNAL -> "PROTECTION_INTERNAL"
                PermissionInfo.PROTECTION_SIGNATURE -> "PROTECTION_SIGNATURE"
                else -> {
                    "unknown - ${permissionInfo.protection}"
                }
            }
            if (permissionInfo.protection == PermissionInfo.PROTECTION_DANGEROUS) return true
            Log.w(TAG, "$permission - $levelStr. Not asking for user grant!")
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(
                TAG,
                "Permission $permission is not found in package manager! Not asking for user grant!"
            )
        }

        return false
    }

    private fun showAndHandlePermissionRationale(permission: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setTitle("Permission required")
            .setMessage("Permission '$permission' is required for app to work!")
            .setPositiveButton("OK, grant") { _, _ ->
                requestPermissionsLauncher.launch(arrayOf(permission))
            }
            .setNegativeButton("NO") { _, _ ->
                Toast.makeText(
                    this,
                    "Cannot work without '$permission' permission!",
                    Toast.LENGTH_LONG
                ).show()
            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

}

/* discussion

private protected public??
what is the cost of a listener?
how to make the app more energy efficient?
services? broadcasts? where to use? eventbus?
where to keep robot logic?
where to store robot logic?
c#? c++?

test detectdata vs facerecog difference, plurality, working in anonymous greet mode
custom greet mode vs keeping interaction on to exit a conversation // turning greet on off takes 10 seconds
"Overriding the onRobotReady method allows your app to be placed as a shortcut in temi's top bar. For example:"
is this createable? https://github.com/robotemi/sdk/blob/449a909e28ba54906b9b709d1e452ab91da23552/docs/sdk/com.robotemi.sdk/-robot/show-normal-notification.md?plain=1


wiki    https://github.com/robotemi/sdk/wiki
sample  https://github.com/robotemi/sdk/blob/master/sample/src/main/java/com/robotemi/sdk/sample/MainActivity.kt
robot api   https://github.com/robotemi/sdk/blob/master/docs/sdk/com.robotemi.sdk/-robot/index.md
technical temi guide    https://readthedocs.org/projects/temi-guide/ https://github.com/hapi-robo/temi-guide?tab=readme-ov-file
 */
