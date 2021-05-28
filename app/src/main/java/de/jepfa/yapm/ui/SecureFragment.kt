package de.jepfa.yapm.ui

import javax.crypto.SecretKey

open class SecureFragment : BaseFragment() {

    fun getSecureActivity() : SecureActivity? {
        return activity as SecureActivity?
    }

    val masterSecretKey: SecretKey?
        get() {
            return getSecureActivity()?.masterSecretKey
        }
}