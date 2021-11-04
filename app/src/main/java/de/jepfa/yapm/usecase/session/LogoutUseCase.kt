package de.jepfa.yapm.usecase.session

import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.io.TempFileService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.BasicUseCase
import de.jepfa.yapm.util.ClipboardUtil

object LogoutUseCase: BasicUseCase<SecureActivity>() {

    override fun execute(activity: SecureActivity): Boolean {
        Session.logout()

        TempFileService.clearSharesCache(activity)
        activity.closeOverlayDialogs()
        activity.finishAffinity()
        activity.finishAndRemoveTask()

        ClipboardUtil.clearClips(activity)

        return true
    }
}