package de.jepfa.yapm.usecase.secret

import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.*
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.util.DebugInfo

object ChangeMasterPasswordUseCase:
    InputUseCase<LoginData, SecureActivity>() {

    private const val TAG = "CHANGE_MP"

    override suspend fun doExecute(loginData: LoginData, activity: SecureActivity): Boolean {

        val salt = SaltService.getSalt(activity)
        val currentMasterPassword = MasterPasswordService.getMasterPasswordFromSession(activity)
        if (currentMasterPassword == null) {
            DebugInfo.logException(TAG, "master password not at Session")
            return false
        }
        val cipherAlgorithm = SecretService.getCipherAlgorithm(activity)
        val oldMasterPassphraseSK = MasterKeyService.getMasterPassPhraseSecretKey(
            loginData.pin,
            currentMasterPassword,
            salt,
            cipherAlgorithm,
            activity,
        )

        val encEncryptedMasterKey =
            PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, activity)
        if (encEncryptedMasterKey == null) {
            DebugInfo.logException(TAG, "master key not on device")
            return false
        }

        val masterKey = MasterKeyService.decryptMasterKey(oldMasterPassphraseSK, encEncryptedMasterKey, activity)
        if (masterKey == null) {
            DebugInfo.logException(TAG, "cannot decrypt master key, pin wrong?")
            return false
        }

        val pbkdfIterations = KdfParameterService.getStoredPbkdfIterations()

        MasterKeyService.encryptAndStoreMasterKey(
            masterKey,
            loginData.pin,
            loginData.masterPassword,
            salt,
            pbkdfIterations,
            cipherAlgorithm,
            activity
        )
        masterKey.clear()


        PreferenceService.delete(PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY, activity)
        PreferenceService.putCurrentDate(PreferenceService.DATA_MK_MODIFIED_AT, activity)
        PreferenceService.putCurrentDate(PreferenceService.DATA_MP_MODIFIED_AT, activity)

        return LoginUseCase.execute(loginData, activity).success

    }
}