package de.jepfa.yapm.usecase.secret

import android.util.Log
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.usecase.session.LoginUseCase

object ChangePinUseCase: InputUseCase<LoginData, SecureActivity>() {

    private const val TAG = "CHANGE_PIN"


    override fun doExecute(loginData: LoginData, activity: SecureActivity): Boolean {

        val salt = SaltService.getSalt(activity)
        val masterPassword = MasterPasswordService.getMasterPasswordFromSession()
        if (masterPassword == null) {
            Log.e(TAG, "master password not at Session")
            return false
        }

        val cipherAlgorithm = SecretService.getCipherAlgorithm(activity)
        val oldMasterPassphraseSK = MasterKeyService.getMasterPassPhraseSK(
            loginData.pin,
            masterPassword,
            salt,
            cipherAlgorithm
        )

        val encEncryptedMasterKey =
            PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, activity)
        if (encEncryptedMasterKey == null) {
            Log.e(TAG, "master key not on device")
            return false
        }

        val masterKey = MasterKeyService.getMasterKey(oldMasterPassphraseSK, encEncryptedMasterKey)
        if (masterKey == null) {
            Log.e(TAG, "cannot decrypt master key, pin wrong?")
            return false
        }

        MasterKeyService.encryptAndStoreMasterKey(
            masterKey,
            loginData.pin,
            masterPassword,
            salt,
            cipherAlgorithm,
            activity
        )

        return LoginUseCase.execute(LoginData(loginData.pin, masterPassword), activity).success
    }

}