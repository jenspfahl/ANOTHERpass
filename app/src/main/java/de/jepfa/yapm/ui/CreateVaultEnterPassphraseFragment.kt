package de.jepfa.yapm.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.secretgenerator.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.PassphraseGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.PasswordStrength


class CreateVaultEnterPassphraseFragment : BaseFragment() {

    private lateinit var generatedPasswdView: TextView

    private var generatedPassword: Password = Password("")

    private val passphraseGenerator = PassphraseGenerator()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_vault_enter_passphrase, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generatedPasswdView = view.findViewById(R.id.generated_passwd)

        val secretService = getApp().secretService

        val buttonGeneratePasswd: Button = view.findViewById(R.id.button_generate_passwd)
        buttonGeneratePasswd.setOnClickListener {
            generatedPassword = generatePassword()
            generatedPasswdView.text = generatedPassword.debugToString()
        }

        val button = view.findViewById<Button>(R.id.button_next)
        button.setOnClickListener {
            if (generatedPassword.data.isEmpty()) {
                Toast.makeText(it.context, "Generate a password first", Toast.LENGTH_LONG).show()
            }
            else {
                val key = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_TEMP)
                val encPassword = secretService.encryptPassword(key, generatedPassword)
                generatedPassword.clear()

                val args = Bundle()
                args.putString(CreateVaultActivity.ARG_ENC_PASSWD, encPassword.toBase64String())
                findNavController().navigate(R.id.action_Create_Vault_FirstFragment_to_SecondFragment, args)

            }
        }

    }

    private fun generatePassword() : Password {
        return passphraseGenerator.generatePassphrase(
                PassphraseGeneratorSpec(
                        strength = PasswordStrength.EXTREME))
    }
}