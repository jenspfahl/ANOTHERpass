package de.jepfa.yapm.service.biometrix

import javax.crypto.Cipher

/*
Taken and modified from https://github.com/FSecureLABS/android-keystore-audit/tree/master/keystorecrypto-app
 */
interface BiometricCallback {
    fun onAuthenticationCancelled()
    fun onAuthenticationSuccessful(result: Cipher?)
    fun onAuthenticationError(errString: CharSequence?)
}