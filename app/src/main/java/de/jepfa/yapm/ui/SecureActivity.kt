package de.jepfa.yapm.ui

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.overlay.OverlayShowingService
import de.jepfa.yapm.ui.login.LoginActivity
import de.jepfa.yapm.util.ClipboardUtil
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

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
        checkSecret()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        checkSecret()
        return super.dispatchTouchEvent(ev)
    }

    protected abstract fun lock()

    @get:Synchronized
    val masterSecretKey: SecretKey?
        get() {
            val secret = SecretChecker.getOrAskForSecret(this)
            return if (secret.isDenied()) {
                null
            } else {
                secret.getMasterKeySK()
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

        val fromSecretChecker = "fromSecretChecker"
        val fromAutofill = "fromAutofill"
        val loginRequestCode = 38632

        private val DELTA_LOGIN_ACTIVITY_INTENTED = TimeUnit.SECONDS.toMillis(1)

        @Volatile
        private var loginActivityIntented: Long = 0

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

                if (!isLoginIntented()) {
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.putExtras(activity.intent)
                    intent.putExtra(fromSecretChecker, true)
                    if (activity.intent.getBooleanExtra(fromAutofill, false)) {
                        activity.startActivityForResult(intent, loginRequestCode)
                        activity.lock()
                    }
                    else {
                        activity.startActivity(intent)
                        activity.finish()
                    }

                    loginIntented()
                }
            } else {
                Session.touch()
            }
            return Session
        }

        private fun isLoginIntented(): Boolean {
            val current = System.currentTimeMillis()
            return  loginActivityIntented >= current - DELTA_LOGIN_ACTIVITY_INTENTED
        }

        private fun loginIntented() {
            loginActivityIntented = System.currentTimeMillis()
        }
    }
}