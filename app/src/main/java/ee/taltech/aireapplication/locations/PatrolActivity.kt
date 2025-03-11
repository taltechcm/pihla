package ee.taltech.aireapplication.locations

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.face.ContactModel
import com.robotemi.sdk.face.OnFaceRecognizedListener
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToDestinationChangedListener
import com.zeugmasolutions.localehelper.currentLocale
import ee.taltech.aireapplication.App
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.R
import ee.taltech.aireapplication.domain.Location
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BaseActivity
import ee.taltech.aireapplication.helpers.CustomAsrListener
import ee.taltech.aireapplication.helpers.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.text.DecimalFormat

class PatrolActivity : BaseActivity(), OnGoToLocationStatusChangedListener,
    OnDistanceToDestinationChangedListener,
    // OnFaceRecognizedListener,
    CustomAsrListener {

    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        val decimalFormat = DecimalFormat("#.#")
    }

    // UI elements
    private lateinit var patrolButtonReturn: Button
    private lateinit var recyclerView: RecyclerView


    private lateinit var patrolButtonStartStop: Button
    private lateinit var patrolTextViewLocation: TextView
    private lateinit var patrolTextViewLocationLabel: TextView

    private lateinit var adapter: DataRecyclerViewAdapter

    private lateinit var patrolGroupGoingTo: Group


    // variables
    private var patrolLocations: List<Location> = listOf()
    private var patrolActive = false
    private var goingToLocation: Location? = null
    private var goingToLocationIndex: Int = -1
    private var homeBaseLocation: Location? = null

    private var faceDetectionPauseActive = false
    private val faceDetectionPauseDuration = 10 * 1000L
    private var playPatrolRepeatMessageIsActive = false


    private var patrolUseRepeatMessage = false
    private var patrolSingleRun = true
    private var patrolUseArrivalMessage = false
    private var patrolArrivalMessage = "Patrol arrival message"
    private var patrolUsePauseAfterArrival = true
    private var patrolArrivalPauseDuration = 5
    private var patrolUseFaceDetection = true
    private var patrolUseFaceDetectionCustomMessage = true
    private var patrolFaceDetectionCustomMessage = "Face Detection Custom Message"

    private var patrolRepeatMessageInterval = 5
    private var patrolRepeatMessage = "Patrol repeat message"


    // ============================= Android lifecycle =============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patrol)

        decimalFormat.roundingMode = RoundingMode.FLOOR


        // find UI elements, bind to variables
        patrolButtonReturn = findViewById(R.id.patrolButtonReturn)
        recyclerView = findViewById(R.id.gamesRecyclerView)


        patrolButtonStartStop = findViewById(R.id.patrolButtonStartStop)

        patrolGroupGoingTo = findViewById(R.id.patrolGroupGoingTo)
        patrolTextViewLocation = findViewById(R.id.patrolTextViewLocation)
        patrolTextViewLocationLabel = findViewById(R.id.patrolTextViewLocationLabel)
        patrolGroupGoingTo.visibility = View.INVISIBLE



        patrolLocations =
            app.locationsRepository.getPatrolLocations(App.MAP_ID, locale = currentLocale)

        homeBaseLocation =
            app.locationsRepository.getHomeBaseLocation(App.MAP_ID, locale = currentLocale)

        // recyclerview setup
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DataRecyclerViewAdapter(
            context = this,
            dataSet = patrolLocations
        ) { location, idx ->
            run {
                // callback from adapter
                goingToLocation = location
                goingToLocationIndex = idx
                patrolTextViewLocation.text = location.displayName + " (...)"
                patrolGroupGoingTo.visibility = View.VISIBLE
            }
        }
        recyclerView.adapter = adapter

        // load settings
        loadSettings()
        Log.d(TAG, "patrol info. Homebase: ${homeBaseLocation?.displayName ?: "null"}, first location: ${patrolLocations[0].displayName}, patrolSingleRun: $patrolSingleRun")
    }

    private fun loadSettings() {
        patrolUseRepeatMessage = SettingsRepository.getBoolean(
            this,
            "patrolCheckBoxUseRepeatMessage",
            resources.getBoolean(R.bool.patrolCheckBoxUseRepeatMessage)
        )
        patrolSingleRun = SettingsRepository.getBoolean(
            this,
            "patrolCheckBoxSingleRun",
            resources.getBoolean(R.bool.patrolCheckBoxSingleRun)
        )
        patrolUseArrivalMessage = SettingsRepository.getBoolean(
            this,
            "patrolCheckBoxUseArrivalMessage",
            resources.getBoolean(R.bool.patrolCheckBoxUseArrivalMessage)
        )
        patrolArrivalMessage = SettingsRepository.getLangString(
            this,
            "patrolEditTextArrivalMessage",
            getString(R.string.patrolEditTextArrivalMessage)
        )

        patrolUsePauseAfterArrival = SettingsRepository.getBoolean(
            this,
            "patrolCheckBoxUsePauseAfterArrival",
            resources.getBoolean(R.bool.patrolCheckBoxUsePauseAfterArrival)
        )
        patrolArrivalPauseDuration = SettingsRepository.getInt(
            this,
            "patrolArrivalPauseDuration",
            5
        )

        patrolUseFaceDetection = SettingsRepository.getBoolean(
            this,
            "patrolCheckBoxUseFaceDetection",
            resources.getBoolean(R.bool.patrolCheckBoxUseFaceDetection)
        )

        patrolUseFaceDetectionCustomMessage = SettingsRepository.getBoolean(
            this,
            "patrolCheckBoxUseFaceDetectionCustomMessage",
            resources.getBoolean(R.bool.patrolCheckBoxUseFaceDetectionCustomMessage)
        )

        patrolFaceDetectionCustomMessage = SettingsRepository.getLangString(
            this,
            "patrolEditTextFaceDetectionCustomMessage",
            getString(R.string.patrolEditTextFaceDetectionCustomMessage)
        )

        patrolRepeatMessageInterval = SettingsRepository.getInt(
            this,
            "patrolRepeatMessageInterval",
            5
        )


        patrolRepeatMessage = SettingsRepository.getLangString(
            this,
            "patrolEditTextRepeatMessage",
            getString(R.string.patrolEditTextRepeatMessage)
        )
    }

    override fun onPause() {
        super.onPause()

        playPatrolRepeatMessageIsActive = false
        unbindTemiEventListeners()

        app.removeCustomAsrListener(this)
    }

    override fun onResume() {
        super.onResume()
        bindTemiEventListeners()
        refreshTemiUi()

        app.addCustomAsrListener(this)
    }

    // ============================= UI methods =============================

    private fun refreshTemiUi() {
        try {
            val activityInfo = packageManager
                .getActivityInfo(componentName, PackageManager.GET_META_DATA)
            Robot.getInstance().onStart(activityInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    fun patrolButtonReturnOnClick(view: View) {
        closeActivity()
    }


    private fun closeActivity() {
        app.robot.cancelAllTtsRequests()

        patrolButtonReturn.text = getString(R.string.Close)

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.button", message = "closeButtonOnClick"
            )
        }

        finish()
    }

    fun patrolButtonStartStopOnClick(view: View) {
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button",
                message = "patrolButtonStartStopOnClick. Patrol active: $patrolActive"
            )
        }

        startStopPatrol()
    }

    private fun startStopPatrol() {
        if (patrolActive) {
            app.robot.stopMovement()
            patrolGroupGoingTo.visibility = View.INVISIBLE
            goingToLocation = null
            goingToLocationIndex = -1
            playPatrolRepeatMessageIsActive = false
            hideFaceAnimation()

        } else {
            // no starting location was selected from list, start from top
            if (goingToLocation == null) {
                goingToLocationIndex = 0
                goingToLocation = patrolLocations[goingToLocationIndex]
                patrolTextViewLocation.text = goingToLocation!!.displayName + " (...)"
                patrolGroupGoingTo.visibility = View.VISIBLE
            }

            app.robot.goTo(
                location = goingToLocation!!.systemName,
                backwards = false,
                noBypass = null,
                speedLevel = null
            )

            if (patrolUseRepeatMessage) {
                playPatrolRepeatMessageIsActive = true
                playPatrolRepeatMessage()
            }

            showFaceAnimation(patrolTextViewLocation.text.toString())

        }

        patrolButtonStartStop.text =
            if (patrolActive) getString(R.string.start_patrol) else getString(R.string.start_patrol_stop)
        patrolActive = !patrolActive

        // no app level face detection when patrol is active
        //app.faceDetectionDisabled = patrolActive
    }

    // ============================= MISC =============================


    private fun patrolLocationComplete(location: String) {
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.patrolLocationComplete",
                message = "location: $location index: $goingToLocationIndex singlerun: $patrolSingleRun"
            )
        }

        // we arrived at Home, patrol over (it was single run)
        if (goingToLocationIndex < 0) {
            patrolActive = false
            hideFaceAnimation()
            finish()
            return
        }


        goingToLocationIndex++

        // should we loop pack to start or go to Home (last location in list)
        if (goingToLocationIndex >= patrolLocations.size) {
            goingToLocationIndex = if (patrolSingleRun) -1 else 0
        }

        goingToLocation =
            if (goingToLocationIndex == -1) {
                if (homeBaseLocation != null) homeBaseLocation else patrolLocations[0]
            } else patrolLocations[goingToLocationIndex]


        applicationScope.launch {
            if (patrolUseArrivalMessage) {
                app.speak(
                    sentence = patrolArrivalMessage,
                    shown = false
                )
                updateFaceAnimationStatusText(patrolArrivalMessage)
            }

            if (patrolUsePauseAfterArrival) {
                if (patrolArrivalPauseDuration > 0)
                    delay(patrolArrivalPauseDuration * 1000L)
            }

            // goingToLocation might have been set to null during delay (stop was pressed)
            if (goingToLocation != null) {
                updateFaceAnimationStatusText(goingToLocation!!.displayName)
                showFaceAnimation(goingToLocation!!.displayName)
                patrolTextViewLocation.text = goingToLocation!!.displayName + " (...)"

                app.robot.goTo(
                    location = goingToLocation!!.systemName,
                    backwards = false,
                    noBypass = null,
                    speedLevel = null
                )

                applicationScope.launch {
                    BackendApiKtorSingleton.logEvent(
                        tag = "$TAG.robot.goto",
                        message = goingToLocation!!.systemName
                    )
                }
            }
        }

    }


    // ============================= TEMI Event listeners =============================

    private fun bindTemiEventListeners() {
        app.robot.addOnGoToLocationStatusChangedListener(this)
        app.robot.addOnDistanceToDestinationChangedListener(this)
        //app.robot.addOnFaceRecognizedListener(this)
    }

    private fun unbindTemiEventListeners() {
        app.robot.removeOnGoToLocationStatusChangedListener(this)
        app.robot.removeOnDistanceToDestinationChangedListener(this)
        //app.robot.removeOnFaceRecognizedListener(this)
    }

    // https://github.com/robotemi/sdk/wiki/Locations#onGoToLocationStatusChangedListener
    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        Log.d(
            TAG,
            "onGoToLocationStatusChanged. location: $location, status: $status, descriptionId: $descriptionId, description: $description"
        )

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.onGoToLocationStatusChanged",
                message = "location: $location, status: $status, descriptionId: $descriptionId, description: $description"
            )
        }

        when (status) {
            "start" -> {}
            "calculating" -> {}
            "obstacle detected" -> {}
            "going" -> {}
            "abort" -> {
                // why abort? was MAIN/follow me button clicked?
                //patrolLocationComplete(location)
                startStopPatrol()
            }

            "reposing" -> {}
            "complete" -> {
                patrolLocationComplete(location)
            }

            else -> {
                Log.e(TAG, "onGoToLocationStatusChanged. Unhandled status: $status")
            }
        }
    }

    override fun onDistanceToDestinationChanged(location: String, distance: Float) {
        Log.d(TAG, "onDistanceToDestinationChanged. Location: $location, distance: $distance")

        if (goingToLocation != null) {
            patrolTextViewLocation.text =
                "${goingToLocation!!.displayName} (${decimalFormat.format(distance)})"
            updateFaceAnimationStatusText(patrolTextViewLocation.text.toString())
        }
    }


    override fun onFaceRecognized(contactModelList: List<ContactModel>) {
        if (contactModelList.isEmpty() || faceDetectionPauseActive) {
            return
        }

        Log.d(TAG, "onFaceRecognized. contactModelList: $contactModelList")
        // only start person greeting when tts is idle
        if (ttsStatus == TtsRequest.Status.COMPLETED) {
            applicationScope.launch {
                faceDetectionPauseActive = true
                if (patrolUseFaceDetection) {
                    if (patrolUseFaceDetectionCustomMessage) {
                        app.speak(
                            sentence = patrolFaceDetectionCustomMessage,
                            shown = false
                        )
                    } else {
                        app.speak(
                            sentence = getFaceString(contactModelList),
                            shown = false
                        )
                    }
                }
                delay(faceDetectionPauseDuration)
                faceDetectionPauseActive = false
            }
        }
    }

    private fun playPatrolRepeatMessage() {
        if (patrolUseRepeatMessage && playPatrolRepeatMessageIsActive) {

            if (ttsStatus == TtsRequest.Status.COMPLETED) {
                applicationScope.launch {
                    app.speak(
                        sentence = patrolRepeatMessage,
                        shown = false
                    )

                    applicationScope.launch {
                        BackendApiKtorSingleton.logEvent(
                            tag = "${TAG}.playPatrolRepeatMessage",
                            message = "$patrolRepeatMessage delay: $patrolRepeatMessageInterval"
                        )
                    }


                    if (patrolRepeatMessageInterval > 0)
                        delay(patrolRepeatMessageInterval * 1000L)
                    // go into cycle
                    playPatrolRepeatMessage()

                }
            } else {
                applicationScope.launch {

                    if (patrolRepeatMessageInterval > 0)
                        delay(patrolRepeatMessageInterval * 1000L)
                    // go into cycle
                    playPatrolRepeatMessage()
                }
            }

        }
    }

    // tts listener is attached in baseActivity
    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        Log.d(
            TAG,
            "onTtsStatusChanged. Status: ${ttsRequest.status.name}, speech: ${ttsRequest.speech}"
        )
        ttsStatus = ttsRequest.status

    }

    fun patrolActivityOnClick(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }


    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {
        super.onAsrResult(asrResult, sttLanguage)
        val triggers = HashMap<String, List<String>>()

        triggers["start"] = listOf("alusta", "start")
        triggers["close"] = listOf("sulge", "tagasi")
        triggers["stop"] = listOf("peatu", "stop")


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
                startStopPatrol()
            }

            "stop" -> {
                startStopPatrol()
            }

            "close" -> {
                closeActivity()
            }
        }
    }


    override fun onCustomAsrResult(asrResult: String) {
        onAsrResult(asrResult, SttLanguage.ET_EE)
    }


}