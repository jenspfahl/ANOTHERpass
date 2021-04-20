package de.jepfa.yapm.usecase

import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.PreferenceUtil

object RemoveStoredMasterPasswordUseCase: SecureActivityUseCase {

    override fun execute(activity: SecureActivity): Boolean {
        PreferenceUtil.delete(PreferenceUtil.DATA_ENCRYPTED_MASTER_PASSWORD, activity)
        //Session.logout()

        return true
    }

}