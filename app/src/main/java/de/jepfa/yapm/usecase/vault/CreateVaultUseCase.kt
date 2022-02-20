package de.jepfa.yapm.usecase.vault

import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.util.Constants

object CreateVaultUseCase: InputUseCase<CreateVaultUseCase.Input, BaseActivity>() {

    data class Input(val loginData: LoginData,
                     val cipherAlgorithm: CipherAlgorithm)

    override fun doExecute(input: Input, activity: BaseActivity): Boolean {
        val salt = SaltService.getSalt(activity)
        val masterKey = MasterKeyService.generateMasterKey()
        MasterKeyService.encryptAndStoreMasterKey(
            masterKey,
            input.loginData.pin,
            input.loginData.masterPassword,
            salt,
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