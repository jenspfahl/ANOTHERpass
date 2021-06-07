package de.jepfa.yapm.service.secret

import android.content.Context
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.service.PreferenceService
import javax.crypto.SecretKey

object MasterPasswordService {

    private val EMP_SALT = Key(byteArrayOf(12, 57, 33, 75, 22, -33, 1, 123, -72, -82, 42, 100, -18, 54, 92, 23, -89, -21, -1, 95, -51, 11, 4, -99))

    fun getMasterPasswordFromSession() : Password? {
        val transSK = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
        val encMasterPasswd = Session.getEncMasterPasswd() ?: return null
        return SecretService.decryptPassword(transSK, encMasterPasswd)
    }

    fun storeMasterPassword(masterPasswd: Password, context: Context) {
        val mpSK = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MP)
        val encMasterPasswd = SecretService.encryptPassword(mpSK, masterPasswd)
        PreferenceService.putEncrypted(
            PreferenceService.DATA_ENCRYPTED_MASTER_PASSWORD,
            encMasterPasswd,
            context
        )
    }

    fun generateEncMasterPasswdSK(passwd: Password): SecretKey {
        val empSK = SecretService.generateSecretKey(passwd, EMP_SALT, 1000)
        passwd.clear()
        return empSK
    }

}