package de.jepfa.yapm.usecase

import android.content.Context
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_CIPHER_ALGORITHM
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_CREATED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_VERSION
import de.jepfa.yapm.service.secret.MasterKeyService.encryptAndStoreMasterKey
import de.jepfa.yapm.service.secret.MasterKeyService.generateMasterKey
import de.jepfa.yapm.service.secret.MasterPasswordService.storeMasterPassword
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService.generateKey
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.Constants.FAST_KEYGEN_VAULT_VERSION
import java.util.*

object CreateVaultUseCase {

    fun execute(
        pin: Password,
        masterPasswd: Password,
        storeMasterPasswd: Boolean,
        cipherAlgorithm: CipherAlgorithm,
        context: Context?
    ): Boolean {
        if (context == null) return false
        val salt = SaltService.getSalt(context)
        val masterKey = generateMasterKey()
        encryptAndStoreMasterKey(masterKey, pin, masterPasswd, salt, cipherAlgorithm, context)
        masterKey.clear()

        if (storeMasterPasswd) {
            storeMasterPassword(masterPasswd, context)
        }
        PreferenceService.putString(
            DATA_VAULT_CREATED_AT,
            Constants.SDF_DT_MEDIUM.format(Date()),
            context)
        PreferenceService.putString(DATA_VAULT_VERSION, FAST_KEYGEN_VAULT_VERSION.toString(), context)
        PreferenceService.putString(DATA_CIPHER_ALGORITHM, cipherAlgorithm.name, context)

        return true
    }

}