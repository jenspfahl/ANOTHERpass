package de.jepfa.yapm.usecase.webextension

import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase


object DeleteWebExtensionUseCase: InputUseCase<EncWebExtension, BaseActivity>() {

    override suspend fun doExecute(webExtension: EncWebExtension, activity: BaseActivity): Boolean {
        val id = webExtension.id
        if (id != null) {
            activity.webExtensionViewModel.deleteById(id)
            SecretService.removeAndroidSecretKey(webExtension.getServerKeyPairAlias())
        }

        return true
    }

}