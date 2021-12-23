package de.jepfa.yapm.usecase.vault

import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.label.LabelsHolder
import de.jepfa.yapm.service.secret.MasterPasswordService
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

        MasterPasswordService.deleteStoredMasterPassword(activity)
        PreferenceService.deleteAllData(activity)
        PreferenceService.delete(PreferenceService.STATE_LOGIN_ATTEMPTS, activity)

        LabelService.defaultHolder.clearAll()
        CoroutineScope(Dispatchers.IO).launch {
            activity.getApp().database?.clearAllTables()
        }
    }
}