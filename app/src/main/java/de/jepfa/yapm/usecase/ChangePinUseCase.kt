package de.jepfa.yapm.usecase

import android.util.Log
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Key
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.CreateVaultUseCase.encryptAndStoreMasterKey
import de.jepfa.yapm.usecase.LoginUseCase.getMasterKey
import de.jepfa.yapm.usecase.LoginUseCase.getMasterPassPhraseSK
import de.jepfa.yapm.util.PreferenceUtil
import javax.crypto.SecretKey

object ChangePinUseCase {

    fun execute(currentPin: Password, newPin: Password, activity: BaseActivity): Boolean {

        val salt = SecretService.getSalt(activity)
        val masterPassword = getMasterPassword()
        if (masterPassword == null) {
            Log.e("CHANGPIN", "master password not at Session")
            return false;
        }

        val oldMasterPassphraseSK = getMasterPassPhraseSK(currentPin, masterPassword, salt)

        val encEncryptedMasterKey = PreferenceUtil.getEncrypted(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, activity)
        if (encEncryptedMasterKey == null) {
            Log.e("CHANGPIN", "master key not on device")
            return false;
        }

        val masterKey = getMasterKey(oldMasterPassphraseSK, encEncryptedMasterKey)
        if (masterKey == null) {
            Log.e("CHANGPIN", "cannot decrypt master key, pin wrong?")
            return false;
        }

        encryptAndStoreMasterKey(masterKey, newPin, masterPassword, salt, activity)

        return LoginUseCase.execute(newPin, masterPassword, activity)

    }

    private fun getMasterPassword() : Password? {
        val transSK = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)

        val encMasterPasswd = Session.getEncMasterPasswd()
        if (encMasterPasswd == null) {
            return null;
        }
        return SecretService.decryptPassword(transSK, encMasterPasswd)
    }

}