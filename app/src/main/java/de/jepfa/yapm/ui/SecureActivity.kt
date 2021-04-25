package de.jepfa.yapm.ui

import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.view.autofill.AutofillManager
import androidx.core.app.ActivityCompat.finishAffinity
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.overlay.OverlayShowingService
import de.jepfa.yapm.ui.login.LoginActivity
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

        private val DELTA_LOGIN_ACTIVITY_INTENTED = TimeUnit.SECONDS.toMillis(3)

        @Volatile
        private var loginActivityIntented: Long = 0

        @Synchronized
        fun getOrAskForSecret(activity: SecureActivity): Session {
            if (Session.isDenied()) {
                // make all not readable by setting key as invalid
                if (Session.isOutdated()) {
                    if (Session.shouldBeLoggedOut())
                        Session.logout()
                    else
                        Session.lock()

                    activity.closeOverlayDialogs()
                    finishAffinity(activity)
                }


                if (!isLoginIntented()) {
                    val intent = Intent(activity, LoginActivity::class.java)
                    val assistStructure = activity.intent.getParcelableExtra<AssistStructure>(
                        AutofillManager.EXTRA_ASSIST_STRUCTURE
                    )
                    if (assistStructure != null) {
                        intent.putExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE, assistStructure)
                    }
                    activity.startActivity(intent)
                    loginIntented()
                    activity.lock()
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