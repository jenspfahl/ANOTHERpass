package de.jepfa.yapm.ui.importread

import android.os.Bundle
import android.widget.*
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.export.EncExportableCredential
import de.jepfa.yapm.model.export.ExportContainer
import de.jepfa.yapm.model.export.PlainShareableCredential
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.JsonService
import de.jepfa.yapm.service.secret.MasterPasswordService.generateEncMasterPasswdSK
import de.jepfa.yapm.service.secret.MasterPasswordService.getMasterPasswordFromSession
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService


class VerifyActivity : ReadActivityBase() {

    private lateinit var verifyResultText :TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verifyResultText = findViewById(R.id.verify_status_text_result)

    }

    override fun getLayoutId(): Int = R.layout.activity_verify

    override fun handleScannedData(scanned: String) {
        verifyResultText.text = getString(R.string.unknown_qr_code_or_nfc_tag)
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
                        getNameFromECR(ecr)?.let { name ->
                            verifyResultText.text = getString(R.string.well_known_ecr_scanned, name)
                        }
                    }
                    is PlainShareableCredential -> {
                        verifyResultText.text = getString(R.string.pcr_scanned)
                    }
                    else -> return@let
                }
            }
        }

    }

}