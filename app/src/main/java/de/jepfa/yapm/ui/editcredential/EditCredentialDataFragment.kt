@file:Suppress("KotlinDeprecation")

package de.jepfa.yapm.ui.editcredential

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.decryptCommonString
import de.jepfa.yapm.service.secret.SecretService.encryptCommonString
import de.jepfa.yapm.ui.SecureFragment
import de.jepfa.yapm.ui.label.LabelEditViewExtender
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.toastText


class EditCredentialDataFragment : SecureFragment() {

    private lateinit var editCredentialActivity: EditCredentialActivity
    private lateinit var labelEditViewExtender: LabelEditViewExtender

    private lateinit var editCredentialNameView: EditText
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

        editCredentialActivity = getBaseActivity() as EditCredentialActivity

        editCredentialNameView = view.findViewById(R.id.edit_credential_name)
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

        labelEditViewExtender = LabelEditViewExtender(editCredentialActivity, view)

        //fill UI
        val current = editCredentialActivity.current

        if (current != null) {
            editCredentialActivity.hideKeyboard(editCredentialNameView)

            masterSecretKey?.let{ key ->
                fillUi(key, current)
            }
        }
        else if (editCredentialActivity.isUpdate()) {
            editCredentialActivity.hideKeyboard(editCredentialNameView)

            editCredentialActivity.load().observe(editCredentialActivity) { orig ->
                editCredentialActivity.original = orig
                masterSecretKey?.let { key ->
                    editCredentialActivity.updateTitle(orig)
                    fillUi(key, orig)
                }
            }
        }
        else {
            editCredentialNameView.requestFocus()
        }

        val buttonNext: Button = view.findViewById(R.id.button_next)
        buttonNext.setOnClickListener {

            if (TextUtils.isEmpty(editCredentialNameView.text)) {
                editCredentialNameView.error = getString(R.string.error_field_required)
                editCredentialNameView.requestFocus()
            } else {

                masterSecretKey?.let{ key ->
                    saveCurrentUiData(key)

                    findNavController().navigate(R.id.action_EditCredential_DataFragment_to_PasswordFragment)

                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (isVisible) {
            masterSecretKey?.let { key ->
                saveCurrentUiData(key)
            }
        }
    }

    private fun fillUi(
        key: SecretKeyHolder,
        current: EncCredential
    ) {
        val name = decryptCommonString(key, current.name)
        val user = decryptCommonString(key, current.user)
        val website = decryptCommonString(key, current.website)
        val additionalInfo = decryptCommonString(
            key,
            current.additionalInfo
        )


        editCredentialNameView.setText(name)
        editCredentialUserView.setText(user)
        editCredentialWebsiteView.setText(website)
        editCredentialAdditionalInfoView.setText(additionalInfo)

        val allLabelsForCredential = LabelService.defaultHolder.decryptLabelsForCredential(key, current)

        labelEditViewExtender.addPersistedLabels(allLabelsForCredential)


    }

    private fun saveCurrentUiData(
        key: SecretKeyHolder
    ) {
        val name = editCredentialNameView.text.toString().trim()
        val additionalInfo = editCredentialAdditionalInfoView.text.toString()
        val user = editCredentialUserView.text.toString().trim()
        val website = editCredentialWebsiteView.text.toString().trim()

        val encName = encryptCommonString(key, name)
        val encAdditionalInfo = encryptCommonString(key, additionalInfo)
        val encUser = encryptCommonString(key, user)
        val encPassword = editCredentialActivity.current?.password
            ?: editCredentialActivity.original?.password
            ?: SecretService.encryptPassword(key, Password.empty()
        )
        val encWebsite = encryptCommonString(key, website)
        val encLabels = LabelService.defaultHolder.encryptLabelIds(
            key,
            labelEditViewExtender.getCommitedLabelNames()
        )

        // we create the new credential out of a former current if present or else out of the original if present
        val credentialToSave = EncCredential(
            editCredentialActivity.currentId,
            editCredentialActivity.original?.uid,
            encName,
            encAdditionalInfo,
            encUser,
            encPassword,
            editCredentialActivity.original?.lastPassword,
            encWebsite,
            encLabels,
            editCredentialActivity.current?.isObfuscated
                ?: editCredentialActivity.original?.isObfuscated
                ?: false,
            editCredentialActivity.original?.isLastPasswordObfuscated
                ?: false,
            null
        )
        editCredentialActivity.current = credentialToSave
    }

}