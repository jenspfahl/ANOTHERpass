package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.SecureActivity

object LogoutUseCase: UseCase {

    override fun execute(activity: SecureActivity): Boolean {
        Session.logout()

        activity.closeOverlayDialogs()
        activity.finishAndRemoveTask()
        activity.finishAffinity()

        return true
    }
}