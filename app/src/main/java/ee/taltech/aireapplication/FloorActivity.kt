package ee.taltech.aireapplication

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.robotemi.sdk.map.Floor
import com.robotemi.sdk.navigation.model.Position
import com.zeugmasolutions.localehelper.currentLocale
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.NewsActivity.Companion
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BaseActivity
import ee.taltech.aireapplication.helpers.ObjectDataRecyclerViewAdapter
import ee.taltech.aireapplication.helpers.TObjectDisplayName
import ee.taltech.aireapplication.locations.DataRecyclerViewAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FloorActivity : BaseActivity() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var buttonCloseFloor: Button
    private lateinit var floorRecyclerView: RecyclerView
    private lateinit var floorTextViewCurrent: TextView
    private lateinit var floorTextViewLocations: TextView
    private lateinit var textViewFloorChange: TextView
    private lateinit var floorButtonActivate: Button
    private lateinit var adapter: ObjectDataRecyclerViewAdapter<FloorDisplayItem>
    private var selectedFloor: Floor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floor)

        buttonCloseFloor = findViewById(R.id.buttonCloseFloor)
        floorRecyclerView = findViewById(R.id.floorRecyclerView)
        floorTextViewCurrent = findViewById(R.id.floorTextViewCurrent)
        floorTextViewLocations = findViewById(R.id.floorTextViewLocations)
        floorButtonActivate = findViewById(R.id.floorButtonActivate)
        textViewFloorChange = findViewById(R.id.textViewFloorChange)


        textViewFloorChange.text = ""


        floorRecyclerView = findViewById(R.id.floorRecyclerView)
        floorRecyclerView.layoutManager = LinearLayoutManager(this)

        val floors =
            app.robot.getAllFloors().map { f -> FloorDisplayItem(displayName = f.name, floor = f) }
        adapter = ObjectDataRecyclerViewAdapter(
            this,
            floors
        ) { adapterSelectedFloor, _ ->
            run {
                floorButtonActivate.isEnabled = true
                floorButtonActivate.text =
                    "Change to floor " + adapterSelectedFloor.displayName
                floorTextViewLocations.text =
                    adapterSelectedFloor.floor.locations.joinToString("\n") { l -> l.name }
                selectedFloor = adapterSelectedFloor.floor
            }
        }

        floorRecyclerView.adapter = adapter
    }


    fun buttonCloseFloorClicked(view: View) {
        closeActivity()
    }


    private fun closeActivity() {
        app.robot.cancelAllTtsRequests()

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.button",
                message = "buttonCloseFloorClicked"
            )
        }

        buttonCloseFloor.text = getString(R.string.Close)
        finish()
    }

    override fun onResume() {
        super.onResume()
        var floor = app.robot.getCurrentFloor()

        if (floor != null) {
            floorTextViewCurrent.text = floor.name
            floorTextViewLocations.text = floor.locations.joinToString("\n") { l -> l.name }
        }

    }

    data class FloorDisplayItem(
        override var displayName: String,
        val floor: Floor,
    ) : TObjectDisplayName

    fun floorButtonActivateClicked(view: View) {

        // try to find the "Lift" location from selected floor to use as initial position after floor change

        var location = selectedFloor!!.locations.find { l -> l.name.uppercase() == "LIFT" }

        if (location == null) {
            app.showToast(
                this,
                "Location LIFT not found on selected floor, using first location instead"
            )
            location = selectedFloor!!.locations[0]
        } else {
            app.showLongToast(
                this,
                "Using LIFT location as initial position on this floor. x:" + location.x + " y:" + location.y + " yaw:" + location.yaw
            )
        }

        app.robot.loadFloor(
            selectedFloor!!.id,
            Position(location.x, location.y, location.yaw)
        )

        var wait = 20
        val delay = 1000L

        applicationScope.launch {

            do {
                textViewFloorChange.text =
                    "Please wait, changing floor to: " + selectedFloor!!.name + " (" + wait + ")" + " loc:" + location!!.name + " x:" + location!!.x + " y:" + +location!!.y + " yaw:" + +location!!.yaw
                delay(delay)
                wait--
            } while (app.robot.getCurrentFloor()!!.id != selectedFloor!!.id && wait > 0)
            textViewFloorChange.text = ""


            val floor = app.robot.getCurrentFloor()
            if (floor == null) {
                showPopup("No floor!", 4000)
            } else {
                if (floor.name != selectedFloor!!.name) {
                    showPopup("Floor did not change.", 4000)
                } else {
                    showPopup(
                        "Floor changed to: " + floor.name + " pos: " + app.robot.getPosition(),
                        10000
                    )

                    floorTextViewCurrent.text = floor.name
                    floorTextViewLocations.text = floor.locations.joinToString("\n") { l -> l.name }
                    floorButtonActivate.isEnabled = false
                    floorButtonActivate.text = "Change to floor..."
                }
            }

        }


    }

    fun settingsButtonReposeClicked(view: View) {
        app.robot.repose()
        app.showLongToast(this, getString(R.string.ReposeInfo))
    }
}