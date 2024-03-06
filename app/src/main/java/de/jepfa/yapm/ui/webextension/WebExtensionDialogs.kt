package de.jepfa.yapm.ui.webextension

import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.webextension.DeleteWebExtensionUseCase

object WebExtensionDialogs {


    fun openDeleteWebExtension(webExtension: EncWebExtension, activity: SecureActivity, finishActivityAfterDelete: Boolean = false) {
        activity.masterSecretKey?.let { key ->
            val name = if (webExtension.title != null) {
                SecretService.decryptCommonString(key, webExtension.title!!)
            }
            else {
                SecretService.decryptCommonString(key, webExtension.webClientId)
            }
            name

            AlertDialog.Builder(activity)
                .setTitle(R.string.title_delete_web_extension)
                .setMessage(activity.getString(R.string.message_delete_web_extension, name))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                    UseCaseBackgroundLauncher(DeleteWebExtensionUseCase)
                        .launch(activity, webExtension)
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