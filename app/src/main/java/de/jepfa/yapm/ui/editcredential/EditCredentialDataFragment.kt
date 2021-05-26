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
            LockVaultUseCase.execute(getSecureActivity())
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
            Toast.makeText(
                getBaseActivity(),
                "Debug mode " + if (DebugInfo.isDebug) "ON" else "OFF",
                Toast.LENGTH_LONG
            ).show()
            true
        }


        getBaseActivity().labelViewModel.allLabels.observe(getSecureActivity(), { labels ->
            val key = masterSecretKey
            if (key != null) {
                editCredentialLabelsView.filterableList = LabelService.getAllLabelChips()
            }
            editCredentialNameView.requestFocus()
        })


        editCredentialLabelsView.addChipsListener(object : ChipsListener {
            override fun onChipAdded(chip: ChipInterface, newSize: Int) {

                val key = masterSecretKey
                if (key != null && editCredentialLabelsView.filterableList != null) {
                    val exising = LabelService.lookupByLabelName(chip.label)
                    if (exising == null) {
                        val encName = encryptCommonString(key, chip.label)
                        val encDesc = encryptCommonString(key, "")
                        val encLabel = EncLabel(null, encName, encDesc, null)
                        getBaseActivity().labelViewModel.insert(encLabel)
                    }
                }
            }

            override fun onChipRemoved(chip: ChipInterface, newSize: Int) {
            }

            override fun onTextChanged(text: CharSequence) {

                if (text.isNotBlank() && isCommitLabel(text)) {

                    val chipsCount = editCredentialLabelsView.selectedChipList.size
                    if (chipsCount > Constants.MAX_LABELS_PER_CREDENTIAL) {
                        Toast.makeText(
                            getBaseActivity(),
                            "Maximum of labels reached (${Constants.MAX_LABELS_PER_CREDENTIAL})",
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (text.length > Constants.MAX_LABEL_LENGTH) {
                        Toast.makeText(
                            getBaseActivity(),
                            "Label too long (max ${Constants.MAX_LABEL_LENGTH})", Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val labelName = text.substring(0, text.length - 1)
                        val label = LabelChip(labelName, "")
                        editCredentialLabelsView.addChip(label)
                    }
                }
            }
        })

        //fill UI
        if (editCredentialActivity.isUpdate()) {
            editCredentialActivity.load().observe(getSecureActivity(), {
                val originCredential = it
                val key = masterSecretKey
                if (key != null) {
                    val name = decryptCommonString(key, originCredential.name)
                    val user = decryptCommonString(key, originCredential.user)
                    val website = decryptCommonString(key, originCredential.website)
                    val additionalInfo = decryptCommonString(
                        key,
                        originCredential.additionalInfo
                    )

                    getBaseActivity().setTitle(getString(R.string.title_change_credential_with_title, name))

                    editCredentialNameView.setText(name)
                    editCredentialUserView.setText(user)
                    editCredentialWebsiteView.setText(website)
                    editCredentialAdditionalInfoView.setText(additionalInfo)

                    LabelService.updateLabelsForCredential(key, originCredential)

                    LabelService.getLabelsForCredential(key, originCredential)
                        .forEachIndexed { idx, it ->
                            editCredentialLabelsView.addChip(it.labelChip)
                        }
                    editCredentialNameView.requestFocus()
                }
            })
        }

        val buttonNext: Button = view.findViewById(R.id.button_next)
        buttonNext.setOnClickListener {

            if (TextUtils.isEmpty(editCredentialNameView.text)) {
                editCredentialNameView.setError(getString(R.string.error_field_required))
                editCredentialNameView.requestFocus()
            } else {

                val key = masterSecretKey
                if (key != null) {
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
                        encWebsite,
                        encLabels
                    )
                    editCredentialActivity.current = credentialToSave

                    findNavController().navigate(R.id.action_EditCredential_DataFragment_to_PasswordFragment)

                }
            }
        }
    }

    private fun isCommitLabel(text: CharSequence): Boolean {
        val lastChar = text.last()
        return lastChar in LAST_CHARS
    }

}