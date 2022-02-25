package de.jepfa.yapm.usecase.label

import de.jepfa.yapm.R
import de.jepfa.yapm.service.label.LabelFilter
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.OutputUseCase
import de.jepfa.yapm.usecase.UseCaseOutput


object DeleteUnusedLabelUseCase: OutputUseCase<String, SecureActivity>() {

    override fun execute(activity: SecureActivity): UseCaseOutput<String> {
        val deleteCandidates = LabelService.defaultHolder.getAllLabels()
            .filter { label ->
                val labelId = label.labelId
                if (labelId != null) {
                    LabelService.defaultHolder.getCredentialIdsForLabelId(labelId)?.isEmpty() ?: true
                }
                else {
                    false
                }
            }.toList()

        if (deleteCandidates.isEmpty()) {
            return UseCaseOutput(false, activity.getString(R.string.no_unused_labels_to_delete))
        }
        // update model
        deleteCandidates.forEach { label ->
                LabelService.defaultHolder.removeLabel(label)
                LabelFilter.unsetFilterFor(label)
            }

        // update repo at once
        val deleteCandidateIds = deleteCandidates
            .mapNotNull { it.labelId }
            .toList()

        activity.labelViewModel.deleteByIds(deleteCandidateIds)

        return UseCaseOutput(true, activity.getString(R.string.unused_labels_deleted, deleteCandidateIds.size))
    }

}