package de.jepfa.yapm.usecase.vault

import android.util.Log
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.encrypted.KdfConfig
import de.jepfa.yapm.model.encrypted.KeyDerivationFunction
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.KdfParameterService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.Constants.LOG_PREFIX

object CreateVaultUseCase: InputUseCase<CreateVaultUseCase.Input, BaseActivity>() {

    data class Input(val loginData: LoginData,
                     val kdfConfig: KdfConfig,
                     val cipherAlgorithm: CipherAlgorithm)

    override suspend fun doExecute(input: Input, activity: BaseActivity): Boolean {

        input.kdfConfig.persist(activity)
        Log.d(LOG_PREFIX + "ITERATIONS", "store KDF config=${input.kdfConfig}")


        val salt = SaltService.getSalt(activity)
        val masterKey = MasterKeyService.generateMasterKey(activity)
        MasterKeyService.encryptAndStoreMasterKey(
            masterKey,
            input.loginData.pin,
            input.loginData.masterPassword,
            salt,
            input.kdfConfig,
            input.cipherAlgorithm,
            activity
        )
        masterKey.clear()

        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_CREATED_AT, activity)

        PreferenceService.putString(
            PreferenceService.DATA_VAULT_VERSION,
            Constants.CURRENT_VERSION.toString(),
            activity
        )
        PreferenceService.putString(
            PreferenceService.DATA_CIPHER_ALGORITHM,
            input.cipherAlgorithm.name,
            activity
        )

        return LoginUseCase.execute(input.loginData, activity).success
    }

}