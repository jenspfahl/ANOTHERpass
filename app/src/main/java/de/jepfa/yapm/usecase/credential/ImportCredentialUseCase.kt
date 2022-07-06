package de.jepfa.yapm.usecase.credential

import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.util.enrichId
import de.jepfa.yapm.util.observeOnce
import de.jepfa.yapm.util.toastText

object ImportCredentialUseCase: InputUseCase<ImportCredentialUseCase.Input, SecureActivity>() {

    data class Input(val credential: EncCredential, val successHandler: () -> Unit)

    override suspend fun doExecute(input: Input, activity: SecureActivity): Boolean {
        val credentialId = input.credential.id
        val credentialUid = input.credential.uid
        if (credentialUid != null) {
            // is either a PCR or ECR, doesn't matter since it has a UID
            activity.credentialViewModel.findByUid(credentialUid).observeOnce(activity) { existingCredential ->
                if (existingCredential != null) {
                    AlertDialog.Builder(activity)
                        .setTitle(R.string.import_credential)
                        .setMessage(R.string.credential_already_exists)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->

                            existingCredential.backupForRestore()
                            existingCredential.copyData(input.credential)

                            saveAndNavigateBack(isNew = false, activity, existingCredential, input.successHandler)
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
                }
                else {
                    // no uid found on vault app, search by id if present
                    if (credentialId != null) {
                        checkIdAskAndSave(activity, credentialId, input)
                    }
                    else {
                        saveAndNavigateBack(isNew = true, activity, input.credential, input.successHandler)
                    }
                }
            }
        }
        else if (credentialId != null) {
            // is a ECR without a UID, PCR doesn't have an id
            checkIdAskAndSave(activity, credentialId, input)
        }
        else {
            // is a PCR without a UID
            saveAndNavigateBack(isNew = true, activity, input.credential, input.successHandler)
        }

        return true
    }

    private fun checkIdAskAndSave(
        activity: SecureActivity,
        credentialId: Int,
        input: Input
    ) {
        activity.credentialViewModel.findById(credentialId)
            .observeOnce(activity) { existingCredential ->
                if (existingCredential != null) {
                    AlertDialog.Builder(activity)
                        .setTitle(R.string.import_credential)
                        .setMessage(R.string.credential_already_exists)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->

                            existingCredential.backupForRestore()
                            existingCredential.copyData(input.credential)

                            saveAndNavigateBack(
                                isNew = false,
                                activity,
                                existingCredential,
                                input.successHandler
                            )
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
                } else {
                    saveAndNavigateBack(
                        isNew = true,
                        activity,
                        input.credential,
                        input.successHandler
                    )
                }
            }
    }

    private fun saveAndNavigateBack(
        isNew: Boolean,
        activity: SecureActivity,
        credential: EncCredential,
        successHandler: () -> Unit
    ) {
        if (isNew) activity.credentialViewModel.insert(credential, activity)
        else activity.credentialViewModel.update(credential, activity)

        activity.masterSecretKey?.let { key ->
            val name = SecretService.decryptCommonString(key, credential.name)
            val enrichedName = enrichId(activity, name, credential.id)
            toastText(activity, activity.getString(R.string.credential_imported, enrichedName))
        }
        successHandler.invoke()

    }

}