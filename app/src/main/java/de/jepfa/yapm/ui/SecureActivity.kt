package de.jepfa.yapm.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.ui.login.LoginActivity
import javax.crypto.SecretKey

abstract class SecureActivity : BaseActivity() {
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

    protected abstract fun refresh(before: Boolean)

    @get:Synchronized
    val masterSecretKey: SecretKey?
        public get() {
            val secret = SecretChecker.getOrAskForSecret(this)
            return if (secret.isDeclined()) {
                null
            } else {
                secret.get()
            }
        }

    @Synchronized
    private fun checkSecret() {
        SecretChecker.getOrAskForSecret(this)
    }

    /**
     * Helper class to check the user secret.
     */
    object SecretChecker {

        @Synchronized
        fun getOrAskForSecret(activity: BaseActivity): Secret {
            if (Secret.isLockedOrOutdated()) {
                // make all not readable by setting key as invalid
                Secret.lock()

                val intent = Intent(activity, LoginActivity::class.java)
                activity.startActivity(intent)
            } else {
                Secret.update()
            }
            return Secret
        }
    }
}