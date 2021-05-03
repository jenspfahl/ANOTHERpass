package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.SecretService.generateKey
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.service.secret.MasterKeyService.encryptAndStoreMasterKey
import de.jepfa.yapm.service.secret.MasterPasswordService.storeMasterPassword
import de.jepfa.yapm.service.secret.SecretService

object CreateVaultUseCase {

    fun execute(pin: Password, masterPasswd: Password, storeMasterPasswd: Boolean, activity: BaseActivity): Boolean {

        val salt = SecretService.getSalt(activity)
        val masterKey = generateKey(128)
        encryptAndStoreMasterKey(masterKey, pin, masterPasswd, salt, activity)
        masterKey.clear()

        if (storeMasterPasswd) {
            storeMasterPassword(masterPasswd, activity)
        }
        return true
    }

}