package de.jepfa.yapm.ui.createvault

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService.ALIAS_KEY_TRANSPORT
import de.jepfa.yapm.service.encrypt.SecretService.encryptPassword
import de.jepfa.yapm.service.encrypt.SecretService.getAndroidSecretKey
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.createvault.CreateVaultActivity.Companion.ARG_ENC_MASTER_PASSWD
import de.jepfa.yapm.ui.createvault.CreateVaultActivity.Companion.ARG_ENC_PIN

class CreateVaultEnterPinFragment : BaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_vault_enter_pin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pin1TextView: EditText = view.findViewById(R.id.first_pin)
        val pin2TextView: EditText = view.findViewById(R.id.second_pin)

        view.findViewById<Button>(R.id.button_next).setOnClickListener {

            val pin1 = Password.fromEditable(pin1TextView.text)
            val pin2 = Password.fromEditable(pin2TextView.text)

            if (pin1.isEmpty()) {
                pin1TextView.setError(getString(R.string.pin_required))
                pin1TextView.requestFocus()
            }
            else if (pin1.length < 4) {
                pin1TextView.setError(getString(R.string.pin_too_short))
                pin1TextView.requestFocus()
            }
            else if (! pin1.isEqual(pin2)) {
                pin2TextView.setError(getString(R.string.pin_not_equal))
                pin2TextView.requestFocus()
            }
            else {
                val transSK = getAndroidSecretKey(ALIAS_KEY_TRANSPORT)
                val encPin = encryptPassword(transSK, pin1)
                val encMasterPasswd = arguments?.getString(ARG_ENC_MASTER_PASSWD)
                val args = Bundle()
                args.putString(ARG_ENC_MASTER_PASSWD, encMasterPasswd)
                args.putString(ARG_ENC_PIN, encPin.toBase64String())

                findNavController().navigate(R.id.action_Create_Vault_SecondFragment_to_ThirdFragment, args)
            }

            pin1.clear()
            pin2.clear()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val upIntent = Intent(getBaseActivity(), CreateVaultActivity::class.java)
            getBaseActivity().navigateUpTo(upIntent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}