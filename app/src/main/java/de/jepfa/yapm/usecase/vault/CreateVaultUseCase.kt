package de.jepfa.yapm.usecase.vault

import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.toastText

object CreateVaultUseCase: InputUseCase<CreateVaultUseCase.Input, BaseActivity>() {

    data class Input(val loginData: LoginData,
                     val storeMasterPasswd: Boolean,
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
            Constants.FAST_KEYGEN_VAULT_VERSION.toString(),
            activity
        )
        PreferenceService.putString(
            PreferenceService.DATA_CIPHER_ALGORITHM,
            input.cipherAlgorithm.name,
            activity
        )

        if (input.storeMasterPasswd) {
            MasterPasswordService.storeMasterPassword(input.loginData.masterPassword, activity,
                {
                    toastText(activity, R.string.masterpassword_stored)
                    LoginUseCase.execute(input.loginData, activity)
                },
                {
                    toastText(activity, R.string.masterpassword_not_stored)
                    LoginUseCase.execute(input.loginData, activity)
                })

            return true
        }
        else {
            return LoginUseCase.execute(input.loginData, activity).success
        }
    }

}