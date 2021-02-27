package de.jepfa.yapm.model

import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

object Secret {

    /**
     * After this period of time of inactivity the secret is outdated.
     */
    private val SECRET_KEEP_VALID: Long = TimeUnit.SECONDS.toMillis(600) // TODO 60

    private var masterSecretKey: SecretKey? = null
    private var masterPassword: Encrypted? = null
    private var lastUpdated: Long = 0

    fun get() : SecretKey {
        return masterSecretKey!!
    }

    fun update(secretKey: SecretKey) {
        masterSecretKey = secretKey
        update()
    }

    fun update() {
        lastUpdated = System.currentTimeMillis()
    }

    fun isLockedOrOutdated() : Boolean {
        return masterSecretKey == null || isOutdated()
    }

    fun isLoggedOut() : Boolean {
        return masterPassword == null
    }

    fun isDeclined() : Boolean {
        return isLoggedOut() || isLockedOrOutdated()
    }

    fun lock() {
        masterSecretKey = null
        update()
    }

    fun logout() {
        lock()
        masterPassword = null
    }

    private fun isOutdated(): Boolean {
        val age: Long = System.currentTimeMillis() - lastUpdated

        return age > SECRET_KEEP_VALID
    }

}
