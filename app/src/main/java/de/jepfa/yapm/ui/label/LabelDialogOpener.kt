package de.jepfa.yapm.ui.label

import android.app.AlertDialog
import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.pchmn.materialchips.ChipView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.service.label.LabelFilter
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.SecureActivity

object LabelDialogOpener {

    fun openLabelDialog(activity: SecureActivity, label: LabelService.Label) {

        val inflater: LayoutInflater = activity.getLayoutInflater()
        val labelsView: View = inflater.inflate(R.layout.content_dynamic_labels_list, null)
        val labelsContainer: LinearLayout = labelsView.findViewById(R.id.dynamic_labels)

        val chipView = ChipView(activity)
        // doesnt work: chipView.setChip(it.labelChip)
        chipView.label = label.labelChip.label
        chipView.setChipBackgroundColor(label.labelChip.getColor(activity))
        chipView.setLabelColor(activity.getColor(R.color.white))
        chipView.setPadding(16)
        chipView.gravity = Gravity.CENTER_HORIZONTAL

        labelsContainer.addView(chipView)

        val textView = TextView(activity)
        textView.text = label.labelChip.description
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
                intent.putExtra(EncLabel.EXTRA_LABEL_ID, label.encLabel.id)
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