package de.jepfa.yapm.usecase.vault

import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.io.TempFileService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.BasicUseCase
import de.jepfa.yapm.util.ClipboardUtil

object LockVaultUseCase: BasicUseCase<SecureActivity>() {

    override fun execute(activity: SecureActivity): Boolean {
        Session.lock()
        TempFileService.clearSharesCache(activity)
        activity.closeOverlayDialogs()
        SecureActivity.SecretChecker.getOrAskForSecret(activity)
        ClipboardUtil.clearClips(activity)
        activity.finish()

        return true
    }
}