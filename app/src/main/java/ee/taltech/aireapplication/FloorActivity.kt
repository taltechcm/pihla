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
                    "Change to floor" + " " + adapterSelectedFloor.displayName
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
        app.robot.loadFloor(
            selectedFloor!!.id,
            Position(selectedFloor!!.locations[0].x, selectedFloor!!.locations[0].y)
        )


        showPopup("Please wait, changing floor to: " + selectedFloor!!.name, 6000)

        applicationScope.launch {

            // build a downloop here until floor is changed

            delay(6000L)

            var floor = app.robot.getCurrentFloor()

            if (floor == null) {
                showPopup("No floor!", 4000)
            } else {
                if (floor.name != selectedFloor!!.name) {
                    showPopup("Floor did not change.", 4000)
                } else {
                    showPopup("Floor changed to: " + floor.name, 4000)

                    floorTextViewCurrent.text = floor.name
                    floorTextViewLocations.text = floor.locations.joinToString("\n") { l -> l.name }
                }
            }

        }


    }
}