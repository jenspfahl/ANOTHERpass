package de.jepfa.yapm.usecase.usernametemplate

import de.jepfa.yapm.model.encrypted.EncUsernameTemplate
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase


object DeleteUsernameTemplateUseCase: InputUseCase<EncUsernameTemplate, SecureActivity>() {

    override suspend fun doExecute(usernameTemplate: EncUsernameTemplate, activity: SecureActivity): Boolean {
        val key = activity.masterSecretKey
        val id = usernameTemplate.id
        if (key != null && id != null) {
            activity.usernameTemplateViewModel.deleteById(id, activity)
        }

        return true
    }

}