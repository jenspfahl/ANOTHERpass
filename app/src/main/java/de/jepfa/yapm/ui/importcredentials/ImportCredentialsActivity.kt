package de.jepfa.yapm.ui.importcredentials

import android.os.Bundle
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.vault.ImportVaultUseCase
import de.jepfa.yapm.usecase.vault.ImportVaultUseCase.parseVaultFileContent

class ImportCredentialsActivity : SecureActivity() {

//    var parsedVault: ImportVaultUseCase.ParsedVault? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        savedInstanceState?.getString("JSON")?.let {
  //          parsedVault = parseVaultFileContent(it, this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_credentials)

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
     //   outState.putString("JSON", parsedVault.toString())
    }

    override fun lock() {
        recreate()
    }

    fun createCredentialFromRecord(
        key: SecretKeyHolder,
        record: ImportCredentialsImportFileAdapter.FileRecord,
        labelNames: List<String>
    ): EncCredential {
        val encName = SecretService.encryptCommonString(key, record.name)
        val encAddInfo = SecretService.encryptCommonString(key, "")
        val encUser = SecretService.encryptCommonString(key, record.userName ?: "")
        val encPassword = SecretService.encryptPassword(key, Password(record.plainPassword))
        val encWebsite = SecretService.encryptCommonString(key, record.url ?: "")
        val encLabels = LabelService.defaultHolder.encryptLabelIds(
            key,
            labelNames
        )
        val credential = EncCredential(
            null, null,
            encName,
            encAddInfo,
            encUser,
            encPassword,
            null,
            encWebsite,
            encLabels,
            false,
            null,
            null
        )
        return credential
    }

}