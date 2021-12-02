package de.jepfa.yapm.service.biometrix

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.os.CancellationSignal
import de.jepfa.yapm.R
import javax.crypto.Cipher

@TargetApi(Build.VERSION_CODES.M)
open class BiometricManagerV23(val cipher: Cipher, val context: Context) {


    private var cryptoObject: FingerprintManagerCompat.CryptoObject
    private lateinit var biometricDialogV23: BiometricDialogV23

    init {
        cryptoObject = FingerprintManagerCompat.CryptoObject(cipher)

    }

    fun displayBiometricPromptV23(description: String, biometricCallback: BiometricCallback) {
        val fingerprintManagerCompat = FingerprintManagerCompat.from(context)
        fingerprintManagerCompat.authenticate(cryptoObject, 0, CancellationSignal(),
            object : FingerprintManagerCompat.AuthenticationCallback() {
                override fun onAuthenticationError(errMsgId: Int, errString: CharSequence) {
                    super.onAuthenticationError(errMsgId, errString)
                    updateStatus(errString.toString())
                    biometricCallback.onAuthenticationError(errString.toString())
                }

                override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence) {
                    super.onAuthenticationHelp(helpMsgId, helpString)
                    updateStatus(helpString.toString())
                }

                override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    dismissDialog()
                    biometricCallback.onAuthenticationSuccessful(result.cryptoObject.cipher)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    updateStatus(context.getString(R.string.biometric_failed))
                    biometricCallback.onAuthenticationFailed()
                }
            }, null
        )
        displayBiometricDialog(description, biometricCallback)
    }

    private fun displayBiometricDialog(description: String, biometricCallback: BiometricCallback) {
        biometricDialogV23 = BiometricDialogV23(context, biometricCallback)
        biometricDialogV23.setTitle(context.getString(R.string.auth_with_biometrics))
        biometricDialogV23.setDescription(description)
        biometricDialogV23.show()
    }

    private fun dismissDialog() {
        if (biometricDialogV23 != null) {
            biometricDialogV23.dismiss()
        }
    }

    private fun updateStatus(status: String) {
        if (biometricDialogV23 != null) {
            biometricDialogV23.updateStatus(status)
        }
    }
}