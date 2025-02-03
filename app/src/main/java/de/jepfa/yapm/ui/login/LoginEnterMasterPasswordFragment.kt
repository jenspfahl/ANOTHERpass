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
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.getAndroidSecretKey
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import de.jepfa.yapm.util.QRCodeUtil
import de.jepfa.yapm.util.QRCodeUtil.extractContentFromIntent
import de.jepfa.yapm.util.toastText


class LoginEnterMasterPasswordFragment : BaseFragment() {

    private lateinit var masterPasswdTextView: EditText
    private lateinit var loginButton: Button
    private var isFromQRScan = false


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
        switchStorePasswd.isChecked = MasterPasswordService.isMasterPasswordStored(loginActivity)

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


            val masterPassword = Password(masterPasswdTextView.text)
            if (masterPassword.isEmpty()) {
                masterPasswdTextView.error = getString(R.string.password_required)
                masterPasswdTextView.requestFocus()
                return@setOnClickListener
            }

            val encPinBase64 = arguments?.getString(CreateVaultActivity.ARG_ENC_PIN)
            if (encPinBase64 == null) {
                toastText(context, R.string.something_went_wrong)
                return@setOnClickListener
            }

            val keyForTemp = getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, view.context)

            val encPin = Encrypted.fromBase64String(encPinBase64)
            val pin = SecretService.decryptPassword(keyForTemp, encPin)

            if (switchStorePasswd.isChecked) {
                val masterPasswdAlreadyStored = MasterPasswordService.isMasterPasswordStored(loginActivity)
                if (masterPasswdAlreadyStored) {
                    login(pin, masterPassword, loginActivity)
                }
                else {
                    MasterPasswordService.storeMasterPassword(masterPassword, loginActivity,
                        {
                            login(pin, masterPassword, loginActivity)
                            toastText(loginActivity, R.string.masterpassword_stored)
                        },
                        {
                            toastText(loginActivity, R.string.masterpassword_not_stored)
                        })
                }
            }
            else {
                MasterPasswordService.deleteStoredMasterPassword(loginActivity)
                login(pin, masterPassword, loginActivity)
            }
        }

        val scannedNdefTag = loginActivity.ndefTag?.data
        if (scannedNdefTag != null) {
            Log.i(LOG_PREFIX + "LOGIN", "Tag available")
            isFromQRScan = false
            readAndUpdateMasterPassword(scannedNdefTag, loginActivity, loginActivity.isFastLoginWithNfcTag())

        }
        else {
            if (loginActivity.isFastLoginWithQrCode()) {
                scanQrCodeImageView.performClick()
            } else if (loginActivity.isFastLoginWithNfcTag()) {
                scanNfcImageView.performClick()
            }
        }

        getBaseActivity()?.hideKeyboard(masterPasswdTextView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val loginActivity = getBaseActivity() as LoginActivity
        val scanned = getScannedFromIntent(requestCode, resultCode, data, loginActivity)
        if (scanned != null) {
            readAndUpdateMasterPassword(scanned, loginActivity,
                (requestCode == NfcActivity.ACTION_READ_NFC_TAG && loginActivity.isFastLoginWithNfcTag()
                        || (requestCode != NfcActivity.ACTION_READ_NFC_TAG && loginActivity.isFastLoginWithQrCode())))
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }


    fun updateMasterKeyFromNfcTag() {
        val loginActivity = getBaseActivity() as LoginActivity

        val scannedNdefTag = loginActivity.ndefTag?.data
        if (scannedNdefTag != null) {
            Log.i(LOG_PREFIX + "LOGIN", "Tag available")
            isFromQRScan = false
            readAndUpdateMasterPassword(scannedNdefTag, loginActivity, loginActivity.isFastLoginWithNfcTag())
        }
    }


    private fun readAndUpdateMasterPassword(scanned: String, loginActivity: LoginActivity, isFastLogin: Boolean) {
        loginActivity.readMasterPassword(scanned, isFromQRScan)
        { masterPassword ->
            masterPassword?.let {
                masterPasswdTextView.setText(masterPassword.toRawFormattedPassword())
                masterPassword.clear()

                if (isFastLogin) {
                    loginButton.performClick()
                }
            }
        }


    }


    private fun getScannedFromIntent(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        loginActivity: LoginActivity
    ): String? {
        if (requestCode == NfcActivity.ACTION_READ_NFC_TAG) {
            isFromQRScan = false
            loginActivity.readTagFromIntent(data) // important to update the tag instance
            return data?.getStringExtra(NfcActivity.EXTRA_SCANNED_NDC_TAG_DATA)
        }
        else {
            isFromQRScan = true
            return extractContentFromIntent(requestCode, resultCode, data)
        }
    }

    private fun login(
        userPin: Password,
        masterPassword: Password,
        loginActivity: LoginActivity
    ) {

        masterPasswdTextView.isEnabled = false
        loginActivity.hideKeyboard(masterPasswdTextView)

        loginActivity.getProgressBar()?.let {

            UseCaseBackgroundLauncher(LoginUseCase)
                .launch(loginActivity, LoginData(userPin, masterPassword))
                { output ->
                    if (!output.success) {
                        masterPasswdTextView.isEnabled = true

                        MasterPasswordService.deleteStoredMasterPassword(loginActivity)
                        loginActivity.handleFailedLoginAttempt()
                        masterPasswdTextView.error = "${getString(R.string.password_wrong)} ${loginActivity.getLoginAttemptMessage()}"
                        masterPasswdTextView.requestFocus()
                    } else {
                        cleanupAndLogin(userPin, masterPassword, loginActivity)
                    }
                }

        }

    }

    private fun cleanupAndLogin(
        userPin: Password,
        masterPassword: Password,
        loginActivity: LoginActivity
    ) {
        userPin.clear()
        masterPassword.clear()
        masterPasswdTextView.setText("")
        arguments?.remove(CreateVaultActivity.ARG_ENC_PIN)

        loginActivity.loginSuccessful()
    }

}