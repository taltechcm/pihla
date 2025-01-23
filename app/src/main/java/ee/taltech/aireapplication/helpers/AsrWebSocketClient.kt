package ee.taltech.aireapplication.helpers

import android.util.Log
import ee.taltech.aireapplication.dto.AsrResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.sse.*
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow

/*
Live audio transcription via WS /v1/audio/transcriptions endpoint.
LocalAgreement2 (paper | original implementation) algorithm is used for live transcription.
Only transcription of a single channel, 16000 sample rate, raw, 16-bit little-endian audio is supported.
*/

class AsrWebSocketClient(private val listener: WebSocketListener) {
    companion object {
        private val TAG = AsrWebSocketClient::class.java.simpleName

        private const val RECONNECT_DELAY = 5_000L
        private const val PING_INTERVAL = 1_000L
        private const val url = "https://whisper.ai.akaver.com/v1/audio/transcriptions"
        private const val wsUrl =
            "ws://whisper.ai.akaver.com:8000/v1/audio/transcriptions?language=et&response_format=text"

    }


    private val client = HttpClient(CIO) {
        /* //okhttp
        engine {
            preconfigured = OkHttpClient.Builder()
                .pingInterval(PING_INTERVAL, TimeUnit.MILLISECONDS)
                .build()
        }
        */
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
            pingIntervalMillis = PING_INTERVAL
        }
        install(SSE)
        //install(Logging)
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }

    private val scope =
        CoroutineScope(Dispatchers.IO)
    /*
    + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
            Log.d(
                TAG,
                "Error: ${throwable.message}"
            )
        }
    */

    private var job: Job? = null

    private var session: WebSocketSession? = null


    fun isConnected(): Boolean {
        return session != null
    }

    suspend fun connect() {
        if (session != null) return;

        try {
            Log.d(
                TAG,
                "Connecting to websocket at $wsUrl..."
            )

            session = client.webSocketSession(wsUrl)


            listener.onConnected()

            Log.i(
                TAG,
                "Connected to websocket at $wsUrl"
            )

            session!!.incoming
                .receiveAsFlow()
                .filterIsInstance<Frame.Text>()
                .filterNotNull()
                .collect { data ->
                    val message = data.readText()
                    listener.onMessage(message)

                    Log.i(
                        TAG,
                        "Received message: $message"
                    )
                }

            Log.d(
                TAG,
                "after session"
            )

        } catch (e: Exception) {
            listener.onDisconnected()

            Log.d(
                TAG,
                "Error: ${e.message}",
            )
            // reconnect()
        }

        stop()

        listener.onDisconnected()

        Log.d(
            TAG,
            "after try and stop"
        )

    }

    private fun reconnect() {
        job?.cancel()

        Log.d(
            TAG,
            "Reconnecting to websocket in $RECONNECT_DELAY ms..."
        )

        job = scope.launch {
            stop()
            delay(RECONNECT_DELAY)
            connect()
        }
    }

    suspend fun stop() {
        Log.d(
            TAG,
            "Closing websocket session..."
        )

        session?.close()
        session = null
    }

    suspend fun send(content: ByteArray) {
        Log.d(
            TAG,
            "Sending content: ${content.size} bytes"
        )

        session?.send(content)
    }

    private fun audioHeaders(filename: String) = Headers.build {
        append(HttpHeaders.ContentDisposition, "filename=$filename")
    }

    suspend fun transcribe(
        audioBytes: ByteArray,
        hotwords: String? = null,
        prompt: String? = null
    ): String? {
        try {

            var finalBytes: ArrayList<Byte> = ArrayList(44 + audioBytes.size)
            finalBytes.addAll(
                AudioUtils.wavHeader(totalAudioLen = audioBytes.size.toLong())!!.toList()
            )
            finalBytes.addAll(audioBytes.toList())


            val response: HttpResponse =
                client.submitFormWithBinaryData(url = url, formData = formData {
                    append("file", finalBytes.toByteArray(), audioHeaders("audio.wav"))
                    append("response_format", "json")
                    append("language", "et")
                    if (hotwords != null) append("hotwords", hotwords)
                    if (prompt != null) append("prompt", prompt)
                })
            // json response format
            // {"text":"ja kõik kõik kõik kõik"}

            if (!response.status.isSuccess()) {
                Log.e(TAG, "transcribe response: " + response.bodyAsText())
            } else {
                Log.d(TAG, "transcribe response: " + response.bodyAsText())
                var res: AsrResponse = response.body()
                return res.text
            }
            return null
        } catch (cause: Throwable) {
            Log.e(TAG, cause.toString())
        }
        return null
    }


    // https://medium.com/@ttdevelopment/building-a-real-time-client-with-ktor-websockets-ce51655f4a4c
    // https://medium.com/@dugguRK/unleashing-websocket-in-android-b82c887b0a27
    /*
    fun connect(listener: WebSocketListener) {
        GlobalScope.launch {

            client.wss("ws://whisper.ai.akaver.com:8000/v1/audio/transcriptions") { // ws://

                listener.onConnected(this)

                try {
                    for (frame in incoming) {

                        Log.d(TAG, "incoming: $frame")
                        if (frame is Frame.Text) {
                            listener.onMessage(frame.readText())
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Exception: $e")

                    listener.onDisconnected()

                }

            }
        }
    }


    fun disconnect() {
        client.close()
    }
    */

}