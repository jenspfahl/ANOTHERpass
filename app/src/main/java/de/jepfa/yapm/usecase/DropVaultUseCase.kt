package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.label.LabelService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DropVaultUseCase: SecureActivityUseCase {

    override fun execute(activity: SecureActivity): Boolean {
        Session.logout()
        activity.closeOverlayDialogs()
        dropVaultData(activity)
        activity.finishAffinity()
        SecureActivity.SecretChecker.getOrAskForSecret(activity) // restart app
        return true
    }

    fun dropVaultData(activity: BaseActivity) {

        PreferenceService.delete(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, activity)
        PreferenceService.delete(PreferenceService.DATA_ENCRYPTED_MASTER_PASSWORD, activity)
        PreferenceService.delete(PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY, activity)
        PreferenceService.delete(PreferenceService.DATA_SALT, activity)
        PreferenceService.delete(PreferenceService.DATA_VAULT_CREATED_AT, activity)
        PreferenceService.delete(PreferenceService.DATA_VAULT_IMPORTED_AT, activity)
        LabelService.clearAll()
        CoroutineScope(Dispatchers.IO).launch {
            activity.getApp().database?.clearAllTables()
        }
    }
}