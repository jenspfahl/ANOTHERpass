package de.jepfa.yapm.usecase

import android.util.Log
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.service.secret.MasterKeyService.encryptAndStoreMasterKey
import de.jepfa.yapm.service.secret.MasterKeyService.getMasterKey
import de.jepfa.yapm.service.secret.MasterKeyService.getMasterPassPhraseSK
import de.jepfa.yapm.service.secret.MasterPasswordService.getMasterPasswordFromSession
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_ENCRYPTED_MASTER_KEY
import de.jepfa.yapm.service.secret.SecretService

object ChangePinUseCase {

    private const val TAG = "CHANGE_PIN"


    fun execute(currentPin: Password, newPin: Password, activity: BaseActivity): Boolean {

        val salt = SaltService.getSalt(activity)
        val masterPassword = getMasterPasswordFromSession()
        if (masterPassword == null) {
            Log.e(TAG, "master password not at Session")
            return false
        }

        val cipherAlgorithm = SecretService.getCipherAlgorithm(activity)
        val oldMasterPassphraseSK = getMasterPassPhraseSK(currentPin, masterPassword, salt, cipherAlgorithm)

        val encEncryptedMasterKey = PreferenceService.getEncrypted(DATA_ENCRYPTED_MASTER_KEY, activity)
        if (encEncryptedMasterKey == null) {
            Log.e(TAG, "master key not on device")
            return false
        }

        val masterKey = getMasterKey(oldMasterPassphraseSK, encEncryptedMasterKey)
        if (masterKey == null) {
            Log.e(TAG, "cannot decrypt master key, pin wrong?")
            return false
        }

        encryptAndStoreMasterKey(masterKey, newPin, masterPassword, salt, cipherAlgorithm, activity)

        return LoginUseCase.execute(newPin, masterPassword, activity)

    }

}