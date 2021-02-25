package de.jepfa.yapm.ui

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Password
import java.util.*

class CreateVaultEnterPinFragment : BaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_vault_enter_pin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pin1: EditText = view.findViewById(R.id.first_pin)
        val pin2: EditText = view.findViewById(R.id.second_pin)

        view.findViewById<Button>(R.id.button_next).setOnClickListener {

            val p1 = Password.fromEditable(pin1.text)
            val p2 = Password.fromEditable(pin2.text)

            if (pin1.text.isNullOrBlank()) {
                pin1.setError(getString(R.string.pin_required))
                pin1.requestFocus()
            }
            else if (pin1.text.length < 4) {
                pin2.setError(getString(R.string.pin_too_short))
                pin2.requestFocus()
            }
            else if (! Arrays.equals(p1.data, p2.data)) {
                pin2.setError(getString(R.string.pin_not_equal))
                pin2.requestFocus()
            }
            else {

                val secretService = getApp().secretService

                val key = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_TEMP)
                val encPin = secretService.encryptPassword(key, Password.fromEditable(pin1.text))
                val encPasswd = arguments?.getString(CreateVaultActivity.ARG_ENC_PASSWD)
                val args = Bundle()
                args.putString(CreateVaultActivity.ARG_ENC_PASSWD, encPasswd)
                args.putString(CreateVaultActivity.ARG_ENC_PIN, encPin.toBase64String())

                findNavController().navigate(R.id.action_Create_Vault_SecondFragment_to_ThirdFragment)
            }

            p1.clear()
            p2.clear()
        }
    }

}