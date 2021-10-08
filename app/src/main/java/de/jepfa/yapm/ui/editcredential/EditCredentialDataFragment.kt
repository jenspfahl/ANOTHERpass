package de.jepfa.yapm.ui.editcredential

import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.pchmn.materialchips.ChipsInput
import com.pchmn.materialchips.ChipsInput.ChipsListener
import com.pchmn.materialchips.model.ChipInterface
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
import de.jepfa.yapm.ui.label.LabelChip
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.toastText


class EditCredentialDataFragment : SecureFragment() {

    private val LAST_CHARS = listOf(' ', '\t', System.lineSeparator())

    private lateinit var editCredentialNameView: EditText
    private lateinit var editCredentialLabelsView: ChipsInput
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
        editCredentialLabelsView = view.findViewById(R.id.edit_credential_labels)
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

        getBaseActivity()?.let {
            it.labelViewModel.allLabels.observe(it, { labels ->
                masterSecretKey?.let{ key ->
                    // Wrapping the result of getAllLabelChips into an ArrayList is a hack to
                    // avoid sorting a SingletonList (which is returned if size==1)
                    // inside com.pchmn.materialchips - lib
                    // which would fail with a UnsupportedOperationException
                    editCredentialLabelsView.filterableList = ArrayList(LabelService.getAllLabelChips())
                }
                editCredentialNameView.requestFocus()
            })
        }

        editCredentialLabelsView.addChipsListener(object : ChipsListener {
            override fun onChipAdded(chip: ChipInterface, newSize: Int) {

                val key = masterSecretKey
                if (key != null && editCredentialLabelsView.filterableList != null) {
                    val exising = LabelService.lookupByLabelName(chip.label)
                    if (exising == null) {
                        val encName = encryptCommonString(key, chip.label)
                        val encDesc = encryptCommonString(key, "")
                        val encLabel = EncLabel(null, encName, encDesc, null)
                        getBaseActivity()?.labelViewModel?.insert(encLabel)
                    }
                }
            }

            override fun onChipRemoved(chip: ChipInterface, newSize: Int) {
            }

            override fun onTextChanged(text: CharSequence) {
                addLabel(text, editCredentialActivity)
            }
        })

        /*
        TODO doesn't work since editCredentialLabelsView.editText return a new ChipsInputEditText ll the time.
         The origin is encapsulated in ChipsAdapter which is encapsulated in ChipsInput.
         Need to fork that lib and change it,
        editCredentialLabelsView.editText.setOnFocusChangeListener { view, focusGained ->
            if (!focusGained) {
                addLabel(editCredentialLabelsView.editText.editableText.toString(), editCredentialActivity)
            }
        }*/

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

                    getBaseActivity()?.title = getString(R.string.title_change_credential_with_title, name)

                    editCredentialNameView.setText(name)
                    editCredentialUserView.setText(user)
                    editCredentialWebsiteView.setText(website)
                    editCredentialAdditionalInfoView.setText(additionalInfo)

                    LabelService.updateLabelsForCredential(key, it)

                    LabelService.getLabelsForCredential(key, it)
                        .forEachIndexed { idx, label ->
                            editCredentialLabelsView.addChip(label.labelChip)
                            editCredentialLabelsView.selectedChipList + label.labelChip
                        }
                    editCredentialNameView.requestFocus()
                }
            })
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
                        editCredentialLabelsView.selectedChipList
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
                        editCredentialActivity.original?.isLastPasswordObfuscated ?: false
                    )
                    editCredentialActivity.current = credentialToSave

                    findNavController().navigate(R.id.action_EditCredential_DataFragment_to_PasswordFragment)

                }
            }
        }
    }

    private fun addLabel(
        text: CharSequence,
        editCredentialActivity: EditCredentialActivity
    ) {
        if (text.isNotBlank() && isCommitLabel(text)) {

            val maxLabelLength =
                editCredentialActivity.resources.getInteger(R.integer.max_label_name_length)
            val chipsCount = editCredentialLabelsView.selectedChipList.size
            if (chipsCount >= Constants.MAX_LABELS_PER_CREDENTIAL) {
                toastText(
                    getBaseActivity(),
                    getString(R.string.max_labels_reached, Constants.MAX_LABELS_PER_CREDENTIAL)
                )
            } else if (text.length > maxLabelLength) {
                toastText(
                    getBaseActivity(),
                    getString(R.string.label_too_long, maxLabelLength)
                )
            } else {
                val labelName = text.substring(0, text.length - 1)
                val label = LabelChip(labelName, "")
                editCredentialLabelsView.addChip(label)
            }
        }
    }

    private fun isCommitLabel(text: CharSequence): Boolean {
        val lastChar = text.last()
        return lastChar in LAST_CHARS
    }

}