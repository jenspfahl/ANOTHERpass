package de.jepfa.yapm.ui.label

import android.app.AlertDialog
import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.createAndAddLabelChip

object LabelDialogOpener {

    fun openLabelDialog(activity: SecureActivity, label: Label) {

        val inflater: LayoutInflater = activity.layoutInflater
        val labelsView: View = inflater.inflate(R.layout.content_dynamic_labels_list, null)
        val labelsContainer: LinearLayout = labelsView.findViewById(R.id.dynamic_labels)

        createAndAddLabelChip(label, labelsContainer, activity)

        val textView = TextView(activity)
        textView.text = label.description
        textView.gravity = Gravity.CENTER_HORIZONTAL
        labelsContainer.addView(textView)

        val dialog = AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.label_details))
            .setIcon(R.drawable.ic_baseline_label_24)
            .setView(labelsView)
            .setPositiveButton(R.string.title_change_label, null)
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
}