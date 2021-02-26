package de.jepfa.yapm.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.util.PreferenceUtil
import java.util.*


class LoginEnterPinFragment : BaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_enter_pin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val secretService = getApp().secretService

        val pinTextView: EditText = view.findViewById(R.id.edittext_enter_pin)

        view.findViewById<Button>(R.id.button_login_next).setOnClickListener {

            val salt = secretService.getOrCreateSalt(getBaseActivity())
            val keyForHPin = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_HPIN)
            val keyForTemp = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_TEMP)
            val encStoredMasterPinHashBase64 = PreferenceUtil.get(PreferenceUtil.PREF_HASHED_MASTER_PIN, getBaseActivity())
            if (encStoredMasterPinHashBase64 != null) {
                val encStoredMasterPinHash = Encrypted.fromBase64String(encStoredMasterPinHashBase64)
                val storedMasterPinHash = secretService.decryptKey(keyForHPin, encStoredMasterPinHash)

                val userPin = Password.fromEditable(pinTextView.text)
                val userPinHash = secretService.hashPassword(userPin, salt)

                if (pinTextView.text.isNullOrBlank()) {
                    pinTextView.setError(getString(R.string.pin_required))
                    pinTextView.requestFocus()
                }
                else if (! Arrays.equals(userPinHash.data, storedMasterPinHash.data)) {
                    pinTextView.setError(getString(R.string.pin_wrong))
                    pinTextView.requestFocus()
                }
                else {
                    val storedMasterPasswd = PreferenceUtil.get(PreferenceUtil.PREF_MASTER_PASSWORD, getBaseActivity())
                    if (storedMasterPasswd != null) {

                        val keyForMP = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_MP)

                        val encMasterPasswd = Encrypted.fromBase64String(storedMasterPasswd)
                        val decMasterPasswd = secretService.decryptPassword(keyForMP, encMasterPasswd)

                        secretService.login(userPin, decMasterPasswd, salt)
                        findNavController().navigate(R.id.action_Login_SecondFragment_to_List)
                    }
                    else {
                        val encUserPin = secretService.encryptPassword(keyForTemp, userPin)
                        val args = Bundle()
                        args.putString(CreateVaultActivity.ARG_ENC_PIN, encUserPin.toBase64String())

                        findNavController().navigate(R.id.action_Login_FirstFragment_to_SecondFragment, args)

                    }
                }

                userPin.clear()
                userPinHash.clear()
                storedMasterPinHash.clear()
            }

        }
    }
}