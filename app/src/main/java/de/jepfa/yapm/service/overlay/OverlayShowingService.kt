/*
Thanks to BjornQ: https://gist.github.com/bjoernQ/6975256
 */
package de.jepfa.yapm.service.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.*
import android.view.View.OnTouchListener
import android.widget.Button
import androidx.core.content.res.ResourcesCompat
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.util.PasswordColorizer


class OverlayShowingService : Service(), OnTouchListener {
    private var dropToRemove = false
    private var topView: View? = null
    private var overlayedButton: Button? = null
    private var offsetX = 0f
    private var offsetY = 0f
    private var originalXPos = 0
    private var originalYPos = 0
    private var moving = false
    private var wm: WindowManager? = null
    private var password = Password("---")
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        clearIt()
        val data = intent.getCharArrayExtra(DetachHelper.EXTRA_PASSWD)
        if (data.isEmpty()) {
            return START_NOT_STICKY
        }
        password = Password(data)
        paintIt()
        return STOP_FOREGROUND_REMOVE
    }

    private fun paintIt() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (wm == null) {
            return
        }

        overlayedButton = Button(this)
        overlayedButton?.setAllCaps(false)
        overlayedButton?.setPadding(24, 12, 24, 12)
        overlayedButton?.alpha = Math.round(0.50f * 255).toFloat()
        overlayedButton?.setBackgroundColor(0x77fecc44)
        overlayedButton?.compoundDrawablePadding = 12

        updateContent()

        overlayedButton?.setOnTouchListener(this)

        overlayedButton?.setTypeface(Typeface.MONOSPACE)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER or Gravity.TOP
        params.x = 0
        params.y = 0

        wm?.addView(overlayedButton, params)
        topView = View(this)
        val topParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        topParams.gravity = Gravity.CENTER or Gravity.TOP
        topParams.x = 0
        topParams.y = 0
        topParams.width = 0
        topParams.height = 0
        wm?.addView(topView, topParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearIt()
    }

    private fun clearIt() {
        if (overlayedButton != null) {
            wm?.removeView(overlayedButton)
            wm?.removeView(topView)
            overlayedButton = null
            topView = null
        }
        password.clear()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.rawX
            val y = event.rawY
            moving = false
            val location = IntArray(2)
            overlayedButton?.getLocationOnScreen(location)
            originalXPos = location[0]
            originalYPos = location[1]
            offsetX = originalXPos - x
            offsetY = originalYPos - y
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            val topLocationOnScreen = IntArray(2)
            topView?.getLocationOnScreen(topLocationOnScreen)
            val x = event.rawX
            val y = event.rawY
            val params = overlayedButton?.layoutParams as WindowManager.LayoutParams

            val displayMetrics = DisplayMetrics()
            wm?.defaultDisplay?.getMetrics(displayMetrics)
            val screenWidth: Int = displayMetrics.widthPixels

            val buttonWidth = overlayedButton!!.measuredWidth
            val padding = displayMetrics.density * 24
            val newX = (offsetX + x + (screenWidth - buttonWidth)/2 + padding).toInt()
            val newY = (offsetY + y).toInt()
            if (Math.abs(newX - originalXPos) < 1 && Math.abs(newY - originalYPos) < 1 && !moving) {
                return false
            }
            params.x = newX - topLocationOnScreen[0]
            params.y = newY - topLocationOnScreen[1]
            wm?.updateViewLayout(overlayedButton, params)

            if (y < 50) {
                updateRemove()
                dropToRemove = true
            }
            else if (dropToRemove) {
                updateContent()
                dropToRemove = false
            }
            moving = true
        } else if (event.action == MotionEvent.ACTION_UP) {
            if (dropToRemove) {
                clearIt()
                return true
            }
            if (moving) {
                return true
            }

        }
        return false
    }

    private fun updateContent() {
        overlayedButton?.text = PasswordColorizer.spannableString(password, this)
        overlayedButton?.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }
    private fun updateRemove() {
        overlayedButton?.text = "DROP TO REMOVE"
        overlayedButton?.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_baseline_close_12, 0, 0)
    }

}