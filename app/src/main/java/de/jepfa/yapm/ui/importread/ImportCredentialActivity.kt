package de.jepfa.yapm.ui.importread

import android.content.Intent
import android.widget.*
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.export.EncExportableCredential
import de.jepfa.yapm.model.export.ExportContainer
import de.jepfa.yapm.model.export.PlainShareableCredential
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.credential.ShowCredentialActivity
import de.jepfa.yapm.usecase.vault.ImportVaultUseCase
import de.jepfa.yapm.util.toUUIDFromBase64String
import de.jepfa.yapm.util.toastText


class ImportCredentialActivity : ReadActivityBase() {

    override fun getLayoutId(): Int = R.layout.activity_import_credential

    override fun handleScannedData(scanned: String) {
        val credential = extractCredential(scanned)
        if (credential == null) {
            toastText(this, R.string.qr_code_or_nfc_tag_not_a_credential)
        }
        else {
            startShowDetailActivity(credential)
        }
    }

    private fun extractCredential(scanned: String): EncCredential? {
        val parsedVault = ImportVaultUseCase.parseVaultFileContent(scanned, this)
        if (parsedVault.content != null) {
            ExportContainer.fromJson(parsedVault.content)?.let { exportContainer ->
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
            return pcr.toEncCredential(key)
        }
        return null
    }


    private fun startShowDetailActivity(credential: EncCredential) {
        val intent = Intent(this, ShowCredentialActivity::class.java)
        credential.applyExtras(intent)
        intent.putExtra(ShowCredentialActivity.EXTRA_MODE, ShowCredentialActivity.EXTRA_MODE_SHOW_EXTERNAL_FROM_RECORD)
        startActivity(intent)
    }
}