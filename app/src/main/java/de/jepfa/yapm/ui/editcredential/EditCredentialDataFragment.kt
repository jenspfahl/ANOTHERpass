package de.jepfa.yapm.ui.editcredential

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.Button
import android.widget.EditText
import androidx.navigation.fragment.findNavController
import com.pchmn.materialchips.ChipsInput
import com.pchmn.materialchips.ChipsInput.ChipsListener
import com.pchmn.materialchips.model.ChipInterface
import de.jepfa.yapm.R
import de.jepfa.yapm.model.*
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.decryptCommonString
import de.jepfa.yapm.service.secret.SecretService.encryptCommonString
import de.jepfa.yapm.ui.SecureFragment
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.ui.label.LabelChip
import de.jepfa.yapm.usecase.LockVaultUseCase
import javax.crypto.SecretKey


class EditCredentialDataFragment : SecureFragment() {

    private val LAST_CHARS = listOf(',', ';', ' ', System.lineSeparator())

    private lateinit var editCredentialNameView: EditText
    private lateinit var editCredentialLabelsView: ChipsInput
    private lateinit var editCredentialUserView: EditText
    private lateinit var editCredentialWebsiteView: EditText
    private lateinit var editCredentialAdditionalInfoView: EditText

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
        setHasOptionsMenu(true)

        val editCredentialActivity = getBaseActivity() as EditCredentialActivity

        editCredentialNameView = view.findViewById(R.id.edit_credential_name)
        editCredentialLabelsView = view.findViewById(R.id.edit_credential_labels)
        editCredentialUserView = view.findViewById(R.id.edit_credential_user)
        editCredentialWebsiteView = view.findViewById(R.id.edit_credential_website)
        editCredentialAdditionalInfoView = view.findViewById(R.id.edit_credential_additional_info)

        getBaseActivity().labelViewModel.allLabels.observe(getSecureActivity(), { labels ->
            val key = masterSecretKey
            if (key != null) {
                //LabelService.init(key, labels.toSet(), getBaseActivity())
                editCredentialLabelsView.filterableList = LabelService.getAllLabelChips()
            }
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
                        LabelService.updateLabel(key, encLabel)
                    }
                }
            }

            override fun onChipRemoved(chip: ChipInterface, newSize: Int) {
            }

            override fun onTextChanged(text: CharSequence) {
                if (text.isNotBlank() && isCommitLabel(text)) {
                    val labelName = text.substring(0, text.length - 1)
                    val label = LabelChip(labelName, "")
                    editCredentialLabelsView.addChip(label)
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
                    editCredentialNameView.setText(name)
                    editCredentialUserView.setText(user)
                    editCredentialWebsiteView.setText(website)
                    editCredentialAdditionalInfoView.setText(additionalInfo)

                    LabelService.updateLabelsForCredential(key, originCredential)

                    LabelService.getLabelsForCredential(key, originCredential).forEachIndexed { idx, it ->
                        editCredentialLabelsView.addChip(it.labelChip)
                    }
                }
            })
        }

        editCredentialNameView.requestFocus()

        val buttonNext: Button = view.findViewById(R.id.button_next)
        buttonNext.setOnClickListener {
            Session.safeTouch()

            if (TextUtils.isEmpty(editCredentialNameView.text)) {
                editCredentialNameView.setError(getString(R.string.error_field_required))
                editCredentialNameView.requestFocus()
            } else {

                val key = masterSecretKey
                if (key != null) {
                    val name = editCredentialNameView.text.toString()
                    val additionalInfo = editCredentialAdditionalInfoView.text.toString()
                    val user = editCredentialUserView.text.toString()
                    val website = editCredentialWebsiteView.text.toString()

                    val encName = encryptCommonString(key, name)
                    val encAdditionalInfo = encryptCommonString(key, additionalInfo)
                    val encUser = encryptCommonString(key, user)
                    val encPassword = SecretService.encryptPassword(key, Password.empty())
                    val encWebsite = encryptCommonString(key, website)
                    val encLabels = LabelService.encryptLabelIds(key, editCredentialLabelsView.selectedChipList)

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val upIntent = Intent(getBaseActivity(), ListCredentialsActivity::class.java)
            getBaseActivity().navigateUpTo(upIntent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}