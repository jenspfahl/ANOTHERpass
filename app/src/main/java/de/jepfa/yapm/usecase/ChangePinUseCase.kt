package de.jepfa.yapm.usecase

import android.util.Log
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.util.MasterKeyHelper.encryptAndStoreMasterKey
import de.jepfa.yapm.util.MasterKeyHelper.getMasterKey
import de.jepfa.yapm.util.MasterKeyHelper.getMasterPassPhraseSK
import de.jepfa.yapm.util.MasterPasswordHelper.getMasterPasswordFromSession
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY

object ChangePinUseCase {

    private const val TAG = "CHANGE_PIN"


    fun execute(currentPin: Password, newPin: Password, activity: BaseActivity): Boolean {

        val salt = SecretService.getSalt(activity)
        val masterPassword = getMasterPasswordFromSession()
        if (masterPassword == null) {
            Log.e(TAG, "master password not at Session")
            return false;
        }

        val oldMasterPassphraseSK = getMasterPassPhraseSK(currentPin, masterPassword, salt)

        val encEncryptedMasterKey = PreferenceUtil.getEncrypted(PREF_ENCRYPTED_MASTER_KEY, activity)
        if (encEncryptedMasterKey == null) {
            Log.e(TAG, "master key not on device")
            return false;
        }

        val masterKey = getMasterKey(oldMasterPassphraseSK, encEncryptedMasterKey)
        if (masterKey == null) {
            Log.e(TAG, "cannot decrypt master key, pin wrong?")
            return false;
        }

        encryptAndStoreMasterKey(masterKey, newPin, masterPassword, salt, activity)

        return LoginUseCase.execute(newPin, masterPassword, activity)

    }

}