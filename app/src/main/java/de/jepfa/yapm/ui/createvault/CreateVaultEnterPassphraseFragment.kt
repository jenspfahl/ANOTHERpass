package de.jepfa.yapm.ui.createvault

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.AndroidKey.ALIAS_KEY_TRANSPORT
import de.jepfa.yapm.service.secret.SecretService.encryptPassword
import de.jepfa.yapm.service.secret.SecretService.getAndroidSecretKey
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.createvault.CreateVaultActivity.Companion.ARG_ENC_MASTER_PASSWD
import de.jepfa.yapm.usecase.secret.GenerateMasterPasswordUseCase
import de.jepfa.yapm.util.*


class CreateVaultEnterPassphraseFragment : BaseFragment() {

    private var generatedPassword: Password = Password.empty()

    init {
        enableBack = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_vault_enter_passphrase, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(R.string.create_vault_enter_passphrase_fragment_label)

        val pseudoPhraseSwitch: SwitchCompat = view.findViewById(R.id.switch_use_pseudo_phrase)
        val generatedPasswdView: TextView = view.findViewById(R.id.generated_passwd)

        val buttonGeneratePasswd: Button = view.findViewById(R.id.button_generate_passwd)
        if (DebugInfo.isDebug) {
            buttonGeneratePasswd.setOnLongClickListener {

                val input = EditText(it.context)
                input.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                input.setText(generatedPassword.toRawFormattedPassword(), TextView.BufferType.EDITABLE)

                val filters = arrayOf<InputFilter>(InputFilter.LengthFilter(Constants.MAX_CREDENTIAL_PASSWD_LENGTH))
                input.setFilters(filters)

                AlertDialog.Builder(it.context)
                    .setTitle(R.string.edit_password)
                    .setMessage(R.string.edit_password_message)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        generatedPassword = Password(input.text)
                        var spannedString = PasswordColorizer.spannableString(generatedPassword, it.context)
                        generatedPasswdView.text = spannedString
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, which ->
                        dialog.cancel()
                    }
                    .show()

                true
            }
        }

        buttonGeneratePasswd.setOnClickListener {
           getBaseActivity()?.let { baseActivity ->
               generatedPassword = GenerateMasterPasswordUseCase.execute(pseudoPhraseSwitch.isChecked, baseActivity).data
               var spannedString = PasswordColorizer.spannableString(generatedPassword, getBaseActivity())
               generatedPasswdView.text = spannedString
           }
        }

        val button = view.findViewById<Button>(R.id.button_next)
        button.setOnClickListener {
            if (generatedPassword.data.isEmpty()) {
                toastText(it.context, R.string.generate_password_first)
            }
            else {
                val transSK = getAndroidSecretKey(ALIAS_KEY_TRANSPORT, view.context)
                val encPassword = encryptPassword(transSK, generatedPassword)
                generatedPassword.clear()

                PreferenceService.putCurrentDate(PreferenceService.DATA_MP_MODIFIED_AT, button.context)

                val args = Bundle()
                args.putEncrypted(ARG_ENC_MASTER_PASSWD, encPassword)
                findNavController().navigate(R.id.action_Create_Vault_FirstFragment_to_SecondFragment, args)
            }
        }
    }

}
