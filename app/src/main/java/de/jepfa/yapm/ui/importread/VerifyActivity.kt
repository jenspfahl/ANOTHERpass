package de.jepfa.yapm.ui.importread

import android.os.Bundle
import android.widget.*
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.encrypted.EncryptedType.Types.*
import de.jepfa.yapm.model.export.EncExportableCredential
import de.jepfa.yapm.model.export.ExportContainer
import de.jepfa.yapm.model.export.PlainShareableCredential
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.MasterPasswordService.generateEncMasterPasswdSKForExport
import de.jepfa.yapm.service.secret.MasterPasswordService.getMasterPasswordFromSession
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.credential.DeobfuscationDialog


class VerifyActivity : ReadActivityBase() {

    private lateinit var verifyResultText :TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verifyResultText = findViewById(R.id.verify_status_text_result)

    }

    override fun getLayoutId(): Int = R.layout.activity_verify

    override fun handleScannedData(scanned: String) {
        verifyResultText.text = getString(R.string.unknown_qr_code_or_nfc_tag)
        val encrypted = Encrypted.fromEncryptedBase64StringWithCheck(scanned)
        if (encrypted != null) {
            checkEncrypted(encrypted)
        }
        else {
            checkContainer(scanned)
        }

    }

    private fun checkEncrypted(encrypted: Encrypted) {
        when (encrypted.type?.type) {
            ENC_MASTER_KEY -> checkEMK(encrypted)
            ENC_MASTER_PASSWD -> checkEMP(encrypted)
            MASTER_PASSWD_TOKEN -> checkMPT(encrypted)
        }
    }

    private fun checkEMK(emk: Encrypted) {
        verifyResultText.text = getString(R.string.unknown_emk_scanned)

        val encStoredMasterKey =
            PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, this)
        val key = masterSecretKey
        if (key != null && encStoredMasterKey != null) {
            val mkKey = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MK, this)
            val encMasterKey = SecretService.decryptEncrypted(mkKey, encStoredMasterKey)
            if (encMasterKey == emk) {
                verifyResultText.text = getString(R.string.well_known_emk_scanned)
            }
        }
    }

    private fun checkEMP(emp: Encrypted) {
        verifyResultText.text = getString(R.string.unknown_emp_scanned)

        val empSK = generateEncMasterPasswdSKForExport(this)
        val scannedMasterPassword = SecretService.decryptPassword(empSK, emp)

        if (scannedMasterPassword.isValid()) {
            if (MasterPasswordService.isProtectedEMP(emp)) {
                DeobfuscationDialog.openDeobfuscationDialogForMasterPassword(this) { deobfuscationKey ->
                    deobfuscationKey?.let { scannedMasterPassword.deobfuscate(it) }
                    verifyScannedEMP(scannedMasterPassword)
                }
            } else {
                verifyScannedEMP(scannedMasterPassword)
            }
        }
    }

    private fun verifyScannedEMP(scannedMasterPassword: Password) {
        val masterPassword = getMasterPasswordFromSession(this)
        if (masterPassword != null
            && scannedMasterPassword.isEqual(masterPassword)) {
            verifyResultText.text = getString(R.string.well_known_emp_scanned)
        }
        masterPassword?.clear()
        scannedMasterPassword.clear()
    }

    private fun checkMPT(mpt: Encrypted) {
        verifyResultText.text = getString(R.string.unknown_mpt_scanned)

        val encMasterPasswordTokenKey = PreferenceService.getEncrypted(
            PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY,
            this
        )
        encMasterPasswordTokenKey?.let {
            val masterPasswordTokenSK =
                SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MP_TOKEN, this)
            val masterPasswordTokenKey =
                SecretService.decryptKey(masterPasswordTokenSK, encMasterPasswordTokenKey)
            val cipherAlgorithm = SecretService.getCipherAlgorithm(this)
            val mptSK = SecretService.generateStrongSecretKey(
                masterPasswordTokenKey,
                SaltService.getSalt(this),
                cipherAlgorithm
            )

            val decMPT =
                SecretService.decryptPassword(mptSK, mpt)
            val masterPassword = getMasterPasswordFromSession(this)
            if (masterPassword != null && decMPT.isValid() && decMPT.isEqual(
                    masterPassword
                )
            ) {
                verifyResultText.text = getString(R.string.well_known_mpt_scanned, mpt.type?.payload ?: "")
            }
            masterPassword?.clear()
            masterPasswordTokenKey.clear()
        }
    }

    private fun checkContainer(scanned: String) {
        val content = VaultExportService.parseVaultFileContent(scanned)
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
                        val pcr = exportContainer.c
                        verifyResultText.text = getString(R.string.pcr_scanned, pcr.n)
                    }
                    else -> return@let
                }
            }
        }

    }

}