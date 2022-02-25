package de.jepfa.yapm.model.session

import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.autofill.AutofillCredentialHolder
import java.util.concurrent.TimeUnit

object Session {

    const val DEFAULT_LOCK_TIMEOUT = 2
    const val DEFAULT_LOGOUT_TIMEOUT = 10
    /**
     * After this period of time of inactivity the secret is outdated.
     */
    private var lock_timeout: Long = minutesToMillis(DEFAULT_LOCK_TIMEOUT)
    private var logout_timeout: Long = minutesToMillis(DEFAULT_LOGOUT_TIMEOUT)

    private var masterSecretKey: SecretKeyHolder? = null
    private var encMasterPassword: Encrypted? = null
    private var lastUpdated: Long = 0
    private var lockDisabled = false;

    fun getMasterKeySK() : SecretKeyHolder? {
        return masterSecretKey
    }

    fun getEncMasterPasswd() : Encrypted? {
        return encMasterPassword
    }

    fun setLockTimeout(lockTimeoutMinutes: Int) {
        if (lockTimeoutMinutes != 0) lock_timeout = minutesToMillis(lockTimeoutMinutes)
    }

    fun setLogoutTimeout(logoutTimeoutMinutes: Int) {
        if (logoutTimeoutMinutes != 0) logout_timeout = minutesToMillis(logoutTimeoutMinutes)
    }

    fun setTimeouts(lockTimeoutMinutes: Int, logoutTimeoutMinutes: Int) {
        setLockTimeout(lockTimeoutMinutes)
        setLogoutTimeout(logoutTimeoutMinutes)
    }

    fun login(secretKey: SecretKeyHolder, encMasterPasswd: Encrypted) {
        masterSecretKey = secretKey
        encMasterPassword = encMasterPasswd
        touch()
    }

    internal fun touch() {
        lastUpdated = System.currentTimeMillis()
    }

    fun isOutdated(): Boolean {
        return (!lockDisabled && age() > lock_timeout) || shouldBeLoggedOut()
    }

    fun shouldBeLoggedOut(): Boolean {
        return age() > logout_timeout
    }

    fun disableAutomaticLocking() {
        lockDisabled = true
    }

    fun enableAutomaticLocking() {
        lockDisabled = false
    }

    fun isLocked() : Boolean {
        return masterSecretKey == null
    }

    fun isLoggedOut() : Boolean {
        return encMasterPassword == null
    }

    fun isDenied() : Boolean {
        return isLoggedOut() || isLocked() || isOutdated()
    }

    fun lock() {
        lockDisabled = false;
        masterSecretKey?.destroy()
        masterSecretKey = null
        AutofillCredentialHolder.clear()
        touch()
    }

    fun logout() {
        encMasterPassword = null
        lock()
    }

    private fun age() = System.currentTimeMillis() - lastUpdated

    private fun minutesToMillis(value: Int) = TimeUnit.MINUTES.toMillis(value.toLong())

}