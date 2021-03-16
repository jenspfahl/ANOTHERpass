package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.PreferenceUtil

object LogoutUseCase: UseCase {

    override fun execute(activity: SecureActivity): Boolean {
        Secret.logout()

        activity.closeOverlayDialogs()
        activity.finishAndRemoveTask()
        activity.finishAffinity()

        return true
    }
}