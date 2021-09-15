package de.jepfa.yapm.usecase

import android.content.Context
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.MasterKeyService.getMasterPassPhraseSK
import de.jepfa.yapm.service.secret.MasterKeyService.getMasterSK
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_LOCK_TIMEOUT
import de.jepfa.yapm.service.PreferenceService.PREF_LOGOUT_TIMEOUT
import de.jepfa.yapm.util.Constants.FAST_KEYGEN_VAULT_VERSION

object LoginUseCase {

    fun execute(pin: Password, masterPassword: Password, context: Context?): Boolean {
        if (context == null) return false
        val salt = SaltService.getSalt(context)
        val cipherAlgorithm = SecretService.getCipherAlgorithm(context)
        val encMasterKey = PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, context)
        if (encMasterKey == null) {
            return false
        }
        val masterPassPhraseSK = getMasterPassPhraseSK(pin, masterPassword, salt, cipherAlgorithm)
        val vaultVersion = PreferenceService.getAsInt(PreferenceService.DATA_VAULT_VERSION, context)
        val useLegacyGeneration = vaultVersion < FAST_KEYGEN_VAULT_VERSION
        val masterSecretKey = getMasterSK(masterPassPhraseSK, salt, encMasterKey, useLegacyGeneration)
        if (masterSecretKey == null) {
            return false
        }
        val key = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
        val encMasterPassword = SecretService.encryptPassword(key, masterPassword)
        Session.login(masterSecretKey, encMasterPassword)
        Session.setTimeouts(
            PreferenceService.getAsInt(PREF_LOCK_TIMEOUT, context),
            PreferenceService.getAsInt(PREF_LOGOUT_TIMEOUT, context)
        )

        return true

    }

}