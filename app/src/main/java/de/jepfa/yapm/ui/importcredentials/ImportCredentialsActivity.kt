package de.jepfa.yapm.ui.importcredentials

import android.net.Uri
import android.os.Bundle
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.CredentialFileRecord
import de.jepfa.yapm.service.io.CsvService
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.label.LabelsHolder
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.fromSimpleDateFormat
import kotlin.math.max
import kotlin.math.min

class ImportCredentialsActivity : SecureActivity() {

    var firstRecord: Map<String, String>? = null
    var fileName: String? = null
    var csvContent: List<Map<String, String>>? = null
    var content: String? = null
    var records: List<CredentialFileRecord>? = null

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

    fun readContent(csvRecords: List<Map<String, String>>?): List<CredentialFileRecord>? {

        if (csvRecords == null) {
            return null
        }

        firstRecord = csvRecords.firstOrNull()
        val nameKeyAliases = mutableListOf("name", "account", "title")
        val urlKeyAliases = mutableListOf("url", "website", "web site", "login_uri")
        val userKeyAliases = mutableListOf("username", "user", "login name", "login", "login_username")
        val descriptionKeyAliases = mutableListOf("description", "desc", "hint", "hints", "comments", "notes")
        val passwordKeyAliases =
            mutableListOf("password", "passwd", "codeword", "code", "pin", "passphrase", "login_password")
        val expiresOnKeyAliases = mutableListOf(
            "expiresOn",
            "expires on",
            "expiresAt",
            "expires at",
            "expires",
            "valid until",
            "validUntil"
        )

        PreferenceService.getAsString(PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_NAME, this)?.let {
            addToListIfNotEmpty(it, nameKeyAliases)
        }
        PreferenceService.getAsString(PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_WEBSITE, this)?.let {
            addToListIfNotEmpty(it, urlKeyAliases)
        }
        PreferenceService.getAsString(PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_USERNAME, this)?.let {
            addToListIfNotEmpty(it, userKeyAliases)
        }
        PreferenceService.getAsString(PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_ADDITIONAL_INFO, this)?.let {
            addToListIfNotEmpty(it, descriptionKeyAliases)
        }
        PreferenceService.getAsString(PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_PASSWORD, this)?.let {
            addToListIfNotEmpty(it, passwordKeyAliases)
        }
        PreferenceService.getAsString(PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_EXPIRY_DATE, this)?.let {
            addToListIfNotEmpty(it, expiresOnKeyAliases)
        }


        return csvRecords.withIndex().map { record ->
            val nameKey = extractKeys(record, nameKeyAliases)
            val urlKey = extractKeys(record, urlKeyAliases)
            val userKey = extractKeys(record, userKeyAliases)
            val descriptionKey = extractKeys(record, descriptionKeyAliases)
            val passwordKey = extractKeys(record, passwordKeyAliases) ?: return null
            val expiresOnKey = extractKeys(record, expiresOnKeyAliases)

            val password = record.value[passwordKey] ?: return null

            val id = record.index
            val url = record.value[urlKey]
            val name = record.value[nameKey]
            val user = record.value[userKey]
            val description = record.value[descriptionKey]
            val expiresOn = record.value[expiresOnKey]?.fromSimpleDateFormat()

            CredentialFileRecord(id,
                name ?: url?.let { getDomainAsName(it)} ?: "unknown $id",
                url, user, password, description ?: "", expiresOn)

        }.filter { it.plainPassword.isNotEmpty() }

    }

    private fun addToListIfNotEmpty(prefKey: String, keyAliases: MutableList<String>) {
        if (prefKey.isNotBlank()) {
            keyAliases.add(0, prefKey)
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

    private fun extractKeys(record: IndexedValue<Map<String, String>>, keyAliases: List<String>) =
        keyAliases.firstNotNullOfOrNull { extractKey(record, it) }

    private fun extractKey(record: IndexedValue<Map<String, String>>, key: String) =
        record.value.keys.firstOrNull { it.lowercase().trim() == key.lowercase() }

    fun createCredentialFromRecord(
        key: SecretKeyHolder,
        record: CredentialFileRecord,
        labelNames: List<String>,
        labelsHolder: LabelsHolder,
    ): EncCredential {
        val encName = SecretService.encryptCommonString(key, record.name)
        val encAddInfo = SecretService.encryptCommonString(key, record.description)
        val encUser = SecretService.encryptCommonString(key, record.userName ?: "")
        val encPassword = SecretService.encryptPassword(key, Password(record.plainPassword))
        val encWebsite = SecretService.encryptCommonString(key, record.url ?: "")
        val encExpiresAt = SecretService.encryptLong(key, record.expiresAt?.time ?: 0L)

        val allowedLabelCountFromOutside = max(0, Constants.MAX_LABELS_PER_CREDENTIAL - labelNames.size)


        val labelNamesToAdd = HashSet<String>(labelNames)
        labelNamesToAdd.addAll(record.labels.take(allowedLabelCountFromOutside))

        labelNamesToAdd.forEachIndexed { index, it ->
            val existingLabel = labelsHolder.lookupByLabelName(it)
            if (existingLabel == null) {
                val label = labelsHolder.createNewLabel(it, this, id = -index) // fake ids to find the label in externalHolder
                labelsHolder.updateLabel(label)
            }
        }
        val encLabels = labelsHolder.encryptLabelIds(
            key,
            labelNamesToAdd.toList()
        )
        return EncCredential(
            null,
            null, // record.uuid, -- commented out, to not overwrite existing credentials when importing them back
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
            record.modifiedAt?.time,
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        LabelService.externalHolder.clearAll()
    }

}