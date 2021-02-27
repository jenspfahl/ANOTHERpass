package de.jepfa.yapm.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.util.PreferenceUtil
import java.util.*


class LoginEnterMasterPasswordFragment : BaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_enter_masterpassword, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val masterPasswdTextView: EditText = view.findViewById(R.id.edittext_enter_masterpassword)
        val switchStorePasswd: Switch = view.findViewById(R.id.switch_store_master_password)

        view.findViewById<Button>(R.id.button_login).setOnClickListener {


            val masterPassword = Password.fromEditable(masterPasswdTextView.text)
            if (masterPassword.isEmpty()) {
                masterPasswdTextView.setError(getString(R.string.password_required))
                masterPasswdTextView.requestFocus()
            }
            else {

                val salt = SecretService.getOrCreateSalt(getBaseActivity())

                val keyForTemp = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)

                val encPinBase64 = arguments?.getString(CreateVaultActivity.ARG_ENC_PIN)!!
                val encPin = Encrypted.fromBase64String(encPinBase64)
                val masterPin = SecretService.decryptPassword(keyForTemp, encPin)

                val storedEncMasterKeyBase64 = PreferenceUtil.get(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, getBaseActivity())
                val storedEncMasterKey = Encrypted.fromBase64String(storedEncMasterKeyBase64!!)
                
                SecretService.login(masterPin, masterPassword, salt, storedEncMasterKey)


                if (switchStorePasswd.isChecked) {
                    val keyForMP = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MP)
                    val encPasswd = SecretService.encryptPassword(keyForMP, masterPassword)
                    PreferenceUtil.put(PreferenceUtil.PREF_MASTER_PASSWORD, encPasswd.toBase64String(), getBaseActivity())
                }


                findNavController().navigate(R.id.action_Login_MasterPasswordFragment_to_CredentialList)

                masterPin.clear()
                masterPassword.clear()
            }


        }
    }
}