package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.PreferenceUtil

object DeleteStoredMasterPasswordUseCase: SecureActivityUseCase {

    override fun execute(activity: SecureActivity): Boolean {
        if (!Session.isDenied()) {
            PreferenceUtil.delete(PreferenceUtil.PREF_ENCRYPTED_MASTER_PASSWORD, activity)
            Session.logout()
        }
        activity.closeOverlayDialogs()
        SecureActivity.SecretChecker.getOrAskForSecret(activity)

        return true
    }
}