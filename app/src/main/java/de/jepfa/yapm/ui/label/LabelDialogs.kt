package de.jepfa.yapm.ui.label

import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.label.DeleteLabelUseCase

object LabelDialogs {

    fun openDeleteLabel(label: Label, activity: SecureActivity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.title_delete_label)
            .setMessage(activity.getString(R.string.message_delete_label, label.name))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                DeleteLabelUseCase.execute(label, activity)

            }
            .setNegativeButton(android.R.string.no, null)
            .show()
    }
}