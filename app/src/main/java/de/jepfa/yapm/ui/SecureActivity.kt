package de.jepfa.yapm.ui

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Encrypted.Companion.fromBase64String
import de.jepfa.yapm.model.Key
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService.Secret
import de.jepfa.yapm.util.PreferenceUtil
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
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
        private val DELTA_DIALOG_OPENED = TimeUnit.SECONDS.toMillis(5)
        private const val MAX_PASSWD_ATTEMPTS = 3

        @Volatile
        private var secretDialogOpened: Long = 0

        @Synchronized
        fun getOrAskForSecret(activity: BaseActivity): Secret {
            val secret = activity.getApp().secretService.secret
            if (secret.isLockedOrOutdated()) {
                // make all not readable by setting key as invalid
                secret.lock()
                // open user secret dialog
                openDialog(secret, activity)
            } else {
                secret.update()
            }
            return secret
        }

        private fun openDialog(secret: Secret, activity: BaseActivity) {
            if (isRecentlyOpened(secretDialogOpened)) {
                return
            }
            secretDialogOpened = System.currentTimeMillis()
            if (activity is SecureActivity) {
                activity.refresh(true) // show all data as invalid
            }
            val builder = AlertDialog.Builder(activity)

            // TODO this should be an activity instead of a dialog !!
            // TODO, No, for locking a dialog for logout/login an activity
            val input = EditText(activity)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            input.requestFocus()
            val dialog = builder.setTitle(R.string.title_encryption_pin_required)
                    .setMessage(R.string.message_encrypt_pin_required)
                    .setView(input)
                    .setOnDismissListener { secretDialogOpened = 0 }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.ok, null)
                    .setCancelable(false)
                    .create()
            input.imeOptions = EditorInfo.IME_ACTION_DONE
            input.setOnEditorActionListener { textView, id, keyEvent ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
                true
            }
            dialog.setOnShowListener {
                val failCounter = AtomicInteger()
                val buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                buttonPositive.setOnClickListener(View.OnClickListener {
                    val secretService = activity.getApp().secretService
                    val masterPin = Password.fromEditable(input.text)
                    try {
                        if (masterPin!!.isEmpty()) {
                            input.error = activity.getString(R.string.error_field_required)
                            return@OnClickListener
                        } else if (isPinStored(activity) &&
                                !isPinValid(masterPin, secretService.getOrCreateSalt(activity), activity)) {
                            input.error = activity.getString(R.string.wrong_pin)
                            if (failCounter.incrementAndGet() < MAX_PASSWD_ATTEMPTS) {
                                return@OnClickListener  // try again
                            }
                        } else {
                            val masterPassword: Password
                            masterPassword = if (isPasspraseStored(activity)) {
                                getStoredPassword(activity)
                            } else {
                                getPasswordFromUser(activity)
                            }
                            secretService.login(masterPin, masterPassword, secretService.getOrCreateSalt(activity))
                            if (activity is SecureActivity) {
                                activity.refresh(false) // show correct encrypted data
                            }
                        }
                    } finally {
                        masterPin!!.clear()
                    }
                    secretDialogOpened = 0
                    dialog.dismiss()
                })
            }
            dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            dialog.show()
        }

        fun isPasspraseStored(activity: Activity): Boolean {
            return PreferenceUtil.get(PreferenceUtil.PREF_MASTER_PASSWORD, activity) != null
        }

        fun getStoredPassword(activity: BaseActivity): Password {
            val storedPasswordBase64 = PreferenceUtil.get(PreferenceUtil.PREF_MASTER_PASSWORD, activity)
            val secretService = activity.getApp().secretService
            val androidSecretKey = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_HPIN)
            val storedEncPassword = fromBase64String(storedPasswordBase64!!)
            return secretService.decryptPassword(androidSecretKey, storedEncPassword)
        }

        fun getPasswordFromUser(activity: BaseActivity?): Password {
            //TODO another dialog to input that
            return Password("") // mockup
        }

        fun isPinStored(activity: Activity): Boolean {
            return PreferenceUtil.get(PreferenceUtil.PREF_HASHED_MASTER_PIN, activity) != null
        }

        fun isPinValid(userPin: Password?, salt: Key?, activity: BaseActivity): Boolean {
            val storedPinBase64 = PreferenceUtil.get(PreferenceUtil.PREF_HASHED_MASTER_PIN, activity)
            val secretService = activity.getApp().secretService
            val hashedPin = secretService.hashPassword(userPin!!, salt!!)
            val androidSecretKey = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_HPIN)
            val storedEncPin = fromBase64String(storedPinBase64!!)
            val storedPin = secretService.decryptKey(androidSecretKey, storedEncPin)
            return hashedPin == storedPin
        }

        private fun isRecentlyOpened(secretDialogOpened: Long): Boolean {
            val current = System.currentTimeMillis()
            return secretDialogOpened >= current - DELTA_DIALOG_OPENED
        }

    }
}