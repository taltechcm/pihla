package ee.taltech.aireapplication.locations

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToDestinationChangedListener
import com.zeugmasolutions.localehelper.currentLocale
import ee.taltech.aireapplication.App
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.NewsActivity
import ee.taltech.aireapplication.NewsActivity.Companion
import ee.taltech.aireapplication.R
import ee.taltech.aireapplication.domain.Location
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BaseActivity
import ee.taltech.aireapplication.helpers.C
import ee.taltech.aireapplication.helpers.CustomAsrListener
import ee.taltech.aireapplication.helpers.SettingsRepository
import ee.taltech.aireapplication.webview.WebViewActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.floor


class LocationsActivity : BaseActivity(), OnGoToLocationStatusChangedListener,
    OnDistanceToDestinationChangedListener, CustomAsrListener {

    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
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

    private lateinit var locationsButtonBack: Button
    private lateinit var locationsButtonStop: Button
    private lateinit var locationsButtonStartNavigation: Button
    private lateinit var buttonCancelReturnHome: Button

    private lateinit var locationTextViewDistanceToGo: TextView

    private lateinit var locationsTextViewLocation: TextView

    private lateinit var adapter: DataRecyclerViewAdapter
    private lateinit var recyclerView: RecyclerView

    private var myLocation: String? = null
    private var isGoing = false
    private var timeToNavigation = C.NAVIGATION_WAIT_TIME

    private var stopMode = true

    private var currentLocation = ""
    private var currentLocationText = ""

    private var returnHomeAfterArrival = true
    private var returnHomeAfterArrivalDelay = 20
    private var returnHomeAfterArrivalDelayCounter = 20

    private var returnHomeAfterArrivalCanceled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locations)

        locationsButtonBack = findViewById(R.id.locationsButtonReturn)
        locationsButtonStop = findViewById(R.id.locationsButtonStop)
        locationsButtonStartNavigation = findViewById(R.id.locationsButtonStartNavigation)
        buttonCancelReturnHome = findViewById(R.id.buttonCancelReturnHome)
        buttonCancelReturnHome.isVisible = false

        locationsTextViewLocation = findViewById(R.id.locationsTextViewNavigatingToLocation)
        locationsTextViewLocation.text = ""

        locationTextViewDistanceToGo = findViewById(R.id.locationTextViewDistanceToGo)
        locationTextViewDistanceToGo.visibility = View.INVISIBLE

        recyclerView = findViewById(R.id.locationRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DataRecyclerViewAdapter(
            this,
            app.locationsRepository!!.getMapLocations(
                App.MAP_ID,
                app.robot.getCurrentFloor()!!.name,
                locale = currentLocale
            )
            //app.robot.locations ?: listOf("None Yet")//names
        ) { location, _ ->
            run {
                currentLocation = location.systemName
                currentLocationText = location.displayName
                myLocation = location.systemName

                locationsButtonStop.isEnabled = false
                locationsButtonStop.isVisible = false

                returnHomeAfterArrivalCanceled = true
                buttonCancelReturnHome.isVisible = false

                returnHomeAfterArrivalDelayCounter = returnHomeAfterArrivalDelay

                updateLocationInfo(location)
            }
        }


        //updateLocationInfo(locations["$location"]) }
        //lowercase

        recyclerView.adapter = adapter

        app.robot.addOnGoToLocationStatusChangedListener(this)
        app.robot.addOnDistanceToDestinationChangedListener(this)


        locationsButtonStop.isEnabled = false
        locationsButtonStop.isVisible = false


        returnHomeAfterArrival = SettingsRepository.getBoolean(
            this,
            "gotoLocationCheckboxReturnHome",
            resources.getBoolean(R.bool.gotoLocationCheckboxReturnHome)
        )
        returnHomeAfterArrivalDelay = SettingsRepository.getInt(
            this,
            "gotoLocationReturnHomeDelay",
            Integer.parseInt(getString(R.string.gotoLocationReturnHomeDelay))
        )


    }

    override fun onResume() {
        super.onResume()
        refreshTemiUi()
        app.addCustomAsrListener(this)
    }

    override fun onPause() {
        super.onPause()
        app.removeCustomAsrListener(this)
    }

    override fun onDestroy() {
        app.robot.removeOnGoToLocationStatusChangedListener(this)
        app.robot.removeOnDistanceToDestinationChangedListener(this)


        super.onDestroy()
    }

    fun locationsButtonBackOnClick(view: View) {
        closeActivity()
    }

    private fun closeActivity() {
        app.robot.cancelAllTtsRequests()

        locationsButtonBack.text = getString(R.string.Close)

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.button", message = "closeButtonOnClick"
            )
        }

        finish()
    }

    fun locationsButtonStopOnClick(view: View) {

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.button",
                message = "locationsButtonBackOnClick"
            )
        }


        isGoing = false
        app.speak(getString(R.string.stop), false)
        locationsButtonBack.isEnabled = true

        if (stopMode) {
            app.robot.stopMovement()
            locationsButtonStop.text = getString(R.string.start)
            stopMode = false
            locationsButtonBack.isEnabled = true

        } else {
            startNavigation()
            locationsButtonStop.text = getString(R.string.stop)
            locationsButtonBack.isEnabled = false
            stopMode = true
        }
    }

    private fun updateLocationInfo(location: Location) {
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.updateLocationInfo",
                message = location.systemName + "(" + location.displayName + ")"
            )
        }

        locationsTextViewLocation.text = location.displayName


        timeToNavigation = C.NAVIGATION_WAIT_TIME

        locationsButtonStartNavigation.text =
            getString(R.string.start_now)// + " (" + timeToNavigation + ")"

        locationsButtonStartNavigation.visibility = View.VISIBLE


    }


    // ROBOT LISTENERS
    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        when (status) {
            "start" -> {
                if (!isGoing) {
                    isGoing = true
                    locationsButtonBack.isEnabled = false
                    app.speak(getString(R.string.LocationFollow), false)
                    //app.robot.playSequence("6548d74ba8889eeee997e8c8", false)

                    applicationScope.launch {
                        BackendApiKtorSingleton.logEvent(
                            tag = "${TAG}.onGoToLocationStatusChanged",
                            message = "start: $location"
                        )
                    }

                }
            }

            "calculating" -> {
                applicationScope.launch {
                    BackendApiKtorSingleton.logEvent(
                        tag = "${TAG}.onGoToLocationStatusChanged",
                        message = "calculating: $location"
                    )
                }

            }

            "going" -> {
                applicationScope.launch {
                    BackendApiKtorSingleton.logEvent(
                        tag = "${TAG}.onGoToLocationStatusChanged",
                        message = "going: $location"
                    )
                }

            }

            "complete" -> {
                applicationScope.launch {
                    delay(500L)
                    val arrivalTextPrefix = getString(R.string.LocationArrived)
                    app.speak("$arrivalTextPrefix $currentLocationText", false)
                    delay(500L)
                    //app.robot.playSequence("6544ef69393e54494c495761")
                }

                applicationScope.launch {
                    BackendApiKtorSingleton.logEvent(
                        tag = "${TAG}.onGoToLocationStatusChanged",
                        message = "complete: $currentLocation"
                    )
                }

                locationsTextViewLocation.text = ""
                myLocation = null
                isGoing = false
                //app.robot.constraintBeWith()
                locationsButtonBack.isEnabled = true
                locationsButtonStop.isEnabled = false
                locationsButtonStop.isVisible = false
                locationTextViewDistanceToGo.visibility = View.INVISIBLE

                // TODO: Shall we finish here? Or pause and go home? Or stay in activity?

                // start timer to return to home
                // time from settings. two buttons: cancel and start immediately

                if (returnHomeAfterArrival && currentLocation.lowercase() != "home base") {

                    returnHomeAfterArrivalDelayCounter = returnHomeAfterArrivalDelay
                    buttonCancelReturnHome.text = getString(R.string.returning_home_cancel).replace(
                        "%s%",
                        returnHomeAfterArrivalDelayCounter.toString()
                    )

                    buttonCancelReturnHome.isVisible = true
                    returnHomeAfterArrivalCanceled = false

                    returnHomeDelayer()
                }
            }

            "abort" -> {
                applicationScope.launch {
                    BackendApiKtorSingleton.logEvent(
                        tag = "${TAG}.onGoToLocationStatusChanged",
                        message = "abort: $location"
                    )
                }

                locationsButtonBack.isEnabled = true
            }

            "reposing" -> {
                applicationScope.launch {
                    BackendApiKtorSingleton.logEvent(
                        tag = "${TAG}.onGoToLocationStatusChanged",
                        message = "reposing: $location"
                    )
                }

            }
        }
    }

    private fun returnHomeDelayer() {
        if (returnHomeAfterArrivalDelayCounter <= 0) return

        applicationScope.launch {

            delay(1000L)
            returnHomeAfterArrivalDelayCounter--
            buttonCancelReturnHome.text = getString(R.string.returning_home_cancel).replace(
                "%s%",
                returnHomeAfterArrivalDelayCounter.toString()
            )


            if (returnHomeAfterArrivalDelayCounter <= 0 && !returnHomeAfterArrivalCanceled) {

                val homeLocation = app.locationsRepository!!.getHomeBaseLocation(
                    App.MAP_ID,
                    app.robot.getCurrentFloor()!!.name,
                    locale = currentLocale
                )

                buttonCancelReturnHome.isVisible = false

                currentLocation = homeLocation!!.systemName
                currentLocationText = homeLocation.displayName
                myLocation = homeLocation.systemName
                updateLocationInfo(homeLocation)
                returnHomeAfterArrivalCanceled = false

                startNavigation()
                locationsButtonStop.text = getString(R.string.stop)
                locationsButtonBack.isEnabled = false
                locationsButtonStop.isEnabled = true
                locationsButtonStop.isVisible = true
                stopMode = true

            } else {
                if (!returnHomeAfterArrivalCanceled) {
                    returnHomeDelayer()
                }
            }
        }
    }

    fun buttonCancelReturnHomeClicked(view: View) {
        returnHomeAfterArrivalCanceled = true
        buttonCancelReturnHome.isVisible = false
    }


    fun locationsButtonStartNavigationOnClick(view: View) {
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.button",
                message = "locationsButtonStartNavigationOnClick $myLocation"
            )
        }

        startNavigation()
        locationsButtonStop.isEnabled = true
        locationsButtonStop.isVisible = true
        stopMode = true
        locationsButtonStop.text = getString(R.string.stop)
    }

    private fun startNavigation() {
        locationsButtonStartNavigation.visibility = View.INVISIBLE

        if (myLocation != null) {
            app.robot.goTo(
                location = myLocation!!,
                backwards = true,
                noBypass = null,
                speedLevel = null
            )
            locationTextViewDistanceToGo.visibility = View.VISIBLE
        }
    }


    override fun onDistanceToDestinationChanged(location: String, distance: Float) {
        locationTextViewDistanceToGo.text =
            getString(R.string.Distance) + " " + (floor(distance).toInt()).toString()
    }


    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {
        super.onAsrResult(asrResult, sttLanguage)

        val triggers = HashMap<String, List<String>>()

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

        app.robot.finishConversation()
    }

    override fun onCustomAsrResult(asrResult: String) {
        onAsrResult(asrResult, SttLanguage.ET_EE)
    }


}