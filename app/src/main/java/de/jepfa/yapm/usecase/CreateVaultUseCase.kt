package de.jepfa.yapm.usecase

import android.util.Base64
import de.jepfa.yapm.model.Key
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.secret.SecretService.generateKey
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.service.secret.MasterKeyService.encryptAndStoreMasterKey
import de.jepfa.yapm.service.secret.MasterPasswordService.storeMasterPassword
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.PreferenceUtil.PREF_SALT

object CreateVaultUseCase {

    fun execute(pin: Password, masterPasswd: Password, storeMasterPasswd: Boolean, activity: BaseActivity): Boolean {

        val salt = createAndStoreSalt(activity)
        val masterKey = generateKey(128)
        encryptAndStoreMasterKey(masterKey, pin, masterPasswd, salt, activity)
        masterKey.clear()

        if (storeMasterPasswd) {
            storeMasterPassword(masterPasswd, activity)
        }
        return true
    }

    private fun createAndStoreSalt(activity: BaseActivity): Key {
        val salt = generateKey(128)
        val saltBase64 = Base64.encodeToString(salt.data, Base64.DEFAULT)
        PreferenceUtil.put(PREF_SALT, saltBase64, activity)

        return salt
    }

}