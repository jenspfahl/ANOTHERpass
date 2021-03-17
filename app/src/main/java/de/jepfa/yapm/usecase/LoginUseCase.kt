package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Key
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.util.PreferenceUtil
import java.util.*
import javax.crypto.SecretKey

object LoginUseCase {

    fun execute(masterPin: Password, masterPassword: Password, activity: BaseActivity): Boolean {

        val salt = SecretService.getSalt(activity)
        val encMasterKey = PreferenceUtil.getEncrypted(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, activity)
        if (encMasterKey == null) {
            return false;
        }
        val masterPassPhraseSK = getMasterPassPhraseSK(masterPin, masterPassword, salt)
        val masterSecretKey = getMasterSK(masterPassPhraseSK, salt, encMasterKey)
        if (masterSecretKey == null) {
            return false;
        }
        val key = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)
        val encMasterPassword = SecretService.encryptPassword(key, masterPassword)
        Session.login(masterSecretKey, encMasterPassword)

        return true

    }

    /**
     * Returns the Master passphrase which is calculated of the users Master Pin and his Master password
     */
    private fun getMasterPassPhraseSK(masterPin: Password, masterPassword: Password, salt: Key): SecretKey {
        val masterPassPhrase = SecretService.conjunctPasswords(masterPin, masterPassword, salt)

        val masterPassPhraseSK = SecretService.generateSecretKey(masterPassPhrase, salt)
        masterPassPhrase.clear()

        return masterPassPhraseSK
    }

    /**
     * Returns the Master Secret Key which is encrypted twice, first with the Android key
     * and second with the PassPhrase key.
     */
    private fun getMasterSK(masterPassPhraseSK: SecretKey, salt: Key, storedEncMasterKey: Encrypted): SecretKey? {
        val androidSK = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)

        val encMasterKey = SecretService.decryptEncrypted(androidSK, storedEncMasterKey)
        val masterKey = SecretService.decryptKey(masterPassPhraseSK, encMasterKey)
        if (Arrays.equals(masterKey.data, SecretService.FAILED_BYTE_ARRAY)) {
            return null
        }
        val masterSK = SecretService.generateSecretKey(masterKey, salt)
        masterKey.clear()

        return masterSK

    }
}