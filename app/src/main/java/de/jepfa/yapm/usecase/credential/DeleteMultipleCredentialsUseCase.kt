package de.jepfa.yapm.usecase.credential

import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase

object DeleteMultipleCredentialsUseCase: InputUseCase<DeleteMultipleCredentialsUseCase.Input, SecureActivity>() {

    data class Input(val credentials: Set<EncCredential>)

    override suspend fun doExecute(input: Input, activity: SecureActivity): Boolean {

        activity.credentialViewModel.deleteAll(input.credentials, activity)

        return true
    }

}