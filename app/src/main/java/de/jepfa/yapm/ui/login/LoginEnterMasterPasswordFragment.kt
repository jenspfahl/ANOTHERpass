package de.jepfa.yapm.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.fragment.findNavController
import com.google.zxing.integration.android.IntentIntegrator
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.QRCodeUtil


class LoginEnterMasterPasswordFragment : BaseFragment() {

    private lateinit var masterPasswdTextView: EditText

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_enter_masterpassword, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)

        getBaseActivity().supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        masterPasswdTextView = view.findViewById(R.id.edittext_enter_masterpassword)
        val scanQrCodeImageView: ImageView = view.findViewById(R.id.imageview_scan_qrcode)
        val switchStorePasswd: Switch = view.findViewById(R.id.switch_store_master_password)
        val loginButton = view.findViewById<Button>(R.id.button_login)

        scanQrCodeImageView.setOnClickListener {
            QRCodeUtil.scanQRCode(this, "Scanning Master Password")
            true
        }


        masterPasswdTextView.setOnEditorActionListener{ textView, id, keyEvent ->
            loginButton.performClick()
            true
        }

        loginButton.setOnClickListener {


            val masterPassword = Password.fromEditable(masterPasswdTextView.text)
            if (masterPassword.isEmpty()) {
                masterPasswdTextView.setError(getString(R.string.password_required))
                masterPasswdTextView.requestFocus()
                return@setOnClickListener
            }

            val salt = SecretService.getOrCreateSalt(getBaseActivity())
            val keyForTemp = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)

            val encPinBase64 = arguments?.getString(CreateVaultActivity.ARG_ENC_PIN)
            if (encPinBase64 == null) {
                Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val encPin = Encrypted.fromBase64String(encPinBase64)
            val masterPin = SecretService.decryptPassword(keyForTemp, encPin)

            val encStoredMasterKey = PreferenceUtil.getEncrypted(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, getBaseActivity())
            if (encStoredMasterKey == null) {
                Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val success = SecretService.login(
                masterPin,
                masterPassword,
                encStoredMasterKey,
                salt
            )
            if (!success) {
                masterPasswdTextView.setError(getString(R.string.password_wrong))
                masterPasswdTextView.requestFocus()
                (getBaseActivity() as LoginActivity).handleFailedLoginAttempt()
                return@setOnClickListener
            }

            if (switchStorePasswd.isChecked) {
                val keyForMP = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MP)
                val encPasswd = SecretService.encryptPassword(keyForMP, masterPassword)
                PreferenceUtil.put(PreferenceUtil.PREF_ENCRYPTED_MASTER_PASSWORD, encPasswd.toBase64String(), getBaseActivity())
            }

            findNavController().navigate(R.id.action_Login_to_CredentialList)

            masterPin.clear()
            masterPassword.clear()
            masterPasswdTextView.setText("")
            arguments?.remove(CreateVaultActivity.ARG_ENC_PIN)
            (getBaseActivity() as LoginActivity).loginSuccessful()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {

            findNavController().navigate(R.id.action_Login_MasterPasswordFragment_to_Login_PinFragment)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let {
                masterPasswdTextView.setText(it)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}