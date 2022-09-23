
package de.jepfa.yapm.ui.label

import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.view.iterator
import androidx.core.view.size
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService.encryptCommonString
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.createAndAddLabelChip
import de.jepfa.yapm.util.toastText
import kotlin.random.Random


class LabelEditViewExtender(private val activity: SecureActivity,
                            view: View) {

    private val LAST_CHARS = listOf(' ', '\t', System.lineSeparator())


    private var allLabelAdapter = LabelListAdapter(activity, ArrayList(LabelService.defaultHolder.getAllLabels()))

    private var editCredentialLabelsTextView: AutoCompleteTextView = view.findViewById(R.id.autocomplete_labels_textview)
    private var editCredentialLabelsChipGroup: ChipGroup = view.findViewById(R.id.autocomplete_labels_chipgroup)

    init {

        editCredentialLabelsTextView.setAdapter(allLabelAdapter)

        editCredentialLabelsTextView.setOnItemClickListener { parent, _, position, _ ->
            editCredentialLabelsTextView.text = null
            val selectedLabel = parent.getItemAtPosition(position) as Label
            addLabelToLabelGroup(selectedLabel)
        }

        view.findViewById<ImageView>(R.id.autocomplete_label_icon)?.setOnClickListener {
            toastText(activity, R.string.start_typing_to_filter_labels)
        }

        editCredentialLabelsTextView.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                val unfinishedText = editCredentialLabelsTextView.text.toString()
                addTextToLabelGroup(unfinishedText)
                editCredentialLabelsTextView.text = null
                editCredentialLabelsTextView.dismissDropDown()

                true
            }
            else {
                false
            }
        }

        editCredentialLabelsTextView.doAfterTextChanged {
            val committedText = it.toString()
            if (isCommittedLabelName(committedText)) {
                val labelName = clearCommittedLabelName(committedText)
                addTextToLabelGroup(labelName)
                editCredentialLabelsTextView.text = null
                editCredentialLabelsTextView.dismissDropDown()

            }
        }

    }

    fun addPersistedLabels(labels: List<Label>) {
        labels
            .forEachIndexed { _, label ->
                createAndAddChip(label, editCredentialLabelsChipGroup)
            }

    }

    fun getCommitedLabelNames(): List<String> {
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

    private fun addTextToLabelGroup(labelName: String) {
        if (labelName.isNotBlank()) {
            val label = LabelService.defaultHolder.lookupByLabelName(labelName) ?: createNewLabel(labelName)
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
                saveLabelIfNeeded(label)
            }
        }
    }

    private fun createNewLabel(labelName: String): Label {
        val labelColors = activity.resources.getIntArray(R.array.label_colors)
        val allLabelColors = LabelService.defaultHolder.getAllLabels()
            .map { it.getColor(activity) }
            .toSet()
        var freeColor = labelColors
            .filterNot { allLabelColors.contains(it) }
            .firstOrNull()

        if (freeColor == null) {
            val randId = Random.nextInt(allLabelColors.size)
            freeColor = allLabelColors.elementAt(randId)
        }

        return Label(labelName, freeColor)
    }

    private fun saveLabelIfNeeded(label: Label) {
        activity.masterSecretKey?.let { key ->
            val existing = LabelService.defaultHolder.lookupByLabelName(label.name)
            if (existing == null) {
                val encName = encryptCommonString(key, label.name)
                val encDesc = encryptCommonString(key, label.description)
                val encLabel = EncLabel(null, null, encName, encDesc, label.colorRGB)
                activity.labelViewModel.insert(encLabel, activity)
            }
        }
    }

    private fun addLabelToLabelGroup(label: Label) {
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
        } else {
            createAndAddChip(label, editCredentialLabelsChipGroup)

            // save label
            saveLabelIfNeeded(label)
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
        allLabelAdapter.remove(label)

        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            chipGroup.removeView(chip)
            allLabelAdapter.add(label)
        }
        return chip
    }

    private fun isCommittedLabelName(text: String): Boolean {
        if (text.isBlank()) {
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