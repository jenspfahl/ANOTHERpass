package de.jepfa.yapm.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.encrypted.EncryptedType.Types.*
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_ENCRYPTED_MASTER_PASSWORD
import de.jepfa.yapm.service.PreferenceService.PREF_FAST_MASTERPASSWD_LOGIN_WITH_NFC
import de.jepfa.yapm.service.PreferenceService.PREF_FAST_MASTERPASSWD_LOGIN_WITH_QRC
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.MasterPasswordService.generateEncMasterPasswdSK
import de.jepfa.yapm.service.secret.SaltService.getSalt
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.ALIAS_KEY_MP_TOKEN
import de.jepfa.yapm.service.secret.SecretService.decryptKey
import de.jepfa.yapm.service.secret.SecretService.encryptPassword
import de.jepfa.yapm.service.secret.SecretService.generateStrongSecretKey
import de.jepfa.yapm.service.secret.SecretService.getAndroidSecretKey
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.util.QRCodeUtil
import de.jepfa.yapm.util.QRCodeUtil.extractContentFromIntent
import de.jepfa.yapm.util.toastText


class LoginEnterMasterPasswordFragment : BaseFragment() {

    private lateinit var masterPasswdTextView: EditText
    private lateinit var loginButton: Button

    init {
        enableBack = true
        backToPreviousFragment = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login_enter_masterpassword, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)

        val loginActivity = getBaseActivity() as LoginActivity
        loginActivity.showTagDetectedMessage = false

        masterPasswdTextView = view.findViewById(R.id.edittext_enter_masterpassword)
        val switchStorePasswd: SwitchCompat = view.findViewById(R.id.switch_store_master_password)
        loginButton = view.findViewById(R.id.button_login)

        val scanQrCodeImageView: ImageView = view.findViewById(R.id.imageview_scan_qrcode)
        scanQrCodeImageView.setOnClickListener {
            QRCodeUtil.scanQRCode(this, getString(R.string.scanning_emp))
        }

        val scanNfcImageView: ImageView = view.findViewById(R.id.imageview_scan_nfc)
        if (!NfcService.isNfcAvailable(getBaseActivity())) {
            scanNfcImageView.visibility = View.GONE
        }
        scanNfcImageView.setOnClickListener {
            NfcService.scanNfcTag(this)
        }

        masterPasswdTextView.setOnEditorActionListener{ textView, id, keyEvent ->
            loginButton.performClick()
            true
        }

        loginButton.setOnClickListener {


            val masterPassword = Password.fromEditable(masterPasswdTextView.text)
            if (masterPassword.isEmpty()) {
                masterPasswdTextView.error = getString(R.string.password_required)
                masterPasswdTextView.requestFocus()
                return@setOnClickListener
            }

            val keyForTemp = getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)

            val encPinBase64 = arguments?.getString(CreateVaultActivity.ARG_ENC_PIN)
            if (encPinBase64 == null) {
                toastText(context, R.string.something_went_wrong)
                return@setOnClickListener
            }
            val encPin = Encrypted.fromBase64String(encPinBase64)
            val masterPin = SecretService.decryptPassword(keyForTemp, encPin)

            login( masterPin, masterPassword, switchStorePasswd.isChecked, loginActivity)
        }

        val scannedNdefTag = loginActivity.ndefTag?.data
        if (scannedNdefTag != null) {
            Log.i("LOGIN", "Tag available")

            val loginIntented = readAndUpdateMasterPassword(scannedNdefTag)
            if (loginIntented) {
                Log.i("LOGIN", "NDEF tag scanned and fast login granted")
                return
            }
        }

        if (isFastLoginWithQrCode()) {
            scanQrCodeImageView.performClick()
        }
        else if (isFastLoginWithNfcTag()) {
            scanNfcImageView.performClick()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val scanned = getScannedFromIntent(requestCode, resultCode, data)
        if (scanned != null) {
            readAndUpdateMasterPassword(scanned)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun readAndUpdateMasterPassword(scanned: String): Boolean {
        val encrypted = Encrypted.fromEncryptedBase64StringWithCheck(scanned)
        return when (encrypted?.type?.type) {
            ENC_MASTER_PASSWD -> readEMP(encrypted)
            MASTER_PASSWD_TOKEN -> readMPT(encrypted)
            else -> return false
        }

    }

    private fun readEMP(emp: Encrypted): Boolean {
        val baseActivity = getBaseActivity() ?: return false
        val empSK = generateEncMasterPasswdSK(baseActivity)
        val masterPassword =
            SecretService.decryptPassword(empSK, emp)
        if (!masterPassword.isValid()) {
            toastText(getBaseActivity(), R.string.invalid_emp)
            return false
        }

        masterPasswdTextView.setText(masterPassword.toRawFormattedPassword())
        masterPassword.clear()

        if (isFastLoginWithQrCode() || isFastLoginWithNfcTag()) {
            loginButton.performClick()
            return true
        }

        return false
    }

    private fun readMPT(mpt: Encrypted): Boolean {
        if (!PreferenceService.isPresent(
                PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY,
                getBaseActivity()
            )
        ) {
            toastText(getBaseActivity(), R.string.no_mpt_present)
            return true
        }
        // decrypt obliviously encrypted master password token
        val encMasterPasswordTokenKey = PreferenceService.getEncrypted(
            PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY,
            getBaseActivity()
        )
        encMasterPasswordTokenKey?.let {
            val masterPasswordTokenSK = getAndroidSecretKey(ALIAS_KEY_MP_TOKEN)
            val masterPasswordTokenKey =
                decryptKey(masterPasswordTokenSK, encMasterPasswordTokenKey)
            val baseActivity = getBaseActivity() ?: return false
            val salt = getSalt(baseActivity)
            val cipherAlgorithm = SecretService.getCipherAlgorithm(baseActivity)

            val mptSK = generateStrongSecretKey(masterPasswordTokenKey, salt, cipherAlgorithm)

            val masterPassword =
                SecretService.decryptPassword(mptSK, mpt)
            if (!masterPassword.isValid()) {
                toastText(getBaseActivity(), R.string.invalid_mpt)
                return false
            }

            masterPasswdTextView.setText(masterPassword.toRawFormattedPassword())
            masterPassword.clear()

            if (isFastLoginWithQrCode() || isFastLoginWithNfcTag()) {
                loginButton.performClick()
                return true
            }
        }
        return false
    }

    private fun getScannedFromIntent(requestCode: Int, resultCode: Int, data: Intent?): String? {
        if (requestCode == NfcActivity.ACTION_READ_NFC_TAG) {
            return data?.getStringExtra(NfcActivity.EXTRA_SCANNED_NDC_TAG_DATA)
        }
        else {
            return extractContentFromIntent(requestCode, resultCode, data)
        }
        return null
    }

    private fun login(
        userPin: Password,
        masterPassword: Password,
        isStoreMasterPassword: Boolean,
        loginActivity: LoginActivity
    ) {

        loginActivity.hideKeyboard(masterPasswdTextView)

        loginActivity.getProgressBar()?.let {

            UseCaseBackgroundLauncher(LoginUseCase)
                .launch(loginActivity, LoginData(userPin, masterPassword))
                { output ->
                    if (!output.success) {
                        loginActivity.handleFailedLoginAttempt()
                        masterPasswdTextView.error = "${getString(R.string.password_wrong)} ${loginActivity.getLoginAttemptMessage()}"
                        masterPasswdTextView.requestFocus()
                    } else {
                        if (isStoreMasterPassword) {
                            val keyForMP = getAndroidSecretKey(SecretService.ALIAS_KEY_MP)
                            val encPasswd = encryptPassword(keyForMP, masterPassword)
                            val baseActivity = getBaseActivity() ?: return@launch

                            PreferenceService.putEncrypted(DATA_ENCRYPTED_MASTER_PASSWORD, encPasswd, baseActivity)
                        }

                        userPin.clear()
                        masterPassword.clear()
                        masterPasswdTextView.setText("")
                        arguments?.remove(CreateVaultActivity.ARG_ENC_PIN)

                        loginActivity.loginSuccessful()
                    }
                }

        }

    }

    private fun isFastLoginWithQrCode(): Boolean {
        return  PreferenceService.getAsBool(PREF_FAST_MASTERPASSWD_LOGIN_WITH_QRC, getBaseActivity())
    }

    private fun isFastLoginWithNfcTag(): Boolean {
        return  PreferenceService.getAsBool(PREF_FAST_MASTERPASSWD_LOGIN_WITH_NFC, getBaseActivity())
    }

}