package de.jepfa.yapm.usecase.session

import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.TempFileService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.util.Constants


object LoginUseCase: InputUseCase<LoginData, BaseActivity>() {

    override fun doExecute(loginData: LoginData, baseActivity: BaseActivity): Boolean {
        val salt = SaltService.getSalt(baseActivity)
        val cipherAlgorithm = SecretService.getCipherAlgorithm(baseActivity)
        val encMasterKey =
            PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, baseActivity)
                ?: return false
        val masterPassPhraseSK =
            MasterKeyService.getMasterPassPhraseSK(loginData.pin, loginData.masterPassword, salt, cipherAlgorithm)
        val vaultVersion = PreferenceService.getAsInt(PreferenceService.DATA_VAULT_VERSION, baseActivity)
        val useLegacyGeneration = vaultVersion < Constants.FAST_KEYGEN_VAULT_VERSION
        val masterSecretKey = MasterKeyService.getMasterSK(
            masterPassPhraseSK,
            salt,
            encMasterKey,
            useLegacyGeneration
        ) ?: return false
        val key = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
        val encMasterPassword = SecretService.encryptPassword(key, loginData.masterPassword)
        Session.login(masterSecretKey, encMasterPassword)
        Session.setTimeouts(
            PreferenceService.getAsInt(PreferenceService.PREF_LOCK_TIMEOUT, baseActivity),
            PreferenceService.getAsInt(PreferenceService.PREF_LOGOUT_TIMEOUT, baseActivity)
        )

        TempFileService.clearSharesCache(baseActivity)

        return true

    }
}