package de.jepfa.yapm.usecase.secret

import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.BasicUseCase

object RevokeMasterPasswordTokenUseCase: BasicUseCase<BaseActivity>() {

    fun openDialog(activity: BaseActivity, successHandler: () -> Unit) {
        val mptCounter = PreferenceService.getAsInt(
            PreferenceService.STATE_MASTER_PASSWD_TOKEN_COUNTER,
            activity
        )
        if (PreferenceService.isPresent(PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY, activity)) {
            AlertDialog.Builder(activity)
                    .setTitle(activity.getString(R.string.revoke_last_mpt))
                    .setMessage(activity.getString(R.string.message_generate_mpt, mptCounter))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                        execute(activity)
                        successHandler()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()
        }

    }

    override fun execute(activity: BaseActivity): Boolean {

        PreferenceService.delete(
            PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY,
            activity
        )
        PreferenceService.delete(
            PreferenceService.DATA_MASTER_PASSWORD_TOKEN_NFC_TAG_ID,
            activity
        )

        return true

    }

}