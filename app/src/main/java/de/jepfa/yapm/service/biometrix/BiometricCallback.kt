package de.jepfa.yapm.service.biometrix

import javax.crypto.Cipher

// Based on https://github.com/anitaa1990/Biometric-Auth-Sample
interface BiometricCallback {
    fun onAuthenticationFailed()
    fun onAuthenticationCancelled()
    fun onAuthenticationSuccessful(result: Cipher?)
    fun onAuthenticationError(errString: CharSequence)
}