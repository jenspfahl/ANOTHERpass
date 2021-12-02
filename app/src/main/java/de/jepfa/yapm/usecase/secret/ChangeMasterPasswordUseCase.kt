package de.jepfa.yapm.usecase.secret

import android.util.Log
import de.jepfa.yapm.R
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.util.toastText

object ChangeMasterPasswordUseCase:
    InputUseCase<ChangeMasterPasswordUseCase.Input, SecureActivity>() {

    data class Input(
        val loginData: LoginData,
        val storeMasterPassword: Boolean
    )

    private const val TAG = "CHANGE_MP"

    override fun doExecute(input: Input, activity: SecureActivity): Boolean {

        val salt = SaltService.getSalt(activity)
        val currentMasterPassword = MasterPasswordService.getMasterPasswordFromSession(activity)
        if (currentMasterPassword == null) {
            Log.e(TAG, "master password not at Session")
            return false
        }
        val cipherAlgorithm = SecretService.getCipherAlgorithm(activity)
        val oldMasterPassphraseSK = MasterKeyService.getMasterPassPhraseSK(
            input.loginData.pin,
            currentMasterPassword,
            salt,
            cipherAlgorithm
        )

        val encEncryptedMasterKey =
            PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, activity)
        if (encEncryptedMasterKey == null) {
            Log.e(TAG, "master key not on device")
            return false
        }

        val masterKey = MasterKeyService.getMasterKey(oldMasterPassphraseSK, encEncryptedMasterKey, activity)
        if (masterKey == null) {
            Log.e(TAG, "cannot decrypt master key, pin wrong?")
            return false
        }

        MasterKeyService.encryptAndStoreMasterKey(
            masterKey,
            input.loginData.pin,
            input.loginData.masterPassword,
            salt,
            cipherAlgorithm,
            activity
        )
        masterKey.clear()


        PreferenceService.delete(PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY, activity)
        PreferenceService.putCurrentDate(PreferenceService.DATA_MK_MODIFIED_AT, activity)


        if (input.storeMasterPassword) {
            MasterPasswordService.storeMasterPassword(input.loginData.masterPassword, activity,
                {
                    toastText(activity, R.string.masterpassword_stored)
                    LoginUseCase.execute(input.loginData, activity)
                },
                {
                    toastText(activity, R.string.masterpassword_not_stored)
                    LoginUseCase.execute(input.loginData, activity)
                })

            return true
        }
        else {
            return LoginUseCase.execute(input.loginData, activity).success
        }

    }
}