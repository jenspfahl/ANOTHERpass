package de.jepfa.yapm.service.biometrix

import android.annotation.TargetApi
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import de.jepfa.yapm.R
import de.jepfa.yapm.service.biometrix.BiometricUtils.isBiometricPromptEnabled
import de.jepfa.yapm.service.biometrix.BiometricUtils.isFingerprintAvailable
import de.jepfa.yapm.service.biometrix.BiometricUtils.isHardwareSupported
import de.jepfa.yapm.service.biometrix.BiometricUtils.isPermissionGranted
import javax.crypto.Cipher

class BiometricManager(cipher: Cipher, context: Context): BiometricManagerV23(cipher, context) {

    fun authenticate(description: String, biometricCallback: BiometricCallback) {
        try {
            if (!isPermissionGranted(context)) {
                biometricCallback.onAuthenticationError(context.getString(R.string.biometric_permission_not_granted))
            }
            else if (!isHardwareSupported(context)) {
                biometricCallback.onAuthenticationError(context.getString(R.string.biometric_not_supported))
            }
            else if (!isFingerprintAvailable(context)) {
                biometricCallback.onAuthenticationError(context.getString(R.string.fingerprint_not_enrolled))
            }
            else {
                displayBiometricDialog(description, biometricCallback)
            }
        } catch (e: Exception) {
            Log.e("BIOMgr", "couldn't auth", e)
            biometricCallback.onAuthenticationError(context.getString(R.string.unknown_error))
        }
    }

    private fun displayBiometricDialog(description: String, biometricCallback: BiometricCallback) {
        if (isBiometricPromptEnabled) {
            displayBiometricPrompt(description, biometricCallback)
        } else {
            displayBiometricPromptV23(description, biometricCallback)
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun displayBiometricPrompt(description: String, biometricCallback: BiometricCallback) {
        val crypto = BiometricPrompt.CryptoObject(cipher)
        BiometricPrompt.Builder(context)
            .setTitle(context.getString(R.string.auth_with_biometrics))
            .setDescription(description)
            .setNegativeButton(
                context.getString(android.R.string.cancel),
                context.mainExecutor,
                { _, _ -> biometricCallback.onAuthenticationCancelled() })
            .build()
            .authenticate(
                crypto, CancellationSignal(), context.mainExecutor,
                BiometricCallbackV28(biometricCallback)
            )
    }

}