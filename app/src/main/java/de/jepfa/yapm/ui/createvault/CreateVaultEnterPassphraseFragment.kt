package de.jepfa.yapm.ui.createvault

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.AndroidKey.ALIAS_KEY_TRANSPORT
import de.jepfa.yapm.service.secret.SecretService.encryptPassword
import de.jepfa.yapm.service.secret.SecretService.getAndroidSecretKey
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.createvault.CreateVaultActivity.Companion.ARG_ENC_MASTER_PASSWD
import de.jepfa.yapm.usecase.secret.GenerateMasterPasswordUseCase
import de.jepfa.yapm.util.PasswordColorizer
import de.jepfa.yapm.util.putEncrypted
import de.jepfa.yapm.util.toastText


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

                val args = Bundle()
                args.putEncrypted(ARG_ENC_MASTER_PASSWD, encPassword)
                findNavController().navigate(R.id.action_Create_Vault_FirstFragment_to_SecondFragment, args)
            }
        }
    }

}
