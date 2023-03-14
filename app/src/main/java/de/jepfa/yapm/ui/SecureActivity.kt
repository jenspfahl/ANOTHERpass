package de.jepfa.yapm.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.overlay.OverlayShowingService
import de.jepfa.yapm.ui.login.LoginActivity
import de.jepfa.yapm.util.ClipboardUtil
import java.util.concurrent.TimeUnit

abstract class SecureActivity : BaseActivity() {

    protected var checkSession = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkSecret()
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
    private fun checkSecret() {
        if (checkSession) {
            SecretChecker.getOrAskForSecret(this)
        }
    }

    /**
     * Helper class to check the user secret.
     */
    object SecretChecker {

        const val fromSecretChecker = "fromSecretChecker"
        const val fromAutofillOrNotification = "fromAutofill"
        const val loginRequestCode = 38632

        private val DELTA_LOGIN_ACTIVITY_INTENDED = TimeUnit.SECONDS.toMillis(5)

        @Volatile
        private var loginActivityIntended: Long = 0

        @Synchronized
        fun getOrAskForSecret(activity: SecureActivity): Session {
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
                    intent.putExtras(activity.intent)
                    intent.putExtra(fromSecretChecker, true)
                    if (activity.intent.getBooleanExtra(fromAutofillOrNotification, false)) {
                        activity.startActivityForResult(intent, loginRequestCode)
                        activity.lock()
                    }
                    else {
                        activity.startActivity(intent)
                        activity.finish()
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
            Log.i("CS", "login intended at $loginActivityIntended from $activity")
        }
    }
}