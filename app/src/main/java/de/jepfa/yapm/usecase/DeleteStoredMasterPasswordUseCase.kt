package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.PreferenceUtil

object DeleteStoredMasterPasswordUseCase: UseCase {

    override fun execute(activity: SecureActivity): Boolean {
        if (!Secret.isDenied()) {
            PreferenceUtil.delete(PreferenceUtil.PREF_ENCRYPTED_MASTER_PASSWORD, activity)
            Secret.logout()
        }
        activity.closeOverlayDialogs()
        SecureActivity.SecretChecker.getOrAskForSecret(activity)

        return true
    }
}