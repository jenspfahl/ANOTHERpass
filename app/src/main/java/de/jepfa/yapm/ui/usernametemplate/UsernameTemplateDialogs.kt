package de.jepfa.yapm.ui.usernametemplate

import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncUsernameTemplate
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.label.DeleteLabelUseCase
import de.jepfa.yapm.usecase.usernametemplate.DeleteUsernameTemplateUseCase

object UsernameTemplateDialogs {


    fun openDeleteUsernameTemplate(usernameTemplate: EncUsernameTemplate, activity: SecureActivity, finishActivityAfterDelete: Boolean = false) {
        activity.masterSecretKey?.let { key ->
            val username = SecretService.decryptCommonString(key, usernameTemplate.username)
            AlertDialog.Builder(activity)
                .setTitle(R.string.title_delete_username_template)
                .setMessage(activity.getString(R.string.message_delete_username_template, username))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                    UseCaseBackgroundLauncher(DeleteUsernameTemplateUseCase)
                        .launch(activity, usernameTemplate)
                        {
                            if (finishActivityAfterDelete) {
                                activity.finish()
                            }
                        }

                }
                .setNegativeButton(android.R.string.no, null)
                .show()
        }
    }
}