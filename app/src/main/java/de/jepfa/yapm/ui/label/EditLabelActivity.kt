package de.jepfa.yapm.ui.label

import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import com.pchmn.materialchips.ChipView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.getIntExtra


class EditLabelActivity : SecureActivity() {

    private var label: LabelService.Label? = null
    private var labelColor : Int? = null
    private lateinit var labelNameTextView: TextView
    private lateinit var labelDescTextView: TextView
    private lateinit var labelColorChipView: ChipView
    private lateinit var colorDialog: AlertDialog

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_label)

        labelNameTextView  = findViewById(R.id.edit_label_name)
        labelDescTextView  = findViewById(R.id.edit_label_desc)
        labelColorChipView  = findViewById(R.id.edit_label_color)

        labelColorChipView.setChipBackgroundColor(getColor(LabelChip.DEFAULT_CHIP_COLOR_ID))


        val labelId = intent.getIntExtra(EncLabel.EXTRA_LABEL_ID)
        if (labelId != null) {
            label = LabelService.lookupByLabelId(labelId)
            setTitle(R.string.title_change_label)
        }
        else {
            setTitle(R.string.title_new_label)
        }
        label?.let {
            labelNameTextView.text = it.labelChip.label
            labelDescTextView.text = it.labelChip.description
            labelColor = it.labelChip.rgbColor
            labelColorChipView.setChipBackgroundColor(it.labelChip.getColor(this))
        }

        labelColorChipView.setOnChipClicked {
            val inflater: LayoutInflater = layoutInflater
            val labelsView: View = inflater.inflate(R.layout.content_dynamic_labels_list, null)
            val labelsContainer: LinearLayout = labelsView.findViewById(R.id.dynamic_labels)

            val labelColors = resources.getIntArray(R.array.label_colors)

            labelColors.forEachIndexed { idx, color ->
                val chipView = ChipView(this)
                // doesnt work: chipView.setChip(it.labelChip)
                chipView.label = labelNameTextView.text.toString().toUpperCase()
                chipView.setChipBackgroundColor(color)
                chipView.setLabelColor(getColor(R.color.white))
                chipView.setPadding(8)
                chipView.gravity = Gravity.CENTER_HORIZONTAL
                chipView.setOnChipClicked {dialog ->
                    labelColor = color
                    labelColorChipView.setChipBackgroundColor(color)
                    colorDialog.dismiss()
                }

                labelsContainer.addView(chipView)
            }

            colorDialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_color))
                .setIcon(R.drawable.ic_baseline_label_24)
                .setView(labelsView)
                .setNegativeButton(android.R.string.cancel, null)
                .create()

            colorDialog.show()

        }

        val saveButton: Button = findViewById(R.id.button_save)
        saveButton.setOnClickListener {
            if (TextUtils.isEmpty(labelNameTextView.text)) {
                labelNameTextView.error = getString(R.string.error_field_required)
                labelNameTextView.requestFocus()
                return@setOnClickListener
            }
            val existingLabel = LabelService.lookupByLabelName(labelNameTextView.text.toString())
            if (existingLabel != null && existingLabel.encLabel.id != labelId) {
                labelNameTextView.error = getString(R.string.error_labelname_in_use)
                labelNameTextView.requestFocus()
                return@setOnClickListener
            }

            updateLabel(labelId,
                labelNameTextView.text.toString(),
                labelDescTextView.text.toString(),
                labelColor)

            finish()

        }

    }

    override fun lock() {
        finish()
    }

    private fun updateLabel(id: Int?, name: String, desc: String, color: Int?) {

        masterSecretKey?.let { key ->
            val encName = SecretService.encryptCommonString(key, name)
            val encDesc = SecretService.encryptCommonString(key, desc)
            val encLabel = EncLabel(id, encName, encDesc, color)

            if (encLabel.isPersistent()) {
                labelViewModel.update(encLabel)
            }
            else {
                labelViewModel.insert(encLabel)
            }
            LabelService.updateLabel(key, encLabel)
        }
    }
}