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
import de.jepfa.yapm.util.fromSimpleDateFormat

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
            val nameKey = extractKeys(record, listOf("name","account", "title"))
            val urlKey = extractKeys(record, listOf("url", "website", "web site"))
            val userKey = extractKeys(record, listOf("username", "user", "login name", "login"))
            val descriptionKey = extractKeys(record, listOf("description", "desc", "hint", "hints", "comments"))
            val passwordKey = extractKeys(record, listOf("password", "passwd", "codeword", "code", "pin", "passphrase")) ?: return null
            val expiresOnKey = extractKeys(record, listOf("expiresOn", "expires on", "expiresAt", "expires at", "expires", "valid until", "validUntil")) ?: return null

            val password = record.value[passwordKey] ?: return null

            val id = record.index
            val url = record.value[urlKey]
            val name = record.value[nameKey]
            val user = record.value[userKey]
            val description = record.value[descriptionKey]
            val expiresOn = record.value[expiresOnKey]?.fromSimpleDateFormat()

            ImportCredentialsImportFileAdapter.FileRecord(id,
                name ?: url?.let { getDomainAsName(it)} ?: "unknown $id",
                url, user, password, description ?: "", expiresOn)

        }.filter { it.plainPassword.isNotEmpty() }

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

    private fun extractKeys(record: IndexedValue<Map<String, String>>, keyAliases: List<String>) =
        keyAliases.firstNotNullOfOrNull { extractKey(record, it) }

    private fun extractKey(record: IndexedValue<Map<String, String>>, key: String) =
        record.value.keys.firstOrNull { it.lowercase().trim() == key.lowercase() }

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
        val encExpiresAt = SecretService.encryptLong(key, record.expiresOn?.time ?: 0L)
        val encLabels = LabelService.defaultHolder.encryptLabelIds(
            key,
            labelNames
        )
        return EncCredential(
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
    }

}