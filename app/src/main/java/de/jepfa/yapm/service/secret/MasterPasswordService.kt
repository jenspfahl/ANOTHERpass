package de.jepfa.yapm.service.secret

import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.service.PreferenceService

object MasterPasswordService {

    fun getMasterPasswordFromSession() : Password? {
        val transSK = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
        val encMasterPasswd = Session.getEncMasterPasswd() ?: return null
        return SecretService.decryptPassword(transSK, encMasterPasswd)
    }

    fun storeMasterPassword(masterPasswd: Password, activity: BaseActivity) {
        val mpSK = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MP)
        val encMasterPasswd = SecretService.encryptPassword(mpSK, masterPasswd)
        PreferenceService.putEncrypted(
            PreferenceService.DATA_ENCRYPTED_MASTER_PASSWORD,
            encMasterPasswd,
            activity
        )
    }

}