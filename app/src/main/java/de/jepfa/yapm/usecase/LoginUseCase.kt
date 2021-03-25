package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.service.secret.MasterKeyService.getMasterPassPhraseSK
import de.jepfa.yapm.service.secret.MasterKeyService.getMasterSK
import de.jepfa.yapm.util.PreferenceUtil

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
        val key = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
        val encMasterPassword = SecretService.encryptPassword(key, masterPassword)
        Session.login(masterSecretKey, encMasterPassword)

        return true

    }

}