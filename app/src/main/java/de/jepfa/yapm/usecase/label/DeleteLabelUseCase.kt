package de.jepfa.yapm.usecase.label

import de.jepfa.yapm.service.label.LabelFilter
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.label.LabelsHolder
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.util.observeOnce


object DeleteLabelUseCase: InputUseCase<Label, SecureActivity>() {

    override fun doExecute(label: Label, activity: SecureActivity): Boolean {
        val key = activity.masterSecretKey
        val labelId = label.labelId
        if (key != null && labelId != null) {
            val credentialsToUpdate = LabelService.defaultHolder.getCredentialIdsForLabelId(labelId)
            credentialsToUpdate?.forEach { credentialId ->
                activity.credentialViewModel.getById(credentialId).observeOnce(activity) { credential ->
                    credential?.let {
                        val labels = LabelService.defaultHolder.decryptLabelsForCredential(key, credential)

                        val remainingLabelChips = labels
                            .filterNot { it.labelId == labelId}
                            .map { it.name }
                        LabelService.defaultHolder.encryptLabelIds(key, remainingLabelChips)
                        LabelService.defaultHolder.updateLabelsForCredential(key, credential)
                    }
                }

            }
            LabelService.defaultHolder.removeLabel(label)
            LabelFilter.unsetFilterFor(label)
            activity.labelViewModel.deleteById(label.labelId)
        }

        return true
    }

}