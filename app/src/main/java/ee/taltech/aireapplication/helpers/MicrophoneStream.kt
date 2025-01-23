package ee.taltech.aireapplication.helpers


import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

//import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat
//import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback

// Configures the microphone with 16 kHz sample rate, 16 bit samples, mono (single-channel).
class MicrophoneStream : PullAudioInputStreamCallback() {
    /*
    val format: AudioStreamFormat =
        AudioStreamFormat.getWaveFormatPCM(SAMPLE_RATE.toLong(), 16.toShort(), 1.toShort())
    */

    private var recorder: AudioRecord? = null

    init {
        this.initMic()
    }

    override fun read(bytes: ByteArray): Int {
        if (recorder != null) {
            val ret = recorder!!.read(bytes, 0, bytes.size)
            return ret
        }
        return 0
    }

    override fun close() {
        if (recorder == null) return

        recorder!!.release()
        recorder = null
    }

    @SuppressLint("MissingPermission")
    private fun initMic() {
        // Note: currently, the Speech SDK support 16 kHz sample rate, 16 bit samples, mono (single-channel) only.
        val af = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        recorder = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(af)
            .build()

        recorder!!.startRecording()
    }

    companion object {
        private const val SAMPLE_RATE = 16000
        fun create(): MicrophoneStream {
            return MicrophoneStream()
        }
    }
}