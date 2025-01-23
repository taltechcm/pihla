package ee.taltech.aireapplication.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.konovalov.vad.silero.Vad
import com.konovalov.vad.silero.VadSilero
import ee.taltech.aireapplication.MainActivity
import ee.taltech.aireapplication.R
import com.konovalov.vad.silero.config.SampleRate
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
https://github.com/liam-mceneaney/androidwhisper.cpp/tree/master



*/

class AsrService : Service(), VoiceRecorder.AudioCallback, WebSocketListener {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName


        private var speechActive = false

        private val DEFAULT_SAMPLE_RATE = SampleRate.SAMPLE_RATE_16K
        private val DEFAULT_FRAME_SIZE = FrameSize.FRAME_SIZE_512
        private val DEFAULT_MODE = Mode.NORMAL
        private val DEFAULT_SILENCE_DURATION_MS = 300
        private val DEFAULT_SPEECH_DURATION_MS = 50

        private val AUDIO_COLLECTION_MAX_DURATION_MS = 10_000
        private val AUDIO_PAUSE_BEFORE_ASR__MS = 200L


        private val phraseList = listOf(
            "hei",
            "Pille",
            "Olle",
            "robot",
            "temi",
            "hei robot",

            "tagasi",
            "alusta",

            "loe", "uudiseid",
            "uudised",
            "lugemine",
            "peatu",
            "peata",
            "seisa",
            "stop",
            "stopp",

            "järgmine",
            "eelmine",
            "edasi",

            "majajuht",

            "ringisõit",

            "arva", "vanust",
            "analüüsi",

            "mäng",
            "lõbu",
            "meelelahutus",

            "menüü",
            "ajakava",

            "suurem", "suuremaks", "in",
            "väiksem", "väiksemaks", "out",
            "uuesti",
            "varia", "vaaria"
        )


        private const val NOTIFICATION_CHANNEL_ID = "default"


        fun createNotificationChannel(context: Context) {
            val notificationManager =
                context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

            // create the notification channel
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        fun buildNotification(context: Context): Notification {
            return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Pihlakodu")
                .setContentText("Temi is listening to the microphone")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(
                    Intent(
                        context,
                        MainActivity::class.java
                    ).let { notificationIntent ->
                        PendingIntent.getActivity(
                            context,
                            0,
                            notificationIntent,
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    })
                .build()
        }


        var audioBuffer: ArrayList<Short> = ArrayList()
    }

    private lateinit var recorder: VoiceRecorder
    private lateinit var vad: VadSilero

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!speechActive) {
            Log.d(TAG, "onStartCommand, asr active:$speechActive")
            startReco()
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        // https://stackoverflow.com/questions/44425584/context-startforegroundservice-did-not-then-call-service-startforeground

        createNotificationChannel(this)
        ServiceCompat.startForeground(
            this,
            100,
            buildNotification(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        recorder.stop()
        super.onDestroy()

        val nMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nMgr.cancelAll()


    }

    override fun onLowMemory() {
        Log.d(TAG, "onLowMemory")
        super.onLowMemory()
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()

        startForegroundService()
    }

    // ================== text (from speech) processor and notifier =======================

    private val activationPhrase: String by lazy {
        SettingsRepository.getLangString(this, "activationPhrase", "Hei robot")
    }

    private fun handleAsrResult(text: String){
        Log.d(TAG, "RAW SPEECH: '$text'")

        val activationPhraseUpper = activationPhrase.uppercase()
        val startIndex = activationPhraseUpper.length

        val textUpper =
            text.uppercase()
                .replace("EI ", "HEI ")
                .replace("EI! ", "HEI ")
                .replace("EI. ", "HEI ")
                .replace("EI, ", "HEI ")
                .replace("HEI! ", "HEI ")
                .replace("HEI. ", "HEI ")
                .replace("HEI, ", "HEI ")
                .replace("NII! ", "HEI ")
                .replace("NII. ", "HEI ")
                .replace("NII, ", "HEI ")
                .replace("HEA ", "HEI ")
                .replace("HEA! ", "HEI ")
                .replace("HEA. ", "HEI ")
                .replace("HEA, ", "HEI ")

        if (textUpper.contains(activationPhraseUpper)) {
            Log.d(TAG, "$activationPhraseUpper detected in speech. $textUpper")

            val asrIntent = Intent(C.INTENT_ASR)
            asrIntent.putExtra(
                C.INTENT_ASR_TEXT,
                text.substring(startIndex + 1).trim()
            )
            LocalBroadcastManager.getInstance(this).sendBroadcast(asrIntent)
        }

    }

    // ================== speech =======================

    private val asrBackendClient = AsrWebSocketClient(this)

    private fun startReco() {
        Log.d(TAG, "startReco")


        vad = Vad.builder()
            .setContext(this)
            .setSampleRate(DEFAULT_SAMPLE_RATE)
            .setFrameSize(DEFAULT_FRAME_SIZE)
            .setMode(DEFAULT_MODE)
            .setSilenceDurationMs(DEFAULT_SILENCE_DURATION_MS)
            .setSpeechDurationMs(DEFAULT_SPEECH_DURATION_MS)
            .build()

        recorder = VoiceRecorder(this)

        speechActive = true
        recorder.start(vad.sampleRate.value, vad.frameSize.value)
    }

    private var asrConnectionStatus = 0 // 0 not connected, 1 connecting, 2 connected

    /*
    // streaming audio to ASR backend
    override fun onAudio(audioData: ByteArray) { //16 bit signed int
        if (vad.isSpeech(audioData)) {
            GlobalScope.launch {
                if (asrConnectionStatus == 0) {
                    asrConnectionStatus = 1
                    asrBackendClient.connect()
                    while (asrConnectionStatus != 2) {
                        delay(10)
                    }
                }

                if (asrConnectionStatus == 1) {
                    while (asrConnectionStatus != 2) {
                        delay(10)
                    }
                }

                Log.d(TAG, "ASR Connection status: $asrConnectionStatus")
                asrBackendClient.send(audioData)
            }
        }
    }
    */


    // collecting audio data for x seconds or until timeout of y seconds - then calling asr backend
    private var audioBuffer: ArrayList<Byte> = ArrayList()
    private var delayJob: Job? = null

    override fun onAudio(audioData: ByteArray) {

        if (vad.isSpeech(audioData)) {
            audioBuffer.addAll(audioData.toList())

            delayJob?.cancel()

            // start new timer to detect pause in speaking
            delayJob = GlobalScope.launch {
                delay(AUDIO_PAUSE_BEFORE_ASR__MS)
                Log.d(TAG, "onAudio: delay completed")
                sendAudioToAsr()
            }

            if (audioBuffer.size >= (DEFAULT_SAMPLE_RATE.value * 2 * AUDIO_COLLECTION_MAX_DURATION_MS) / 1000) {
                Log.d(TAG, "onAudio: buffer full")
                sendAudioToAsr()
            }
        }
    }

    private fun sendAudioToAsr() {
        var tempAudio: ArrayList<Byte> = ArrayList()
        tempAudio.addAll(audioBuffer)
        audioBuffer.clear()
        Log.d(TAG, "Sending ${tempAudio.size} bytes of raw audio data to backend.")

        GlobalScope.launch {
            var text = asrBackendClient.transcribe(
                tempAudio.toByteArray(),
                hotwords = phraseList.joinToString(", ")
            ) // prompt = "Hei robot, majajuht! Hei robot, ringisõit! Hei robot, uudised! Hei robot, menüü! Hei robot, ajakava!"
            if (!text.isNullOrEmpty()) {
                handleAsrResult(text)
            }
        }

    }

    override fun onConnected() {
        Log.d(TAG, "onConnected")
        asrConnectionStatus = 2
    }

    override fun onMessage(message: String) {
        if (message.isNotEmpty()) {
            Log.d(TAG, "onMessage: $message")
        }
    }

    override fun onDisconnected() {
        Log.d(TAG, "onDisconnected")
        asrConnectionStatus = 0
    }

}