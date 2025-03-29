package de.jepfa.yapm.ui.label

import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.label.DeleteLabelUseCase
import de.jepfa.yapm.util.createAndAddLabelChip

object LabelDialogs {


    fun openLabelOverviewDialog(activity: SecureActivity, label: Label, showChangeLabelButton: Boolean = true) {

        val inflater: LayoutInflater = activity.layoutInflater
        val labelsView: View = inflater.inflate(R.layout.content_dynamic_labels_list, null)
        val labelsContainer: LinearLayout = labelsView.findViewById(R.id.dynamic_labels)

        createAndAddLabelChip(label, labelsContainer, thinner = false, activity)

        val textView = TextView(activity)
        textView.text = label.description
        textView.gravity = Gravity.CENTER_HORIZONTAL

        val builder = AlertDialog.Builder(activity)
        val container = LinearLayout(builder.context)
        container.orientation = LinearLayout.VERTICAL
        container.addView(labelsView)
        container.addView(textView)

        if (showChangeLabelButton) {
            builder
                .setPositiveButton(R.string.title_change_label, null)

        }

        val dialog = builder
            .setTitle(activity.getString(R.string.title_label_details))
            .setIcon(R.drawable.ic_baseline_label_24)
            .setView(container)
            .setNegativeButton(R.string.close, null)
            .create()

        dialog.setOnShowListener {
            val buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            buttonPositive.setOnClickListener {
                val intent = Intent(activity, EditLabelActivity::class.java)
                intent.putExtra(EncLabel.EXTRA_LABEL_ID, label.labelId)
                activity.startActivity(intent)
                dialog.dismiss()
            }

            val buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            buttonNegative.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }


    fun openDeleteLabel(label: Label, activity: SecureActivity, finishActivityAfterDelete: Boolean = false) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.title_delete_label)
            .setMessage(activity.getString(R.string.message_delete_label, label.name))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                UseCaseBackgroundLauncher(DeleteLabelUseCase)
                    .launch(activity, label)
                    {
                        if (finishActivityAfterDelete) {
                            PreferenceService.putBoolean(PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_RELOAD, true, activity)
                            activity.finish()
                        }
                    }

            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}