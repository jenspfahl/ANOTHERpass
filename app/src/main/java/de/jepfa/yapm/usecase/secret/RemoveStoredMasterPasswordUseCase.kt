package de.jepfa.yapm.usecase.secret

import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.BasicUseCase

object RemoveStoredMasterPasswordUseCase: BasicUseCase<SecureActivity>() {

    override fun execute(activity: SecureActivity): Boolean {
        MasterPasswordService.deleteStoredMasterPassword(activity)

        return true
    }

}