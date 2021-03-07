package de.jepfa.yapm.ui.login

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Key
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.util.PreferenceUtil
import java.util.*


class LoginEnterPinFragment : BaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login_enter_pin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBaseActivity().supportActionBar?.setDisplayHomeAsUpEnabled(false)

        val pinTextView: EditText = view.findViewById(R.id.edittext_enter_pin)
        val nextButton = view.findViewById<Button>(R.id.button_login_next)

        pinTextView.setImeOptions(EditorInfo.IME_ACTION_DONE)
        pinTextView.setOnEditorActionListener{ textView, id, keyEvent ->
            nextButton.performClick()
            true
        }

        nextButton.setOnClickListener {

            val salt = SecretService.getOrCreateSalt(getBaseActivity())
            val keyForHPin = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_HPIN)
            val keyForTemp = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)
            val encStoredMasterPinHash = PreferenceUtil.getEncrypted(PreferenceUtil.PREF_HASHED_MASTER_PIN, getBaseActivity())
            if (encStoredMasterPinHash == null) {
                Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val storedMasterPinHash = SecretService.decryptKey(keyForHPin, encStoredMasterPinHash)

            val userPin = Password.fromEditable(pinTextView.text)
            if (userPin.isEmpty()) {
                pinTextView.setError(getString(R.string.pin_required))
                pinTextView.requestFocus()

                return@setOnClickListener
            }
/*
            val userPinHash = SecretService.hashPassword(userPin, salt)

            if (!Arrays.equals(userPinHash.data, storedMasterPinHash.data)) {
                pinTextView.setError(getString(R.string.pin_wrong))
                pinTextView.requestFocus()

                return@setOnClickListener
            }*/

            val encStoredMasterPasswd = PreferenceUtil.getEncrypted(PreferenceUtil.PREF_ENCRYPTED_MASTER_PASSWORD, getBaseActivity())

            if (!Secret.isLoggedOut()) {
                val keyForTemp = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)
                val encMasterPasswd = Secret.getEncMasterPasswd()
                if (encMasterPasswd == null) {
                    Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val masterPasswd = SecretService.decryptPassword(keyForTemp, encMasterPasswd)

                if (!login(userPin, masterPasswd, salt)) {
                    pinTextView.setError(getString(R.string.pin_wrong))
                    pinTextView.requestFocus()
                    return@setOnClickListener
                }
            }
            else if (encStoredMasterPasswd != null) {

                val keyForMP = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MP)
                val storedMasterPasswd = SecretService.decryptPassword(keyForMP, encStoredMasterPasswd)

                if (!login(userPin, storedMasterPasswd, salt)) {
                    pinTextView.setError(getString(R.string.pin_wrong))
                    pinTextView.requestFocus()
                    return@setOnClickListener
                }
            } else {
                val encUserPin = SecretService.encryptPassword(keyForTemp, userPin)
                val args = Bundle()
                args.putString(CreateVaultActivity.ARG_ENC_PIN, encUserPin.toBase64String())

                findNavController().navigate(R.id.action_Login_PinFragment_to_MasterPasswordFragment, args)
            }

          //  userPinHash.clear()
            userPin.clear()
            storedMasterPinHash.clear()
        }
    }

    private fun login(
            userPin: Password,
            masterPasswd: Password,
            salt: Key
    ): Boolean {
        val encStoredMasterKey =
            PreferenceUtil.getEncrypted(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, getBaseActivity())
        if (encStoredMasterKey == null) {
            Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
            return true
        }

        val success = SecretService.login(
                userPin,
                masterPasswd,
                encStoredMasterKey,
                salt
        )
        if (!success) {
           // Toast.makeText(context, R.string.password_wrong, Toast.LENGTH_LONG).show()
            return false
        }

        masterPasswd.clear()
        findNavController().navigate(R.id.action_Login_MasterPasswordFragment_to_CredentialList)
        return true
    }
}