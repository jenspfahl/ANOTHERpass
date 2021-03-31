package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.PreferenceUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object RemoveStoredMasterPasswordUseCase: SecureActivityUseCase {

    override fun execute(activity: SecureActivity): Boolean {
        PreferenceUtil.delete(PreferenceUtil.PREF_ENCRYPTED_MASTER_PASSWORD, activity)
        //Session.logout()

        return true
    }

}