
package de.jepfa.yapm.ui

import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.iterator
import androidx.core.view.size
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService.encryptCommonString
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.createAndAddLabelChip
import de.jepfa.yapm.util.toastText


class LabelEditViewExtender(private val activity: SecureActivity, private val view: View) {

    private val LAST_CHARS = listOf(' ', '\t', System.lineSeparator())


    private var allLabelAdapter: ArrayAdapter<String>

    private var editCredentialLabelsTextView: AutoCompleteTextView = view.findViewById(R.id.edit_credential_labels_textview)
    private var editCredentialLabelsChipGroup: ChipGroup = view.findViewById(R.id.edit_credential_labels_chipgroup)

    init {

        allLabelAdapter = ArrayAdapter(
            activity,
            android.R.layout.simple_dropdown_item_1line,
            ArrayList<String>()
        )

        editCredentialLabelsTextView.setAdapter(allLabelAdapter)
        editCredentialLabelsTextView.setOnItemClickListener { parent, _, position, _ ->
            editCredentialLabelsTextView.text = null
            val selected = parent.getItemAtPosition(position) as String

            addChipToLabelGroup(selected)
        }

        editCredentialLabelsTextView.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val unfinishedText = editCredentialLabelsTextView.text.toString()
                addChipToLabelGroup(unfinishedText)
                editCredentialLabelsTextView.text = null
            }
        }
        editCredentialLabelsTextView.doAfterTextChanged {
            val committedText = it.toString()
            if (isCommittedLabelName(committedText)) {
                val labelName = clearCommittedLabelName(committedText)
                addChipToLabelGroup(labelName)
                editCredentialLabelsTextView.text = null
            }
        }
        val allLabels = LabelService.defaultHolder.getAllLabels()

        val labelSuggestions = allLabels
            .map { it.name }
        allLabelAdapter.addAll(labelSuggestions)

    }


    fun updateWithLabels(labels: List<Label>, ) {
        labels
            .forEachIndexed { _, label ->
                createAndAddChip(label, editCredentialLabelsChipGroup)
            }
    }

    fun getLabelNames(): List<String> {
        return mapToLabelName(editCredentialLabelsChipGroup)
    }

    private fun mapToLabelName(chipGroup: ChipGroup): List<String> {
        val chipNames = ArrayList<String>(chipGroup.size)
        chipGroup.iterator().forEach {
            val chip = it as Chip
            chipNames.add(chip.text.toString())
        }
        return chipNames.toList()
    }

    private fun addChipToLabelGroup(labelName: String) {
        if (labelName.isNotBlank()) {
            val label = LabelService.defaultHolder.lookupByLabelName(labelName) ?: Label(labelName)
            val maxLabelLength = activity.resources.getInteger(R.integer.max_label_name_length)
            val chipsCount = editCredentialLabelsChipGroup.size
            if (containsLabel(editCredentialLabelsChipGroup, label)) {
                toastText(
                    activity,
                    R.string.label_already_in_ist)
            }
            else if (chipsCount >= Constants.MAX_LABELS_PER_CREDENTIAL) {
                toastText(
                    activity,
                    activity.getString(R.string.max_labels_reached, Constants.MAX_LABELS_PER_CREDENTIAL)
                )
            } else if (label.name.length > maxLabelLength) {
                toastText(
                    activity,
                    activity.getString(R.string.label_too_long, maxLabelLength)
                )
            } else {
                createAndAddChip(label, editCredentialLabelsChipGroup)

                // save label
                activity.masterSecretKey?.let { key ->
                    val existing = LabelService.defaultHolder.lookupByLabelName(label.name)
                    if (existing == null) {
                        val encName = encryptCommonString(key, label.name)
                        val encDesc = encryptCommonString(key, "")
                        val encLabel = EncLabel(null, null, encName, encDesc, null)
                        activity.labelViewModel.insert(encLabel, activity)
                    }
                }
            }
        }
    }

    private fun containsLabel(chipGroup: ChipGroup, label: Label): Boolean {
        for (child in chipGroup) {
            if (child is Chip) {
                if (child.text == label.name) {
                    return true
                }
            }
        }
        return false
    }

    private fun createAndAddChip(
        label: Label,
        chipGroup: ChipGroup,
    ): Chip {
        val chip = createAndAddLabelChip(label, chipGroup, thinner = false, activity)
        allLabelAdapter.remove(chip.text.toString())

        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            chipGroup.removeView(chip)
            allLabelAdapter.add(chip.text.toString())
        }
        return chip
    }

    private fun isCommittedLabelName(text: String): Boolean {
        if (text.isEmpty()) {
            return false
        }

        val lastChar = text.last()
        return lastChar in LAST_CHARS
    }

    private fun clearCommittedLabelName(text: String): String {
        if (text.isEmpty()) {
            return text
        }
        return text.substring(0, text.length - 1)
    }


}