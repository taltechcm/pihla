package ee.taltech.aireapplication.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
import android.util.Log

class VolumeChangeReceiver : BroadcastReceiver() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
        const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        var am: AudioManager? = null
        //private var callCount = 0
    }

    init {
        Log.d(TAG, "init")
    }


    override fun onReceive(context: Context?, intent: Intent?) {
        if (am == null) {
            if (context == null) {
                Log.w(TAG, "Context null, can't fetch audiomanager!")
                return
            }
            am = context!!.getSystemService(AUDIO_SERVICE) as AudioManager
        }

        if (intent!!.action == VOLUME_CHANGED_ACTION && am != null) {
            //if (callCount % 3 == 0) {
            // should we keep fixed volume?
            if (!SettingsRepository.getBoolean(context!!, "fixAudioVolume", false)) {
                return
            }

            var fixedVolumeLevel = SettingsRepository.getInt(context!!, "fixAudioVolumeAudioLevel", 5)

            am!!.setStreamVolume(AudioManager.STREAM_MUSIC, fixedVolumeLevel, FLAG_REMOVE_SOUND_AND_VIBRATE)
            val currentVolume = am!!.getStreamVolume(AudioManager.STREAM_MUSIC)
            Log.d(TAG, "Volume: $currentVolume")
        }
    }

}