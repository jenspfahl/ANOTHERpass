package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.PreferenceUtil
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

    private fun dropVaultData(activity: SecureActivity) {

        PreferenceUtil.delete(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, activity)
        PreferenceUtil.delete(PreferenceUtil.PREF_ENCRYPTED_MASTER_PASSWORD, activity)
        PreferenceUtil.delete(PreferenceUtil.PREF_MASTER_PASSWORD_TOKEN_KEY, activity)
        PreferenceUtil.delete(PreferenceUtil.PREF_SALT, activity)
        CoroutineScope(Dispatchers.IO).launch {
            activity.getApp().database?.clearAllTables()
        }
    }
}