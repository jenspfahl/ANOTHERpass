package de.jepfa.yapm.ui.editcredential

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.NavUtils.navigateUpTo
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secretgenerator.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.PasswordGenerator
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.SecureFragment
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.usecase.LoginUseCase
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.AsyncWithProgressBar
import de.jepfa.yapm.util.PasswordColorizer
import de.jepfa.yapm.util.putEncrypted
import de.jepfa.yapm.viewmodel.CredentialViewModel
import de.jepfa.yapm.viewmodel.CredentialViewModelFactory
import java.util.*


class EditCredentialDataFragment : SecureFragment() {

    private lateinit var editCredentialNameView: EditText
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
        editCredentialUserView = view.findViewById(R.id.edit_credential_user)
        editCredentialWebsiteView = view.findViewById(R.id.edit_credential_website)
        editCredentialAdditionalInfoView = view.findViewById(R.id.edit_credential_additional_info)

        //fill UI
        if (editCredentialActivity.isUpdate()) {
            editCredentialActivity.load().observe(getSecureActivity(), {
                val originCredential = it
                val key = masterSecretKey
                if (key != null) {
                    val name = SecretService.decryptCommonString(key, originCredential.name)
                    val user = SecretService.decryptCommonString(key, originCredential.user)
                    val website = SecretService.decryptCommonString(key, originCredential.website)
                    val additionalInfo = SecretService.decryptCommonString(key, originCredential.additionalInfo)
                    editCredentialNameView.setText(name)
                    editCredentialUserView.setText(user)
                    editCredentialWebsiteView.setText(website)
                    editCredentialAdditionalInfoView.setText(additionalInfo)
                }
            })
        }

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

                    val encName = SecretService.encryptCommonString(key, name)
                    val encAdditionalInfo = SecretService.encryptCommonString(key, additionalInfo)
                    val encUser = SecretService.encryptCommonString(key, user)
                    val encPassword = SecretService.encryptPassword(key, Password.empty())
                    val encWebsite = SecretService.encryptCommonString(key, website)
                    val encLabels = Collections.singleton(SecretService.encryptCommonString(key, "")) //TODO

                    val credentialToSave = EncCredential(
                        editCredentialActivity.currentId, encName, encAdditionalInfo, encUser, encPassword, encWebsite, encLabels)
                    editCredentialActivity.current = credentialToSave

                    findNavController().navigate(R.id.action_EditCredential_DataFragment_to_PasswordFragment)

                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val upIntent = Intent(getBaseActivity(), ListCredentialsActivity::class.java)
            getBaseActivity().navigateUpTo(upIntent)
            return true
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}