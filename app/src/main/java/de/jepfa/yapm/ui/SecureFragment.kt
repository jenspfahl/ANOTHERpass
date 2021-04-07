package de.jepfa.yapm.ui

import androidx.fragment.app.Fragment
import javax.crypto.SecretKey

open class SecureFragment : BaseFragment() {

    fun getSecureActivity() : SecureActivity {
        return activity as SecureActivity
    }

    val masterSecretKey: SecretKey?
        get() {
            return getSecureActivity().masterSecretKey
        }
}