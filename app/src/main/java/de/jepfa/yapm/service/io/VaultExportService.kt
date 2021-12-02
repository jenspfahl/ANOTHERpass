package de.jepfa.yapm.service.io

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.export.*
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.FileUtil
import java.lang.reflect.Type
import java.util.*


object VaultExportService {

    const val JSON_APP_VERSION_CODE = "appVersionCode"
    const val JSON_APP_VERSION_NAME = "appVersionName"
    const val JSON_CREATION_DATE = "creationDate"
    const val JSON_VAULT_ID = "vaultId"
    const val JSON_VAULT_VERSION = "vaultVersion"
    const val JSON_CIPHER_ALGORITHM = "cipherAlgoritm"
    const val JSON_ENC_MK = "encMk"
    const val JSON_CREDENTIALS = "credentials"
    const val JSON_CREDENTIALS_COUNT = "credentialsCount"
    const val JSON_LABELS = "labels"
    const val JSON_LABELS_COUNT = "labelsCount"
    const val JSON_APP_SETTINGS = "appSettings"

    val CREDENTIALS_TYPE: Type = object : TypeToken<List<EncCredential>>() {}.type
    val LABELS_TYPE: Type = object : TypeToken<List<EncLabel>>() {}.type

    class EncryptedSerializer : JsonSerializer<Encrypted> {
        override fun serialize(src: Encrypted?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            src?.let {
                return JsonPrimitive(it.toBase64String())
            }

            return JsonPrimitive("")
        }
    }

    class PasswordSerializer : JsonSerializer<Password> {
        override fun serialize(src: Password?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            src?.let {
                return JsonPrimitive(it.toBase64String())
            }

            return JsonPrimitive("")
        }
    }

    val GSON: Gson = GsonBuilder()
            .registerTypeAdapter(Encrypted::class.java, EncryptedSerializer())
            .registerTypeAdapter(Password::class.java,PasswordSerializer())
            .create()

    fun credentialToJson(credential: EncExportableCredential): String {
        return GSON.toJson(ExportContainer(TYPE_ENC_CREDENTIAL_RECORD, credential))
    }

    fun credentialToJson(credential: PlainShareableCredential): String {
        return GSON.toJson(ExportContainer(TYPE_PLAIN_CREDENTIAL_RECORD, credential))
    }

    fun createVaultFile(
        context: Context,
        app: YapmApp,
        includeMasterkey: Boolean,
        includePreferences: Boolean,
        uri: Uri
    ): Boolean {
        val jsonData = exportToJson(context, app, includeMasterkey, includePreferences)
        val success = writeExportFile(context, uri, jsonData)
        return success
    }


    fun parseVaultFileContent(content: String): JsonObject? {
        return try {
            JsonParser.parseString(content).asJsonObject
        } catch (e: Exception) {
            Log.e("JSON", "cannot parse JSON", e)
            null
        }
    }

    private fun writeExportFile(context: Context, uri: Uri, jsonData: JsonObject): Boolean {
        var success = false
        try {
            success = FileUtil.writeFile(context, uri, jsonData.toString())
            val content: String? = FileUtil.readFile(context, uri)
            if (TextUtils.isEmpty(content)) {
                Log.e("BACKUP", "Empty file created: $uri")
                success = false
            }
        } catch (e: Exception) {
            Log.e("BACKUP", "Cannot write file $uri", e)
        }
        return success
    }

    private fun exportToJson(context: Context, app: YapmApp, includeMasterkey: Boolean, includePreferences: Boolean): JsonObject {
        val root = JsonObject()
        try {
            root.addProperty(JSON_APP_VERSION_CODE, DebugInfo.getVersionCode(context))
            root.addProperty(JSON_APP_VERSION_NAME, DebugInfo.getVersionName(context))
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("BACKUPALL", "cannot get version code", e)
        }

        root.addProperty(JSON_CREATION_DATE, Constants.SDF_DT_MEDIUM.format(Date()))
        val salt = SaltService.getSaltAsBase64String(context)
        salt.let {
            root.addProperty(JSON_VAULT_ID, it)
        }

        val vaultVersion =
            PreferenceService.getAsString(PreferenceService.DATA_VAULT_VERSION, context)
            ?: Constants.UNKNOWN_VAULT_VERSION.toString()
        root.addProperty(JSON_VAULT_VERSION, vaultVersion)

        val cipherAlgorithm = SecretService.getCipherAlgorithm(context)
        root.addProperty(JSON_CIPHER_ALGORITHM, cipherAlgorithm.name)

        if (includeMasterkey) {
            val encStoredMasterKey = PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, context)
            if (encStoredMasterKey != null) {

                val mkKey = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MK, context)
                val encMasterKeyBase64 = SecretService.decryptEncrypted(mkKey, encStoredMasterKey).toBase64String()
                root.addProperty(JSON_ENC_MK, encMasterKeyBase64)
            }

        }

        val credentials = app.credentialRepository.getAllSync()

        root.add(JSON_CREDENTIALS, GSON.toJsonTree(credentials, CREDENTIALS_TYPE))
        root.addProperty(JSON_CREDENTIALS_COUNT, credentials.size)


        val labels = app.labelRepository.getAllSync()

        root.add(JSON_LABELS, GSON.toJsonTree(labels, LABELS_TYPE))
        root.addProperty(JSON_LABELS_COUNT, labels.size)

        if (includePreferences) {
            val allPrefs = PreferenceService.getAllPrefs(context)
            root.add(JSON_APP_SETTINGS, GSON.toJsonTree(allPrefs))
        }

        return root
    }

}