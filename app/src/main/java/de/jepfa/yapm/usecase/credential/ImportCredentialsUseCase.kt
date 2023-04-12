package de.jepfa.yapm.usecase.credential

import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase

object ImportCredentialsUseCase: InputUseCase<ImportCredentialsUseCase.Input, SecureActivity>() {

    data class Input(val credentials: List<EncCredential>)

    override suspend fun doExecute(input: Input, activity: SecureActivity): Boolean {
        input.credentials.forEach { credential ->
            activity.credentialViewModel.insert(credential, activity)
            activity.masterSecretKey?.let { key ->
                activity.credentialViewModel.updateExpiredCredential(credential, key, activity, considerExpiredForThePast = true)
            }
        }

        return true
    }


}