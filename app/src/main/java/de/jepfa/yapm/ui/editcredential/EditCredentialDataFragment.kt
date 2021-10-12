package de.jepfa.yapm.ui.editcredential

import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.*
import androidx.core.view.iterator
import androidx.core.view.size
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

import de.jepfa.yapm.R
import de.jepfa.yapm.model.*
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.decryptCommonString
import de.jepfa.yapm.service.secret.SecretService.encryptCommonString
import de.jepfa.yapm.ui.SecureFragment
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.util.*
import kotlin.collections.ArrayList


class EditCredentialDataFragment : SecureFragment() {

    private val LAST_CHARS = listOf(' ', '\t', System.lineSeparator())

    private lateinit var editCredentialNameView: EditText
    private lateinit var editCredentialLabelsTextView: AutoCompleteTextView
    private lateinit var editCredentialLabelsChipGroup: ChipGroup
    private lateinit var editCredentialUserView: EditText
    private lateinit var editCredentialWebsiteView: EditText
    private lateinit var editCredentialAdditionalInfoView: EditText

    init {
        enableBack = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (Session.isDenied()) {
            getSecureActivity()?.let { LockVaultUseCase.execute(it) }
            return null
        }
        return inflater.inflate(R.layout.fragment_edit_credential_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)

        val editCredentialActivity = getBaseActivity() as EditCredentialActivity

        editCredentialNameView = view.findViewById(R.id.edit_credential_name)
        editCredentialLabelsTextView = view.findViewById(R.id.edit_credential_labels_textview)
        editCredentialLabelsChipGroup = view.findViewById(R.id.edit_credential_labels_chipgroup)
        editCredentialUserView = view.findViewById(R.id.edit_credential_user)
        editCredentialWebsiteView = view.findViewById(R.id.edit_credential_website)
        editCredentialAdditionalInfoView = view.findViewById(R.id.edit_credential_additional_info)

        val explanationView: TextView = view.findViewById(R.id.edit_credential_explanation)
        explanationView.setOnLongClickListener {
            DebugInfo.toggleDebug()
            toastText(
                getBaseActivity(),
                "Debug mode " + if (DebugInfo.isDebug) "ON" else "OFF"
            )
            true
        }

        val allLabelAdapter = ArrayAdapter(
            editCredentialActivity,
            android.R.layout.simple_dropdown_item_1line,
            ArrayList<String>()
        )

        editCredentialLabelsTextView.setAdapter(allLabelAdapter)
        editCredentialLabelsTextView.setOnItemClickListener { parent, _, position, _ ->
            editCredentialLabelsTextView.text = null
            val selected = parent.getItemAtPosition(position) as String

            addChipToLabelGroup(
                selected,
                editCredentialLabelsChipGroup,
                allLabelAdapter)
        }

        editCredentialLabelsTextView.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val unfinishedText = editCredentialLabelsTextView.text.toString()
                addChipToLabelGroup(
                    unfinishedText,
                    editCredentialLabelsChipGroup,
                    allLabelAdapter)
                editCredentialLabelsTextView.text = null
            }
        }
        editCredentialLabelsTextView.doAfterTextChanged {
            val committedText = it.toString()
            if (isCommittedLabelName(committedText)) {
                val labelName = clearCommittedLabelName(committedText)
                addChipToLabelGroup(
                    labelName,
                    editCredentialLabelsChipGroup,
                    allLabelAdapter)
                editCredentialLabelsTextView.text = null
            }
        }
        val allLabels = LabelService.getAllLabels()

        //fill UI
        if (editCredentialActivity.isUpdate()) {
            editCredentialActivity.load().observe(editCredentialActivity, {
                editCredentialActivity.original = it
                masterSecretKey?.let{ key ->
                    val name = decryptCommonString(key, it.name)
                    val user = decryptCommonString(key, it.user)
                    val website = decryptCommonString(key, it.website)
                    val additionalInfo = decryptCommonString(
                        key,
                        it.additionalInfo
                    )

                    val enrichedName = enrichId(editCredentialActivity, name, it.id)
                    editCredentialActivity.title = getString(R.string.title_change_credential_with_title, enrichedName)

                    editCredentialNameView.setText(name)
                    editCredentialUserView.setText(user)
                    editCredentialWebsiteView.setText(website)
                    editCredentialAdditionalInfoView.setText(additionalInfo)

                    editCredentialNameView.requestFocus()

                    LabelService.updateLabelsForCredential(key, it)
                    val allLabelsForCredential = LabelService.getLabelsForCredential(key, it)

                    val labelSuggestions = allLabels
                        .filterNot { allLabelsForCredential.contains(it) }
                        .map { it.name }

                    allLabelAdapter.addAll(labelSuggestions)

                    allLabelsForCredential
                        .forEachIndexed { _, label ->
                            createAndAddChip(label, editCredentialLabelsChipGroup, allLabelAdapter)
                        }

                }
            })
        }
        else {
            val labelSuggestions = allLabels
                .map { it.name }
            allLabelAdapter.addAll(labelSuggestions)
        }

        val buttonNext: Button = view.findViewById(R.id.button_next)
        buttonNext.setOnClickListener {

            if (TextUtils.isEmpty(editCredentialNameView.text)) {
                editCredentialNameView.error = getString(R.string.error_field_required)
                editCredentialNameView.requestFocus()
            } else {

                masterSecretKey?.let{ key ->
                    val name = editCredentialNameView.text.toString().trim()
                    val additionalInfo = editCredentialAdditionalInfoView.text.toString()
                    val user = editCredentialUserView.text.toString().trim()
                    val website = editCredentialWebsiteView.text.toString().trim()

                    val encName = encryptCommonString(key, name)
                    val encAdditionalInfo = encryptCommonString(key, additionalInfo)
                    val encUser = encryptCommonString(key, user)
                    val encPassword = SecretService.encryptPassword(key, Password.empty())
                    val encWebsite = encryptCommonString(key, website)
                    val encLabels = LabelService.encryptLabelIds(
                        key,
                        mapToLabelName(editCredentialLabelsChipGroup)
                    )

                    val credentialToSave = EncCredential(
                        editCredentialActivity.currentId,
                        encName,
                        encAdditionalInfo,
                        encUser,
                        encPassword,
                        editCredentialActivity.original?.lastPassword,
                        encWebsite,
                        encLabels,
                        editCredentialActivity.original?.isObfuscated ?: false,
                        editCredentialActivity.original?.isLastPasswordObfuscated ?: false,
                        null
                    )
                    editCredentialActivity.current = credentialToSave

                    findNavController().navigate(R.id.action_EditCredential_DataFragment_to_PasswordFragment)

                }
            }
        }
    }

    private fun mapToLabelName(chipGroup: ChipGroup): List<String> {
        val chipNames = ArrayList<String>(chipGroup.size)
        chipGroup.iterator().forEach {
            val chip = it as Chip
            chipNames.add(chip.text.toString())
        }
        return chipNames.toList()
    }

    private fun addChipToLabelGroup(
        labelName: String,
        chipGroup: ChipGroup,
        allLabelAdapter: ArrayAdapter<String>
    ) {
        if (labelName.isNotBlank()) {
            val label = LabelService.lookupByLabelName(labelName) ?: Label(labelName)
            val maxLabelLength = resources.getInteger(R.integer.max_label_name_length)
            val chipsCount = chipGroup.size
            if (containsLabel(chipGroup, label)) {
                toastText(
                    context,
                    R.string.label_already_in_ist)
            }
            else if (chipsCount >= Constants.MAX_LABELS_PER_CREDENTIAL) {
                toastText(
                    context,
                    getString(R.string.max_labels_reached, Constants.MAX_LABELS_PER_CREDENTIAL)
                )
            } else if (label.name.length > maxLabelLength) {
                toastText(
                    context,
                    getString(R.string.label_too_long, maxLabelLength)
                )
            } else {
                createAndAddChip(label, chipGroup, allLabelAdapter)

                // save label
                masterSecretKey?.let { key ->
                    val existing = LabelService.lookupByLabelName(label.name)
                    if (existing == null) {
                        val encName = encryptCommonString(key, label.name)
                        val encDesc = encryptCommonString(key, "")
                        val encLabel = EncLabel(null, encName, encDesc, null)
                        getBaseActivity()?.labelViewModel?.insert(encLabel)
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
        allLabelAdapter: ArrayAdapter<String>
    ): Chip {
        val chip = createAndAddLabelChip(label, chipGroup, context)
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