package de.jepfa.yapm.service.biometrix

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.annotation.RequiresApi

/*
Taken and modified from https://github.com/FSecureLABS/android-keystore-audit/tree/master/keystorecrypto-app
 */
@RequiresApi(api = Build.VERSION_CODES.P)
class BiometricCallbackV28(private val biometricCallback: BiometricCallback) :
    BiometricPrompt.AuthenticationCallback() {

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        biometricCallback.onAuthenticationSuccessful(result.cryptoObject.cipher)
    }


    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        biometricCallback.onAuthenticationError(errString)
    }

    override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        biometricCallback.onAuthenticationFailed()
    }
}