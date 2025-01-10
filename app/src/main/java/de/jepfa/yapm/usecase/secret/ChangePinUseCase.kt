package de.jepfa.yapm.usecase.secret

import android.util.Log
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.*
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import de.jepfa.yapm.util.DebugInfo

object ChangePinUseCase: InputUseCase<ChangePinUseCase.Input, SecureActivity>() {

    private const val TAG = "CHANGE_PIN"

    data class Input(val currentPin: Password, val newPin: Password)

    override suspend fun doExecute(input: Input, activity: SecureActivity): Boolean {

        val salt = SaltService.getSalt(activity)
        val masterPassword = MasterPasswordService.getMasterPasswordFromSession(activity)
        if (masterPassword == null) {
            DebugInfo.logException(TAG, "master password not at Session")
            return false
        }

        val cipherAlgorithm = SecretService.getCipherAlgorithm(activity)
        val oldMasterPassphraseSK = MasterKeyService.getMasterPassPhraseSK(
            input.currentPin,
            masterPassword,
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

        val masterKey = MasterKeyService.getMasterKey(oldMasterPassphraseSK, encEncryptedMasterKey, activity)
        if (masterKey == null) {
            DebugInfo.logException(TAG, "cannot decrypt master key, pin wrong?")
            return false
        }

        val pbkdfIterations = PbkdfIterationService.getStoredPbkdfIterations()

        MasterKeyService.encryptAndStoreMasterKey(
            masterKey,
            input.newPin,
            masterPassword,
            salt,
            pbkdfIterations,
            cipherAlgorithm,
            activity
        )
        PreferenceService.putCurrentDate(PreferenceService.DATA_MK_MODIFIED_AT, activity)

        return LoginUseCase.execute(LoginData(input.newPin, masterPassword), activity).success
    }

}