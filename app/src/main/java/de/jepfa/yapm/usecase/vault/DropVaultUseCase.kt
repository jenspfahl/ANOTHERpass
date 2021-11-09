package de.jepfa.yapm.usecase.vault

import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.BasicUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DropVaultUseCase: BasicUseCase<SecureActivity>() {

    override fun execute(activity: SecureActivity): Boolean {
        Session.logout()
        activity.closeOverlayDialogs()
        dropVaultData(activity)
        activity.finishAffinity()
        SecureActivity.SecretChecker.getOrAskForSecret(activity) // restart app
        return true
    }

    fun dropVaultData(activity: BaseActivity) {

        PreferenceService.deleteAllData(activity)
        LabelService.clearAll()
        CoroutineScope(Dispatchers.IO).launch {
            activity.getApp().database?.clearAllTables()
        }
    }
}