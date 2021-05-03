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
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.ALIAS_KEY_MP_TOKEN
import de.jepfa.yapm.service.secret.SecretService.decryptKey
import de.jepfa.yapm.service.secret.SecretService.encryptPassword
import de.jepfa.yapm.service.secret.SecretService.generateSecretKey
import de.jepfa.yapm.service.secret.SecretService.getAndroidSecretKey
import de.jepfa.yapm.service.secret.SecretService.getSalt
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.usecase.LoginUseCase
import de.jepfa.yapm.util.AsyncWithProgressBar
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.PreferenceUtil.DATA_ENCRYPTED_MASTER_PASSWORD
import de.jepfa.yapm.util.PreferenceUtil.PREF_FAST_MASTERPASSWD_LOGIN
import de.jepfa.yapm.util.QRCodeUtil


class LoginEnterMasterPasswordFragment : BaseFragment() {

    private lateinit var masterPasswdTextView: EditText
    private lateinit var loginButton: Button

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login_enter_masterpassword, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)

        getBaseActivity().supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        masterPasswdTextView = view.findViewById(R.id.edittext_enter_masterpassword)
        val scanQrCodeImageView: ImageView = view.findViewById(R.id.imageview_scan_qrcode)
        val switchStorePasswd: Switch = view.findViewById(R.id.switch_store_master_password)
        loginButton = view.findViewById(R.id.button_login)

        scanQrCodeImageView.setOnClickListener {
            QRCodeUtil.scanQRCode(this, "Scanning Master Password")
            true
        }

        if (isFastLogin()) {
            scanQrCodeImageView.performClick()
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

            val keyForTemp = getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)

            val encPinBase64 = arguments?.getString(CreateVaultActivity.ARG_ENC_PIN)
            if (encPinBase64 == null) {
                Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val encPin = Encrypted.fromBase64String(encPinBase64)
            val masterPin = SecretService.decryptPassword(keyForTemp, encPin)
            val loginActivity = getBaseActivity() as LoginActivity

            login( masterPin, masterPassword, switchStorePasswd.isChecked, loginActivity)
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
        if (result != null && result.contents != null) {
            val scanned = result.contents

            if (scanned.startsWith(Encrypted.TYPE_MASTER_PASSWD_TOKEN)) {
                if (!PreferenceUtil.isPresent(PreferenceUtil.DATA_MASTER_PASSWORD_TOKEN_KEY, getBaseActivity())) {
                    Toast.makeText(getBaseActivity(), "No master password token present.", Toast.LENGTH_LONG).show()
                    return
                }
                // decrypt obliviously encrypted master password token
                val encMasterPasswordTokenKey = PreferenceUtil.getEncrypted(PreferenceUtil.DATA_MASTER_PASSWORD_TOKEN_KEY, getBaseActivity())
                encMasterPasswordTokenKey?.let {
                    val masterPasswordTokenSK = getAndroidSecretKey(ALIAS_KEY_MP_TOKEN)
                    val masterPasswordTokenKey = decryptKey(masterPasswordTokenSK, encMasterPasswordTokenKey)
                    val mptSK = generateSecretKey(masterPasswordTokenKey, getSalt(getBaseActivity()))

                    val encMasterPassword = SecretService.decryptPassword(mptSK, Encrypted.fromBase64String(scanned))
                    if (!encMasterPassword.isValid()) {
                        Toast.makeText(getBaseActivity(), "Invalid master password token", Toast.LENGTH_LONG).show()
                        return
                    }

                    masterPasswdTextView.setText(encMasterPassword)

                    if (isFastLogin()) {
                        loginButton.performClick()
                    }
                }
            }
            else if (scanned.startsWith(Encrypted.TYPE_ENC_MASTER_PASSWD)) {

                val salt = getSalt(getBaseActivity())
                val saltSK = SecretService.generateFastSecretKey(salt, salt)
                val encMasterPassword = SecretService.decryptPassword(saltSK, Encrypted.fromBase64String(scanned))
                if (!encMasterPassword.isValid()) {
                    Toast.makeText(getBaseActivity(), "Invalid encrypted master password", Toast.LENGTH_LONG).show()
                    return
                }

                masterPasswdTextView.setText(encMasterPassword)

                if (isFastLogin()) {
                    loginButton.performClick()
                }
            }
            else {
                Toast.makeText(getBaseActivity(), "Unknown master password", Toast.LENGTH_LONG).show()

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun login(
        userPin: Password,
        masterPassword: Password,
        isStoreMasterPassword: Boolean,
        loginActivity: LoginActivity
    ) {

        loginActivity.hideKeyboard(masterPasswdTextView)

        loginActivity.getProgressBar()?.let {

            AsyncWithProgressBar(
                loginActivity,
                {
                    LoginUseCase.execute(
                        userPin,
                        masterPassword,
                        getBaseActivity()
                    )
                },
                { success ->
                    if (!success) {
                        loginActivity.handleFailedLoginAttempt()
                        masterPasswdTextView.setError("${getString(R.string.password_wrong)} ${loginActivity.getLoginAttemptMessage()}")
                        masterPasswdTextView.requestFocus()
                    } else {
                        if (isStoreMasterPassword) {
                            val keyForMP = getAndroidSecretKey(SecretService.ALIAS_KEY_MP)
                            val encPasswd = encryptPassword(keyForMP, masterPassword)
                            PreferenceUtil.putEncrypted(DATA_ENCRYPTED_MASTER_PASSWORD, encPasswd, getBaseActivity())
                        }

                        userPin.clear()
                        masterPassword.clear()
                        masterPasswdTextView.setText("")
                        arguments?.remove(CreateVaultActivity.ARG_ENC_PIN)

                        loginActivity.loginSuccessful()
                    }
                }
            )

        }

    }

    private fun isFastLogin(): Boolean {
        return  PreferenceUtil.getAsBool(PREF_FAST_MASTERPASSWD_LOGIN, false, getBaseActivity())
    }

}