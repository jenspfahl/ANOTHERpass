package de.jepfa.yapm.usecase

import android.util.Log
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.CreateVaultUseCase.encryptAndStoreMasterKey
import de.jepfa.yapm.usecase.CreateVaultUseCase.storeMasterPassword
import de.jepfa.yapm.usecase.LoginUseCase.getMasterKey
import de.jepfa.yapm.usecase.LoginUseCase.getMasterPassPhraseSK
import de.jepfa.yapm.util.PreferenceUtil

object ChangeMasterPasswordUseCase {

    fun execute(pin: Password, newMasterPassword: Password, storeMasterPassword: Boolean, activity: BaseActivity): Boolean {

        val salt = SecretService.getSalt(activity)
        val currentMasterPassword = getMasterPassword()
        if (currentMasterPassword == null) {
            Log.e("CHANMP", "master password not at Session")
            return false;
        }

        val oldMasterPassphraseSK = getMasterPassPhraseSK(pin, currentMasterPassword, salt)

        val encEncryptedMasterKey = PreferenceUtil.getEncrypted(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, activity)
        if (encEncryptedMasterKey == null) {
            Log.e("CHANMP", "master key not on device")
            return false;
        }

        val masterKey = getMasterKey(oldMasterPassphraseSK, encEncryptedMasterKey)
        if (masterKey == null) {
            Log.e("CHANMP", "cannot decrypt master key, pin wrong?")
            return false;
        }

        encryptAndStoreMasterKey(masterKey, pin, newMasterPassword, salt, activity)
        masterKey.clear()

        if (storeMasterPassword) {
            storeMasterPassword(newMasterPassword, activity)
        }

        PreferenceUtil.delete(PreferenceUtil.PREF_MASTER_PASSWORD_TOKEN_KEY)

        return LoginUseCase.execute(pin, newMasterPassword, activity)

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