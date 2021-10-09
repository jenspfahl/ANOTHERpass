package de.jepfa.yapm.ui.importread

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.google.zxing.integration.android.IntentIntegrator
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.model.encrypted.EncCredential
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
import de.jepfa.yapm.ui.credential.ShowCredentialActivity
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.ui.nfc.NfcBaseActivity
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.util.QRCodeUtil
import de.jepfa.yapm.util.toastText


class ImportCredentialActivity : ReadActivityBase() {

    override fun getLayoutId(): Int = R.layout.activity_import_credential

    override fun handleScannedData(scanned: String) {
        val credential = extractCredential(scanned)
        if (credential == null) {
            toastText(this, R.string.unknown_qr_code_or_nfc_tag)
        }
        else {
            startShowDetailActivity(credential)
        }
    }

    private fun extractCredential(scanned: String): EncCredential? {
        val content = JsonService.parse(scanned)
        if (content != null) {
            ExportContainer.fromJson(content)?.let { exportContainer ->
                when (exportContainer.c) {
                    is EncExportableCredential -> {
                        val ecr = exportContainer.c
                        getNameFromECR(ecr)?.let { name ->
                            return ecr.toEncCredential()
                        }
                    }
                    is PlainShareableCredential -> {
                        val pcr = exportContainer.c
                        return createEncCredentialFromPcr(pcr)
                    }
                    else -> return@let
                }
            }
        }
        return null
    }

    private fun createEncCredentialFromPcr(pcr: PlainShareableCredential): EncCredential? {
        masterSecretKey?.let {  key ->

            val encName = SecretService.encryptCommonString(key, pcr.n)
            val encAdditionalInfo = SecretService.encryptCommonString(key, pcr.aI)
            val encUser = SecretService.encryptCommonString(key, pcr.u)
            val encPasswd = SecretService.encryptPassword(key, pcr.p)
            val encWebsite = SecretService.encryptCommonString(key, pcr.w)
            val encLabels = SecretService.encryptCommonString(key, "")

            pcr.p.clear()

            val credential = EncCredential(
                null,
                encName,
                encAdditionalInfo,
                encUser,
                encPasswd,
                null,
                encWebsite,
                encLabels,
                false,
                null,
                null
            )
            return credential
        }
        return null
    }


    private fun startShowDetailActivity(credential: EncCredential) {
        val intent = Intent(this, ShowCredentialActivity::class.java)
        credential.applyExtras(intent)
        intent.putExtra(ShowCredentialActivity.EXTRA_MODE, ShowCredentialActivity.EXTRA_MODE_SHOW_EXTERNAL)
        startActivity(intent)
    }
}