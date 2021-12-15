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
import android.text.SpannableStringBuilder
import android.view.*
import android.view.View.OnTouchListener
import android.widget.Button
import de.jepfa.yapm.R
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_OVERLAY_SHOW_USER
import de.jepfa.yapm.service.PreferenceService.PREF_OVERLAY_SIZE
import de.jepfa.yapm.service.PreferenceService.PREF_TRANSPARENT_OVERLAY
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.PasswordColorizer
import de.jepfa.yapm.util.getEncryptedExtra
import android.app.ActivityManager.RunningTaskInfo

import android.app.Activity

import android.app.ActivityManager
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import de.jepfa.yapm.service.PreferenceService.PREF_OVERLAY_CLOSE_ALL


class OverlayShowingService : Service(), OnTouchListener {

    private var topView: View? = null
    private var overlayedButton: Button? = null
    private var wm: WindowManager? = null

    private var user = ""
    private var password = Password.empty()
    private var presentationMode = Password.FormattingStyle.DEFAULT
    private var multiLine = false

    private var offsetX = 0f
    private var offsetY = 0f
    private var originalXPos = 0
    private var originalYPos = 0

    private var moving = false
    private var dropToRemove = false
    private var dropToGoBack = false



    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        clearIt()
        multiLine = PreferenceService.getAsBool(PreferenceService.PREF_PASSWD_WORDS_ON_NL, this)
        val encryptedPasswd = intent.getEncryptedExtra(DetachHelper.EXTRA_PASSWD)
        if (encryptedPasswd != null && !Session.isDenied()) {
            val masterKeySK = Session.getMasterKeySK()
            if (masterKeySK != null) {
                val transSK = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, applicationContext)
                password = SecretService.decryptPassword(transSK, encryptedPasswd)

                val encryptedUser = intent.getEncryptedExtra(DetachHelper.EXTRA_USER)
                if (encryptedUser != null) {
                    user = SecretService.decryptCommonString(transSK, encryptedUser)
                }
                else {
                    user = ""
                }

                val formatted = PreferenceService.getAsBool(PreferenceService.PREF_PASSWD_SHOW_FORMATTED, this)
                val multiLine = PreferenceService.getAsBool(PreferenceService.PREF_PASSWD_WORDS_ON_NL, this)
                val presentationModeIdx = intent.getIntExtra(DetachHelper.EXTRA_PRESENTATION_MODE, -1)
                if (presentationModeIdx == -1) {
                    presentationMode = Password.FormattingStyle.createFromFlags(multiLine, formatted)
                }
                else {
                    presentationMode = Password.FormattingStyle.values()[presentationModeIdx]
                }

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
                alpha = 0.7f
            }
            compoundDrawablePadding = 12
            setPadding(24)

            val overlaySize = PreferenceService.getAsInt(PREF_OVERLAY_SIZE, applicationContext)
            textSize = overlaySize.toFloat()
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
                    val closeAll = PreferenceService.getAsBool(PREF_OVERLAY_CLOSE_ALL, applicationContext)
                    if (closeAll) {
                        Session.logout()
                        closeAll()
                    }
                    return true
                }
                if (dropToGoBack) {
                    bringToFront()
                    return true
                }
                if (moving) {
                    return true
                }
                else {
                    presentationMode = if (multiLine) presentationMode.prev()
                    else presentationMode.next()
                    updateContent()
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
        } else if (event.rawX < 50) {
            updateBack()
            dropToGoBack = true
        } else if (dropToRemove || dropToGoBack) {
            updateContent()
            dropToRemove = false
            dropToGoBack = false
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
        val showUser = PreferenceService.getAsBool(PREF_OVERLAY_SHOW_USER, applicationContext)
        if (showUser && user.isNotBlank()) {
            overlayedButton?.text = SpannableStringBuilder(user)
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append(PasswordColorizer.spannableString(password, presentationMode, this))
        }
        else {
            overlayedButton?.text =
                PasswordColorizer.spannableString(password, presentationMode, this)

        }
        overlayedButton?.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.ic_launcher_round,0, 0)
        overlayedButton?.setTypeface(Typeface.MONOSPACE, Typeface.BOLD)

        calcOriginalPos()
    }

    private fun updateRemove() {
        val closeAll = PreferenceService.getAsBool(PREF_OVERLAY_CLOSE_ALL, applicationContext)
        if (closeAll) {
            overlayedButton?.text = getString(R.string.drop_to_close)
        }
        else {
            overlayedButton?.text = getString(R.string.drop_to_remove)
        }
        overlayedButton?.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_baseline_close_12, 0, 0)
        overlayedButton?.typeface = null
        calcOriginalPos()
    }

    private fun updateBack() {
        overlayedButton?.text = getString(R.string.drop_to_go_back)
        overlayedButton?.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_baseline_arrow_back_24, 0, 0)
        overlayedButton?.typeface = null
        calcOriginalPos()
    }

    private fun closeAll() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        am.appTasks.first().finishAndRemoveTask()

    }

    private fun bringToFront() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        am.appTasks.first().moveToFront()

    }

}