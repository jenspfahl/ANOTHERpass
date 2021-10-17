package de.jepfa.yapm.usecase.label

import de.jepfa.yapm.R
import de.jepfa.yapm.service.label.LabelFilter
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.SecureActivityUseCase2
import de.jepfa.yapm.util.toastText


object DeleteUnusedLabelUseCase: SecureActivityUseCase2<Unit> {

    override fun execute(unit: Unit, activity: SecureActivity): Boolean {
        val deleteCandidates = LabelService.getAllLabels()
            .filter { label ->
                val labelId = label.labelId
                if (labelId != null) {
                    LabelService.getCredentialIdsForLabelId(labelId)?.isEmpty() ?: true
                }
                else {
                    false
                }
            }.toList()

        if (deleteCandidates.isEmpty()) {
            return false
        }
        // update model
        deleteCandidates.forEach { label ->
                LabelService.removeLabel(label)
                LabelFilter.unsetFilterFor(label)
            }

        // update repo at once
        val deleteCandidateIds = deleteCandidates
            .mapNotNull { it.labelId }
            .toList()

        activity.labelViewModel.deleteByIds(deleteCandidateIds)
        
      //  toastText(activity, activity.getString(R.string.unused_credentials_deleted, deleteCandidateIds.size))
        
        return true
    }

}