package ee.taltech.aireapplication.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BroadcastReceiverInActivity(
    private val activityTag: String,
    private val receiveFn: (intent: Intent?) -> Unit
) : BroadcastReceiver() {

    companion object {
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(
            "$activityTag.BroadcastReceiverInActivity.onReceive",
            intent?.action ?: "null intent"
        )
        receiveFn(intent)
    }
}