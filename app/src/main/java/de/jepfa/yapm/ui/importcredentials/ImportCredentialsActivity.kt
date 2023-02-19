package de.jepfa.yapm.ui.importcredentials

import android.net.Uri
import android.os.Bundle
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.io.CsvService
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity

class ImportCredentialsActivity : SecureActivity() {

    var content: String? = null
    var records: List<ImportCredentialsImportFileAdapter.FileRecord>? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        savedInstanceState?.getString("CSV")?.let { csv ->
            content = csv
            CsvService.parseCsv(csv)?.let {
                records = readContent(it)
            }
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_credentials)

        //TODO all calls of initLabels() is to refresh the holder when new Labels are inserted to ths model to obtain the id for future lookups
        labelViewModel.allLabels.observe(this) { labels ->
            masterSecretKey?.let { key ->
                LabelService.defaultHolder.initLabels(key, labels.toSet())
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("CSV", content)
    }

    override fun lock() {
        recreate()
    }

    fun readContent(csvRecords: List<Map<String, String>>): List<ImportCredentialsImportFileAdapter.FileRecord>? {
        return csvRecords.withIndex().map { record ->
            val nameKey = extractKey(record, "name")
            val urlKey = extractKey(record, "url")
            val userKey = extractKey(record, "username")
            val descriptionKey = extractKey(record, "description")
            val passwordKey = extractKey(record, "password") ?: return null

            val id = record.index
            val url = record.value[urlKey]
            val name = record.value[nameKey]
            val user = record.value[userKey]
            val description = record.value[descriptionKey]
            val password = record.value[passwordKey] ?: return null
            ImportCredentialsImportFileAdapter.FileRecord(id,
                name ?: url?.let { getDomainAsName(it)} ?: "unknown $id",
                url, user, password, description ?: "")

        }

    }

    private fun getDomainAsName(url: String): String {
        val uri = Uri.parse(url)
        val host = uri.host
        if (host != null) {
            return host.substringBeforeLast(".").substringAfterLast(".").capitalize()
        }
        else {
            return url
        }

    }

    private fun extractKey(record: IndexedValue<Map<String, String>>, key: String) =
        record.value.keys.map { it.lowercase().trim() }.firstOrNull { it == key }

    fun createCredentialFromRecord(
        key: SecretKeyHolder,
        record: ImportCredentialsImportFileAdapter.FileRecord,
        labelNames: List<String>
    ): EncCredential {
        val encName = SecretService.encryptCommonString(key, record.name)
        val encAddInfo = SecretService.encryptCommonString(key, record.description)
        val encUser = SecretService.encryptCommonString(key, record.userName ?: "")
        val encPassword = SecretService.encryptPassword(key, Password(record.plainPassword))
        val encWebsite = SecretService.encryptCommonString(key, record.url ?: "")
        val encExpiresAt = SecretService.encryptLong(key, 0L)
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
            encExpiresAt,
            false,
            null,
            null
        )
        return credential
    }

}