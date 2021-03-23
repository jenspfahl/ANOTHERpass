package de.jepfa.yapm.ui.createvault

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService.ALIAS_KEY_TRANSPORT
import de.jepfa.yapm.service.encrypt.SecretService.encryptPassword
import de.jepfa.yapm.service.encrypt.SecretService.getAndroidSecretKey
import de.jepfa.yapm.service.secretgenerator.*
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.createvault.CreateVaultActivity.Companion.ARG_ENC_MASTER_PASSWD
import de.jepfa.yapm.util.putEncrypted


class CreateVaultEnterPassphraseFragment : BaseFragment() {

    private lateinit var pseudoPhraseSwitch: Switch
    private lateinit var generatedPasswdView: TextView
    private var generatedPassword: Password = Password.empty()
    private var passphraseGenerator = PassphraseGenerator()
    private var passwordGenerator = PasswordGenerator()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_vault_enter_passphrase, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pseudoPhraseSwitch = view.findViewById(R.id.switch_use_pseudo_phrase)
        generatedPasswdView = view.findViewById(R.id.generated_passwd)

        val buttonGeneratePasswd: Button = view.findViewById(R.id.button_generate_passwd)
        buttonGeneratePasswd.setOnClickListener {
            generatedPassword = generatePassword()
            generatedPasswdView.text = generatedPassword.debugToString()
        }

        val button = view.findViewById<Button>(R.id.button_next)
        button.setOnClickListener {
            if (generatedPassword.data.isEmpty()) {
                Toast.makeText(it.context, getString(R.string.generate_passwd_first), Toast.LENGTH_LONG).show()
            }
            else {
                val transSK = getAndroidSecretKey(ALIAS_KEY_TRANSPORT)
                val encPassword = encryptPassword(transSK, generatedPassword)
                generatedPassword.clear()

                val args = Bundle()
                args.putEncrypted(ARG_ENC_MASTER_PASSWD, encPassword)
                findNavController().navigate(R.id.action_Create_Vault_FirstFragment_to_SecondFragment, args)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val upIntent = Intent(getBaseActivity(), CreateVaultEnterPinFragment::class.java)
            getBaseActivity().navigateUpTo(upIntent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun generatePassword() : Password {
        //return Password("abcd") //TODO mockup

        if (pseudoPhraseSwitch.isChecked) {
            return passphraseGenerator.generate(
                    PassphraseGeneratorSpec(
                            strength = PassphraseStrength.EXTREME))
        }
        else {
            return passwordGenerator.generate(
                    PasswordGeneratorSpec(
                            strength = PasswordStrength.SUPER_STRONG))
        }

    }
}
