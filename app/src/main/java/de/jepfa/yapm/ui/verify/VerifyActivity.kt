package de.jepfa.yapm.ui.verify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.google.zxing.integration.android.IntentIntegrator
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.export.EncExportableCredential
import de.jepfa.yapm.model.export.ExportContainer
import de.jepfa.yapm.model.export.PlainShareableCredential
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.JsonService
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.MasterPasswordService.generateEncMasterPasswdSK
import de.jepfa.yapm.service.secret.MasterPasswordService.getMasterPasswordFromSession
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.ui.nfc.NfcBaseActivity
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.util.QRCodeUtil


class VerifyActivity : NfcBaseActivity() {

    private lateinit var verifyResultText :TextView
    private lateinit var scannedTextView : TextView

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }

        setContentView(R.layout.activity_verify)

        scannedTextView  = findViewById(R.id.text_scan_data)
        verifyResultText = findViewById(R.id.verify_status_text_result)

        val scanQrCodeImageView = findViewById<ImageView>(R.id.imageview_scan_qrcode)
        scanQrCodeImageView.setOnClickListener {
            QRCodeUtil.scanQRCode(this, getString(R.string.scanning_qrcode))
        }

        val scanNfcImageView: ImageView = findViewById(R.id.imageview_scan_nfc)
        if (!NfcService.isNfcAvailable(this)) {
            scanNfcImageView.visibility = View.GONE
        }
        scanNfcImageView.setOnClickListener {
            NfcService.scanNfcTag(this)
        }

    }

    override fun handleTag() {
        Log.i("VERIFY", "tag detected " + ndefTag?.tagId)
        if (ndefTag != null) {
            Toast.makeText(this, getString(R.string.nfc_tag_detected), Toast.LENGTH_LONG).show()
            ndefTag?.let {
                handleScannedData(it.data ?: "")
            }
        }
    }

    override fun lock() {
        recreate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val scanned = getScannedFromIntent(requestCode, resultCode, data)
        if (scanned != null) {
            handleScannedData(scanned)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun getScannedFromIntent(requestCode: Int, resultCode: Int, data: Intent?): String? {
        if (requestCode == NfcActivity.ACTION_READ_NFC_TAG) {
            return data?.getStringExtra(NfcActivity.EXTRA_SCANNED_NDC_TAG_DATA)
        }
        else {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null && result.contents != null) {
                return result.contents
            }
        }
        return null
    }

    private fun handleScannedData(scanned: String) {
        verifyResultText.text = getString(R.string.unknown_qr_code_or_nfc_tag)
        scannedTextView.text = ndefTag?.data
        if (Encrypted.isEncryptedBase64String(scanned)) {
            checkEncrypted(scanned)
        }
        else {
            checkContainer(scanned)
        }

    }

    private fun checkEncrypted(scanned: String) {
        if (scanned.startsWith(Encrypted.TYPE_ENC_MASTER_KEY)) {
            checkEMK(scanned)
        } else if (scanned.startsWith(Encrypted.TYPE_ENC_MASTER_PASSWD)) {
            checkEMP(scanned)
        } else if (scanned.startsWith(Encrypted.TYPE_MASTER_PASSWD_TOKEN)) {
            checkMPT(scanned)
        }
    }

    private fun checkEMK(scanned: String) {
        verifyResultText.text = getString(R.string.unknown_emk_scanned)

        val encStoredMasterKey =
            PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, this)
        val key = masterSecretKey
        if (key != null && encStoredMasterKey != null) {
            val mkKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)
            val encMasterKey = SecretService.decryptEncrypted(mkKey, encStoredMasterKey)
            val scannedEMK = Encrypted.fromBase64String(scanned)
            if (encMasterKey == scannedEMK) {
                verifyResultText.text = getString(R.string.well_known_emk_scanned)
            }
        }
    }

    private fun checkEMP(scanned: String) {
        verifyResultText.text = getString(R.string.unknown_emp_scanned)

        val salt = SaltService.getSalt(this)
        val cipherAlgorithm = SecretService.getCipherAlgorithm(this)
        val empSK = generateEncMasterPasswdSK(Password(salt.toCharArray()), cipherAlgorithm)
        val scannedMasterPassword =
            SecretService.decryptPassword(empSK, Encrypted.fromBase64String(scanned))
        val masterPassword = getMasterPasswordFromSession()
        if (masterPassword != null && scannedMasterPassword.isValid() && scannedMasterPassword.isEqual(
                masterPassword
            )
        ) {
            verifyResultText.text = getString(R.string.well_known_emp_scanned)
        }
        masterPassword?.clear()
    }

    private fun checkMPT(scanned: String) {
        verifyResultText.text = getString(R.string.unknown_mpt_scanned)

        val encMasterPasswordTokenKey = PreferenceService.getEncrypted(
            PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY,
            this
        )
        encMasterPasswordTokenKey?.let {
            val masterPasswordTokenSK =
                SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MP_TOKEN)
            val masterPasswordTokenKey =
                SecretService.decryptKey(masterPasswordTokenSK, encMasterPasswordTokenKey)
            val cipherAlgorithm = SecretService.getCipherAlgorithm(this)
            val mptSK = SecretService.generateStrongSecretKey(
                masterPasswordTokenKey,
                SaltService.getSalt(this),
                cipherAlgorithm
            )

            val encMasterPasswordFromScanned =
                SecretService.decryptPassword(mptSK, Encrypted.fromBase64String(scanned))
            val masterPassword = getMasterPasswordFromSession()
            if (masterPassword != null && encMasterPasswordFromScanned.isValid() && encMasterPasswordFromScanned.isEqual(
                    masterPassword
                )
            ) {
                verifyResultText.text = getString(R.string.well_known_mpt_scanned)
            }
            masterPassword?.clear()
            masterPasswordTokenKey.clear()
        }
    }

    private fun checkContainer(scanned: String) {
        val content = JsonService.parse(scanned)
        if (content != null) {
            ExportContainer.fromJson(content)?.let { exportContainer ->
                when (exportContainer.c) {
                    is EncExportableCredential -> {
                        verifyResultText.text = getString(R.string.unknown_ecr_scanned)
                        val ecr = exportContainer.c
                        if (isKnownECR(ecr)) {
                            verifyResultText.text = getString(R.string.well_known_ecr_scanned)
                        }
                    }
                    is PlainShareableCredential -> {
                        verifyResultText.text = getString(R.string.pcr_scanned)
                    }
                }
            }
        }

    }

    private fun isKnownECR(ecr: EncExportableCredential): Boolean {
        val encPasswd = ecr.p
        masterSecretKey?.let{key ->
            val passwd = SecretService.decryptPassword(key, encPasswd)
            val isKnown = passwd.isValid()
            passwd.clear()
            return isKnown
        }
        return false
    }
}