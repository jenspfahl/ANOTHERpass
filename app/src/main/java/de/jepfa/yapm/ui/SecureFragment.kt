package de.jepfa.yapm.ui

import de.jepfa.yapm.model.secret.SecretKeyHolder

open class SecureFragment : BaseFragment() {

    fun getSecureActivity() : SecureActivity? {
        return activity as SecureActivity?
    }

    val masterSecretKey: SecretKeyHolder?
        get() {
            return getSecureActivity()?.masterSecretKey
        }
}