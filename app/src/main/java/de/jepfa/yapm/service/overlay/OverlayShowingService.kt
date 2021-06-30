/*
Thanks to BjornQ: https://gist.github.com/bjoernQ/6975256
 */
package de.jepfa.yapm.service.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.View.OnTouchListener
import android.widget.Button
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_PASSWD_WORDS_ON_NL
import de.jepfa.yapm.service.PreferenceService.PREF_TRANSPARENT_OVERLAY
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.PasswordColorizer
import de.jepfa.yapm.util.getEncryptedExtra


class OverlayShowingService : Service(), OnTouchListener {

    private var topView: View? = null
    private var overlayedButton: Button? = null
    private var wm: WindowManager? = null

    private var password = Password.empty()
    private var multiLine = false

    private var offsetX = 0f
    private var offsetY = 0f
    private var originalXPos = 0
    private var originalYPos = 0

    private var moving = false
    private var dropToRemove = false



    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        clearIt()
        val encrypted = intent.getEncryptedExtra(DetachHelper.EXTRA_PASSWD)
        if (encrypted != null && !Session.isDenied()) {
            val masterKeySK = Session.getMasterKeySK()
            if (masterKeySK != null) {
                val transSK = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
                password = SecretService.decryptPassword(transSK, encrypted)

                val multiLineDefault =
                    PreferenceService.getAsBool(PREF_PASSWD_WORDS_ON_NL, this)
                multiLine = intent.getBooleanExtra(DetachHelper.EXTRA_MULTILINE, multiLineDefault)

                paintIt()
                return START_STICKY // STOP_FOREGROUND_REMOVE
            }
        }
        return START_NOT_STICKY

    }

    private fun paintIt() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (wm == null) {
            return
        }

        overlayedButton = Button(this)
        overlayedButton?.apply {
            isAllCaps = false
            setPadding(24, 12, 24, 12)
            val isTransparent = PreferenceService.getAsBool(PREF_TRANSPARENT_OVERLAY, this@OverlayShowingService)
            if (isTransparent) {
                setBackgroundColor(0x77feccff)
            }
            compoundDrawablePadding = 12
        }

        overlayedButton?.setOnTouchListener(this)
        updateContent()

        val LAYOUT_FLAG =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
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
            LAYOUT_FLAG,
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

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                moving = false
                calcOriginalPos()

                offsetX = originalXPos - event.rawX
                offsetY = originalYPos - event.rawY
            }

            MotionEvent.ACTION_MOVE -> {
                val newX = (offsetX + event.rawX).toInt()
                val newY = (offsetY + event.rawY).toInt()
                if (Math.abs(newX - originalXPos) < 1 && Math.abs(newY - originalYPos) < 1 && !moving) {
                    return false
                }

                move(newX, newY)
                checkUpdateAppearance(event)

                moving = true
            }

            MotionEvent.ACTION_UP -> {
                if (dropToRemove) {
                    clearIt()
                    return true
                }
                if (moving) {
                    return true
                }
            }
        }

        return false
    }

    private fun move(newX: Int, newY: Int) {
        val topLocationOnScreen = IntArray(2)
        topView?.getLocationOnScreen(topLocationOnScreen)

        val params = overlayedButton?.layoutParams as WindowManager.LayoutParams
        params.x = newX - topLocationOnScreen[0]
        params.y = newY - topLocationOnScreen[1]
        wm?.updateViewLayout(overlayedButton, params)
    }

    private fun checkUpdateAppearance(event: MotionEvent) {
        if (event.rawY < 50) {
            updateRemove()
            dropToRemove = true
        } else if (dropToRemove) {
            updateContent()
            dropToRemove = false
        }
    }

    private fun calcOriginalPos() {
        val buttonWidth = overlayedButton!!.measuredWidth
        val location = IntArray(2)
        overlayedButton?.getLocationOnScreen(location)
        originalXPos = location[0] + ((buttonWidth) / 2)
        originalYPos = location[1]
    }

    private fun updateContent() {
        overlayedButton?.text = PasswordColorizer.spannableString(password, multiLine, this)
        overlayedButton?.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        overlayedButton?.setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
        calcOriginalPos()
    }
    private fun updateRemove() {
        overlayedButton?.text = getString(R.string.drop_to_reove)
        overlayedButton?.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_baseline_close_12, 0, 0)
        overlayedButton?.typeface = null
        calcOriginalPos()
    }

}