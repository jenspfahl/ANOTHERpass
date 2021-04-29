package de.jepfa.yapm.ui.label

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import com.pchmn.materialchips.ChipView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncLabel
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.getIntExtra


class EditLabelActivity : SecureActivity() {

    private var label: LabelService.Label? = null
    private lateinit var labelNameTextView: TextView
    private lateinit var labelDescTextView: TextView
    private lateinit var labelColorChipView: ChipView

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_label)

        labelNameTextView  = findViewById(R.id.edit_label_name)
        labelDescTextView  = findViewById(R.id.edit_label_desc)
        labelColorChipView  = findViewById(R.id.edit_label_color)

        val labelId = intent.getIntExtra(EncLabel.EXTRA_LABEL_ID)
        if (labelId != null) {
            label = LabelService.lookupByLabelId(labelId)
        }
        label?.let {
            labelNameTextView.text = it.labelChip.label
            //labelDescTextView.text = it.labelChip.description

            labelColorChipView.setChipBackgroundColor(it.labelChip.getColor(this))
        }

        val saveButton: Button = findViewById(R.id.button_save)
        saveButton.setOnClickListener {
            if (TextUtils.isEmpty(labelNameTextView.text)) {
                labelNameTextView.setError(getString(R.string.error_field_required))
                labelNameTextView.requestFocus()
                return@setOnClickListener
            }
            val existingLabel = LabelService.lookupByLabelName(labelNameTextView.text.toString())
            if (existingLabel != null && existingLabel.encLabel.id != labelId) {
                labelNameTextView.setError(getString(R.string.error_labelname_in_use))
                labelNameTextView.requestFocus()
                return@setOnClickListener
            }

            val replyIntent = Intent()
            replyIntent.putExtra(EncLabel.EXTRA_LABEL_ID, labelId)
            replyIntent.putExtra(EncLabel.EXTRA_LABEL_NAME, labelNameTextView.text.toString())
            replyIntent.putExtra(EncLabel.EXTRA_LABEL_DESC, labelDescTextView.text.toString())
            replyIntent.putExtra(EncLabel.EXTRA_LABEL_COLOR, label?.labelChip?.color)
            setResult(Activity.RESULT_OK, replyIntent)
            finish()

        }

    }

    override fun lock() {
        recreate()
    }
}