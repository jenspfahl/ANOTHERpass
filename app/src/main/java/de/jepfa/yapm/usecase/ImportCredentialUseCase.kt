package de.jepfa.yapm.usecase

import android.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.toastText

object ImportCredentialUseCase {
    fun execute(credential: EncCredential, activity: SecureActivity, successHandler: () -> Unit) {
        val credentialId = credential.id
        if (credentialId != null) {
            activity.credentialViewModel.findById(credentialId).observe(activity) { existingCredential ->
                if (existingCredential != null) {
                    AlertDialog.Builder(activity)
                        .setTitle(R.string.import_credential)
                        .setMessage(R.string.credential_already_exists)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->

                            existingCredential.backupForRestore()
                            existingCredential.copyData(credential)

                            saveAndNavigateBack(isNew = false, activity, existingCredential, successHandler)
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
                }
                else {
                    saveAndNavigateBack(isNew = true, activity, credential, successHandler)
                }
            }
        }
        else {
            saveAndNavigateBack(isNew = true, activity, credential, successHandler)
        }
    }

    private fun saveAndNavigateBack(
        isNew: Boolean,
        activity: SecureActivity,
        credential: EncCredential,
        successHandler: () -> Unit
    ) {
        if (isNew) activity.credentialViewModel.insert(credential)
        else activity.credentialViewModel.update(credential)

        activity.masterSecretKey?.let { key ->
            val name = SecretService.decryptCommonString(key, credential.name)
            toastText(activity, activity.getString(R.string.credential_imported, name))
        }
        successHandler.invoke()

    }

}