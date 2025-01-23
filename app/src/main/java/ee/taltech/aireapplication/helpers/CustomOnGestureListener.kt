package ee.taltech.aireapplication.helpers

import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

open class CustomOnGestureListener(private val customGestureHandler: CustomGestureHandler? = null) : GestureDetector.OnGestureListener {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
        private const val VELOCITY_THRESHOLD = 500
    }

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {

    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (abs(velocityX) < VELOCITY_THRESHOLD && abs(velocityY) < VELOCITY_THRESHOLD) {
            // it's a drag
            return false
        }

        var direction = ""

        if (abs(velocityX) > abs(velocityY)) {
            //if velocityX is negative, then it's towards left
            direction = if (velocityX >= 0) {
                "right"
            } else {
                "left"
            }
        } else {
            direction = if (velocityY >= 0) {
                "down"
            } else {
                "up"
            }
        }
        Log.i(TAG, "swipe $direction");

        customGestureHandler?.onSwipeGesture(direction)

        return true;
    }

}