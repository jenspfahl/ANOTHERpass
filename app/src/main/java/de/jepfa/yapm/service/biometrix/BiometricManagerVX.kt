package de.jepfa.yapm.service.biometrix

import android.annotation.TargetApi
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import de.jepfa.yapm.R
import de.jepfa.yapm.service.biometrix.BiometricUtils.hasBiometricsEnrolled
import de.jepfa.yapm.service.biometrix.BiometricUtils.isBiometricPromptAvailable
import de.jepfa.yapm.service.biometrix.BiometricUtils.isBiometricsAvailable
import de.jepfa.yapm.service.biometrix.BiometricUtils.isPermissionGranted
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import javax.crypto.Cipher

/*
Taken and modified from https://github.com/FSecureLABS/android-keystore-audit/tree/master/keystorecrypto-app
 */
class BiometricManagerVX(cipher: Cipher, context: Context): BiometricManagerV23(cipher, context) {

    fun authenticate(description: String, cancelText: String, biometricCallback: BiometricCallback) {
        try {
            if (!isPermissionGranted(context)) {
                biometricCallback.onAuthenticationError(context.getString(R.string.biometric_permission_not_granted))
            }
            else if (!isBiometricsAvailable(context)) {
                biometricCallback.onAuthenticationError(context.getString(R.string.biometric_not_supported))
            }
            else if (!hasBiometricsEnrolled(context)) {
                biometricCallback.onAuthenticationError(context.getString(R.string.fingerprint_not_enrolled))
            }
            else {
                displayBiometricDialog(description, cancelText, biometricCallback)
            }
        } catch (e: Exception) {
            Log.e(LOG_PREFIX + "BIOMgr", "couldn't auth", e)
            biometricCallback.onAuthenticationError(context.getString(R.string.unknown_error))
        }
    }

    private fun displayBiometricDialog(
        description: String,
        cancelText: String,
        biometricCallback: BiometricCallback
    ) {
        if (isBiometricPromptAvailable) {
            displayBiometricPrompt(description, cancelText, biometricCallback)
        } else {
            displayBiometricPromptV23(description, cancelText, biometricCallback)
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun displayBiometricPrompt(
        description: String,
        cancelText: String,
        biometricCallback: BiometricCallback
    ) {
        val crypto = BiometricPrompt.CryptoObject(cipher)
        BiometricPrompt.Builder(context)
            .setTitle(context.getString(R.string.auth_with_biometrics))
            .setDescription(description)
            .setNegativeButton(
                cancelText,
                context.mainExecutor,
                { _, _ -> biometricCallback.onAuthenticationCancelled() })
            .build()
            .authenticate(
                crypto, CancellationSignal(), context.mainExecutor,
                BiometricCallbackV28(biometricCallback)
            )
    }

}