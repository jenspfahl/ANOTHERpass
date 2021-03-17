package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.SecureActivity

object LockVaultUseCase: UseCase {

    override fun execute(activity: SecureActivity): Boolean {
        Session.lock()
        activity.closeOverlayDialogs()
        activity.finishAffinity()
        SecureActivity.SecretChecker.getOrAskForSecret(activity)

        return true
    }
}