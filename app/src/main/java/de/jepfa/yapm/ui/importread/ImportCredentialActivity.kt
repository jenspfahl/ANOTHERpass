package de.jepfa.yapm.ui.importread

import android.content.Intent
import com.google.gson.JsonParser
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.export.DecryptedExportableCredential
import de.jepfa.yapm.model.export.EncExportableCredential
import de.jepfa.yapm.model.export.ExportContainer
import de.jepfa.yapm.model.export.PlainShareableCredential
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.credential.ShowCredentialActivity
import de.jepfa.yapm.usecase.vault.ImportVaultUseCase
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.toastText


class ImportCredentialActivity : ReadQrCodeOrNfcActivityBase() {

    init {
        readPlainTextFromNfc = true
    }

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
                val content = exportContainer.c
                when (content) {
                    is EncExportableCredential -> {
                        val ecr = content
                        getNameFromECR(ecr)?.let { name ->
                            return ecr.toEncCredential()
                        }
                    }
                    is Encrypted -> {
                        masterSecretKey?.let { key ->
                            val decryptedExportableCredential = try {
                                val decryptedExportedCredentialJsonString = SecretService.decryptCommonString(key, content)
                                val decryptedExportedCredentialJson = JsonParser.parseString(decryptedExportedCredentialJsonString)
                                DecryptedExportableCredential.fromJson(decryptedExportedCredentialJson)
                            } catch (e: Exception) {
                                DebugInfo.logException("ECR", "Cannot parse exported credential", e)
                                null
                            }
                            return decryptedExportableCredential?.toEncCredential(key)
                        }

                    }
                    is PlainShareableCredential -> {
                        val pcr = content
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