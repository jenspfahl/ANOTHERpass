package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.service.secret.MasterKeyService.getMasterPassPhraseSK
import de.jepfa.yapm.service.secret.MasterKeyService.getMasterSK
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_LOCK_TIMEOUT
import de.jepfa.yapm.service.PreferenceService.PREF_LOGOUT_TIMEOUT

object LoginUseCase {

    fun execute(masterPin: Password, masterPassword: Password, activity: BaseActivity): Boolean {

        val salt = SaltService.getSalt(activity)
        val encMasterKey = PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, activity)
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
        Session.setTimeouts(
            PreferenceService.getAsInt(PREF_LOCK_TIMEOUT, activity),
            PreferenceService.getAsInt(PREF_LOGOUT_TIMEOUT, activity)
        )

        return true

    }

}