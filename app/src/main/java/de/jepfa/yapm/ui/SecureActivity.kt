package de.jepfa.yapm.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.overlay.OverlayShowingService
import de.jepfa.yapm.ui.login.LoginActivity
import de.jepfa.yapm.util.ClipboardUtil
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import java.util.concurrent.TimeUnit

abstract class SecureActivity : BaseActivity() {

    @Volatile
    protected var checkSession = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkSecret()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkSecret(intent)
    }

    override fun onPause() {
        super.onPause()
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onResume() {
        super.onResume()
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        closeOverlayDialogs()
        checkSecret()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        checkSecret()
        return super.dispatchTouchEvent(ev)
    }

    protected abstract fun lock()

    @get:Synchronized
    val masterSecretKey: SecretKeyHolder?
        get() {
            val session = SecretChecker.getOrAskForSecret(this)
            return if (session.isDenied()) {
                null
            } else {
                session.getMasterKeySK()
            }
        }

    fun closeOverlayDialogs() {
        val intent = Intent(this, OverlayShowingService::class.java)
        stopService(intent)
    }

    @Synchronized
    private fun checkSecret(incomingIntent: Intent? = null) {
        if (checkSession) {
            if (incomingIntent != null) {
                SecretChecker.getOrAskForSecret(this, incomingIntent)
            }
            else {
                SecretChecker.getOrAskForSecret(this)
            }
        }
    }

    /**
     * Helper class to check the user secret.
     */
    object SecretChecker {

        const val fromSecretChecker = "fromSecretChecker"
        const val fromAutofill = "fromAutofill"
        const val fromNotification = "fromNotification"
        const val loginRequestCode = 38632

        private val DELTA_LOGIN_ACTIVITY_INTENDED = TimeUnit.SECONDS.toMillis(1) // TODO not sure whether we still need this

        @Volatile
        private var loginActivityIntended: Long = 0

        @Synchronized
        fun getOrAskForSecret(activity: SecureActivity, incomingIntent: Intent = activity.intent): Session {
            if (Session.isDenied()) {
                // make all not readable by setting key as invalid
                if (Session.isOutdated()) {
                    if (Session.shouldBeLoggedOut()) {
                        Session.logout()
                    }
                    else {
                        Session.lock()
                    }

                    ClipboardUtil.clearClips(activity)
                    activity.closeOverlayDialogs()
                }

                if (!isLoginIntended()) {
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.putExtras(incomingIntent)
                    intent.putExtra(fromSecretChecker, true)
                    intent.action = incomingIntent.action
                    if (incomingIntent.getBooleanExtra(fromAutofill, false)) {
                        activity.startActivityForResult(intent, loginRequestCode)
                        activity.lock()
                    }
                    else {
                        activity.startActivity(intent)
                        activity.finish()  // Why? To restart from the main activity.
                    }

                    loginIntended(activity)
                }
            } else {
                Session.touch()
            }
            return Session
        }

        private fun isLoginIntended(): Boolean {
            val current = System.currentTimeMillis()
            return  loginActivityIntended >= current - DELTA_LOGIN_ACTIVITY_INTENDED
        }

        private fun loginIntended(activity: SecureActivity) {
            loginActivityIntended = System.currentTimeMillis()
            Log.i(LOG_PREFIX + "CS", "login intended at $loginActivityIntended from $activity")
        }
    }
}