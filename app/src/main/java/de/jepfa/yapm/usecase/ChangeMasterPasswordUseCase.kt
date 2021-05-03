package de.jepfa.yapm.usecase

import android.util.Log
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.service.secret.MasterKeyService.encryptAndStoreMasterKey
import de.jepfa.yapm.service.secret.MasterKeyService.getMasterKey
import de.jepfa.yapm.service.secret.MasterKeyService.getMasterPassPhraseSK
import de.jepfa.yapm.service.secret.MasterPasswordService.getMasterPasswordFromSession
import de.jepfa.yapm.service.secret.MasterPasswordService.storeMasterPassword
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.PreferenceUtil.DATA_ENCRYPTED_MASTER_KEY
import de.jepfa.yapm.util.PreferenceUtil.DATA_MASTER_PASSWORD_TOKEN_KEY

object ChangeMasterPasswordUseCase {

    private const val TAG = "CHANGE_MP"

    fun execute(pin: Password, newMasterPassword: Password, storeMasterPassword: Boolean, activity: BaseActivity): Boolean {

        val salt = SecretService.getSalt(activity)
        val currentMasterPassword = getMasterPasswordFromSession()
        if (currentMasterPassword == null) {
            Log.e(TAG, "master password not at Session")
            return false;
        }

        val oldMasterPassphraseSK = getMasterPassPhraseSK(pin, currentMasterPassword, salt)

        val encEncryptedMasterKey = PreferenceUtil.getEncrypted(DATA_ENCRYPTED_MASTER_KEY, activity)
        if (encEncryptedMasterKey == null) {
            Log.e(TAG, "master key not on device")
            return false;
        }

        val masterKey = getMasterKey(oldMasterPassphraseSK, encEncryptedMasterKey)
        if (masterKey == null) {
            Log.e(TAG, "cannot decrypt master key, pin wrong?")
            return false;
        }

        encryptAndStoreMasterKey(masterKey, pin, newMasterPassword, salt, activity)
        masterKey.clear()

        if (storeMasterPassword) {
            storeMasterPassword(newMasterPassword, activity)
        }

        PreferenceUtil.delete(DATA_MASTER_PASSWORD_TOKEN_KEY, activity)

        return LoginUseCase.execute(pin, newMasterPassword, activity)

    }

}