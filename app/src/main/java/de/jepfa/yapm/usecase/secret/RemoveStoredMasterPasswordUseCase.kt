package de.jepfa.yapm.usecase.secret

import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.BasicUseCase
import de.jepfa.yapm.usecase.UseCaseOutput

object RemoveStoredMasterPasswordUseCase: BasicUseCase<SecureActivity>() {

    override fun execute(activity: SecureActivity): Boolean {
        PreferenceService.delete(PreferenceService.DATA_ENCRYPTED_MASTER_PASSWORD, activity)

        return true
    }

}