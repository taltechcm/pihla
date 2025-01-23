package ee.taltech.aireapplication.games

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.robotemi.sdk.SttLanguage
import ee.taltech.aireapplication.AnalyzeActivity
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.MainActivity
import ee.taltech.aireapplication.MainActivity.Companion
import ee.taltech.aireapplication.R
import ee.taltech.aireapplication.dto.WebLink
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BaseActivity
import ee.taltech.aireapplication.helpers.C
import ee.taltech.aireapplication.helpers.CustomAsrListener
import ee.taltech.aireapplication.webview.WebViewActivity
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GamesActivity : BaseActivity(), CustomAsrListener {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var buttonClose: Button
    private lateinit var gamesRecyclerView: RecyclerView
    private lateinit var adapter: GamesDataRecyclerViewAdapter

    private val flowerGameWebLink = WebLink(
        id = "",
        uri = "",
        isIframe = false,
        zoomFactor = null,
        textZoom = null,

        loadWithOverviewMode = true,
        useWideViewPort = true,
        layoutAlgorithm = "TEXT_AUTOSIZING",
        builtInZoomControls = true,
        displayZoomControls = true,

        webLinkName = "",
        webLinkDisplayName = "Arva vanust"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)

        buttonClose = findViewById(R.id.buttonCloseGames)
        gamesRecyclerView = findViewById(R.id.gamesRecyclerView)

        gamesRecyclerView.setLayoutManager(GridLayoutManager(this, 2))


        adapter = GamesDataRecyclerViewAdapter(
            context = this,
            dataSet = listOf(
                flowerGameWebLink
            )
        ) { webLink, idx ->
            launchWebGame(webLink)
        }
        gamesRecyclerView.adapter = adapter
    }


    override fun onResume() {
        super.onResume()

        app.addCustomAsrListener(this)

        applicationScope.launch {

            app.showToast(applicationContext, "Loading games...")

            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.onResume"
            )

            try {
                val data = BackendApiKtorSingleton.getWebLinks("Game")

                // fixed record for flower game
                data.add(0, flowerGameWebLink)

                Log.d(TAG, data.toString())
                if (data.isEmpty()) {
                    app.showLongToast(applicationContext, "No games found")
                }

                adapter.dataSet = data
                adapter.notifyDataSetChanged();

            } catch (e: Exception) {
                app.showToast(applicationContext, "Error fetching games. " + e.message)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        app.removeCustomAsrListener(this)
    }


    fun buttonCloseGamesOnClick(view: View) {
        closeActivity()
    }

    private fun closeActivity() {
        app.robot.cancelAllTtsRequests()

        buttonClose.text = getString(R.string.Close)

        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.button", message = "closeButtonOnClick"
            )
        }

        finish()
    }


    private fun launchWebGame(webLink: WebLink) {
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "${TAG}.launchWebGame", message = webLink.webLinkName
            )
        }


        if (webLink.id == "") {
            val intent = Intent(this, AnalyzeActivity::class.java)

            startActivity(intent)

            return
        }



        val intent = Intent(this, WebLinkActivity::class.java)
        intent.putExtra(C.EXTRA_GAMEVIEW_SETTINGS, Json.encodeToString(webLink))
        startActivity(intent)
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
                app.robot.cancelAllTtsRequests()

                applicationScope.launch {
                    BackendApiKtorSingleton.logEvent(
                        tag = "${TAG}.onAsrResult",
                        message = "closing activity"
                    )
                }
                finish()
            }
        }

        Log.d(TAG, "onAsrResult. trigger: '$trigger', result: $asrResult");
        if (trigger == "close") app.robot.finishConversation()
    }

    override fun onCustomAsrResult(asrResult: String) {
        onAsrResult(asrResult, SttLanguage.ET_EE)
    }

}