package de.jepfa.yapm.usecase

import android.content.Context
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_CREATED_AT
import de.jepfa.yapm.service.secret.MasterKeyService.encryptAndStoreMasterKey
import de.jepfa.yapm.service.secret.MasterPasswordService.storeMasterPassword
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService.generateKey
import de.jepfa.yapm.util.Constants
import java.util.*

object CreateVaultUseCase {

    fun execute(pin: Password, masterPasswd: Password, storeMasterPasswd: Boolean, context: Context?): Boolean {
        if (context == null) return false
        val salt = SaltService.getSalt(context)
        val masterKey = generateKey(128)
        encryptAndStoreMasterKey(masterKey, pin, masterPasswd, salt, context)
        masterKey.clear()

        if (storeMasterPasswd) {
            storeMasterPassword(masterPasswd, context)
        }
        PreferenceService.putString(
            DATA_VAULT_CREATED_AT,
            Constants.SDF_DT_MEDIUM.format(Date()),
            context)

        return true
    }

}