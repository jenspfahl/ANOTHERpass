package de.jepfa.yapm.usecase.webextension

import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase


object DeleteWebExtensionUseCase: InputUseCase<EncWebExtension, SecureActivity>() {

    override suspend fun doExecute(webExtension: EncWebExtension, activity: SecureActivity): Boolean {
        val key = activity.masterSecretKey
        val id = webExtension.id
        if (key != null && id != null) {
            activity.webExtensionViewModel.deleteById(id)
            SecretService.removeAndroidSecretKey(webExtension.getClientPubKeyAlias())
        }

        return true
    }

}