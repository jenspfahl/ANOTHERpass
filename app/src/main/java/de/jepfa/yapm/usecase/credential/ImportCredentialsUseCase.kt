package de.jepfa.yapm.usecase.credential

import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.AutoBackupService
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase

object ImportCredentialsUseCase: InputUseCase<ImportCredentialsUseCase.Input, SecureActivity>() {

    data class Input(val credentials: List<EncCredential>, val newLabelsNotExisting: List<EncLabel>)

    override suspend fun doExecute(input: Input, activity: SecureActivity): Boolean {

        val key = activity.masterSecretKey ?: return false

        // create and update cache
        input.newLabelsNotExisting.forEach { encLabel ->
            val inserted = activity.getApp().labelRepository.insert(encLabel)
            LabelService.defaultHolder.updateLabel(LabelService.getLabelFromEncLabel(key, inserted))
        }



        input.credentials.forEach { credential ->

            // update label refs
            val credLabels = LabelService.defaultHolder.decryptLabelsForCredential(key, credential)

            val labelNamesToAdd = HashSet<String>(credLabels.map { it.name })

            val encLabels = LabelService.defaultHolder.encryptLabelIds(
                key,
                labelNamesToAdd.toList()
            )

            credential.labels = encLabels

            /*

            // unsure to allow overwriting credentials with the same uuid
            // Commented out for now since this would require a massive UI change (old/new)
            var existingCredential: EncCredential? = null
            if (credential.uid != null) {
                val credentials = activity.getApp().credentialRepository.getAllByUidsSync(listOf(credential.uid))
                if (credentials.isNotEmpty()) {
                    existingCredential = credentials.first()
                }
            }
            if (existingCredential != null) {
                existingCredential.copyData(credential)
                activity.credentialViewModel.update(existingCredential, activity)

            }
            else {*/
                credential.uid = null
                activity.getApp().credentialRepository.insert(credential)
            //}


            activity.masterSecretKey?.let { key ->
                activity.credentialViewModel.updateCredentialExpiry(credential, key, activity, considerExpiredForThePast = true)
            }
        }

        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, activity)
        AutoBackupService.autoExportVault(activity)

        return true
    }


}