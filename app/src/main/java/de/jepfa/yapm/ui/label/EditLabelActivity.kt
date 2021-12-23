package de.jepfa.yapm.ui.label

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.chip.Chip
import de.jepfa.yapm.R
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.label.LabelsHolder
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.createAndAddLabelChip
import de.jepfa.yapm.util.getIntExtra
import java.util.*


class EditLabelActivity : SecureActivity() {

    private var label: Label? = null
    private var labelColor : Int? = null
    private lateinit var labelNameTextView: TextView
    private lateinit var labelDescTextView: TextView
    private lateinit var labelColorChip: Chip
    private lateinit var colorDialog: AlertDialog

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_label)

        labelNameTextView = findViewById(R.id.edit_label_name)
        labelDescTextView = findViewById(R.id.edit_label_desc)
        labelColorChip = findViewById(R.id.edit_label_color)
        labelColorChip.chipBackgroundColor = getColorStateList(Label.DEFAULT_CHIP_COLOR_ID)

        val labelId = intent.getIntExtra(EncLabel.EXTRA_LABEL_ID)
        if (labelId != null) {
            label = LabelService.defaultHolder.lookupByLabelId(labelId)
            setTitle(R.string.title_change_label)
        }
        else {
            setTitle(R.string.title_new_label)
        }
        label?.let {
            labelNameTextView.text = it.name
            labelDescTextView.text = it.description
            labelColor = it.colorRGB
            labelColorChip.chipBackgroundColor = ColorStateList.valueOf(it.getColor(this))

        }

        labelColorChip.setOnClickListener {
            val inflater: LayoutInflater = layoutInflater
            val labelsView: View = inflater.inflate(R.layout.content_dynamic_labels_list, null)
            val labelsContainer: LinearLayout = labelsView.findViewById(R.id.dynamic_labels)

            val labelColors = resources.getIntArray(R.array.label_colors)

            labelColors.forEachIndexed { idx, color ->
                val labelName = labelNameTextView.text.toString()
                val labelDesc = labelDescTextView.text.toString()
                val label = Label(labelName, labelDesc)
                val chip = createAndAddLabelChip(label, labelsContainer, thinner = false, this)
                chip.chipBackgroundColor = ColorStateList.valueOf(color)

                chip.setOnClickListener {_ ->
                    labelColor = color
                    labelColorChip.chipBackgroundColor = ColorStateList.valueOf(color)
                    colorDialog.dismiss()
                }
            }

            val builder = AlertDialog.Builder(this)
            val container = ScrollView(builder.context)
            container.addView(labelsView)

            colorDialog = builder
                .setTitle(getString(R.string.choose_color))
                .setIcon(R.drawable.ic_baseline_label_24)
                .setView(container)
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
            val existingLabel = LabelService.defaultHolder.lookupByLabelName(labelNameTextView.text.toString())
            if (existingLabel != null && existingLabel.labelId != labelId) {
                labelNameTextView.error = getString(R.string.error_labelname_in_use)
                labelNameTextView.requestFocus()
                return@setOnClickListener
            }
            val label = Label(labelId,
                labelNameTextView.text.toString(),
                labelDescTextView.text.toString(),
                labelColor)
            updateLabel(label)

            PreferenceService.putBoolean(PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_RELOAD, true, this)
            finish()

        }

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (checkSession && Session.isDenied()) {
            return false
        }

        if (label != null) {
            menuInflater.inflate(R.menu.menu_label_edit, menu)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (checkSession && Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return false
        }

        if (id == R.id.menu_delete_label) {
            label?.let { current ->
                LabelDialogs.openDeleteLabel(current, this, finishActivityAfterDelete = true)
            }

            return true
        }


        return super.onOptionsItemSelected(item)
    }


    override fun lock() {
        finish()
    }

    private fun updateLabel(label: Label) {

        masterSecretKey?.let { key ->
            val encName = SecretService.encryptCommonString(key, label.name)
            val encDesc = SecretService.encryptCommonString(key, label.description)
            val encLabel = EncLabel(label.labelId, encName, encDesc, label.colorRGB)

            if (encLabel.isPersistent()) {
                labelViewModel.update(encLabel, this)
            }
            else {
                labelViewModel.insert(encLabel, this)
            }
            LabelService.defaultHolder.updateLabel(label)
        }
    }
}