package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.ClipboardUtil

object LogoutUseCase: SecureActivityUseCase {

    override fun execute(activity: SecureActivity): Boolean {
        Session.logout()

        activity.closeOverlayDialogs()
        activity.finishAndRemoveTask()

        ClipboardUtil.clearClips(activity)


        return true
    }
}