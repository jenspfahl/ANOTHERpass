package de.jepfa.yapm.usecase.vault

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Validable
import de.jepfa.yapm.model.encrypted.*
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import java.util.*

object ImportVaultUseCase: InputUseCase<ImportVaultUseCase.Input, SecureActivity>() {

    data class Input(
        val jsonContent: JsonObject,
        val encMasterKey: String?,
        val override: Boolean,
        val credentialIdsToOverride: Set<Int>? = null,
        val labelIdsToOverride: Set<Int>? = null,
        val copyOrigin: Boolean = false,
    )

    override suspend fun doExecute(input: Input, activity: SecureActivity): Boolean {
        try {
            val success =
                if (input.override)
                    readAndOverride(
                        input.jsonContent,
                        input.credentialIdsToOverride!!,
                        input.labelIdsToOverride!!,
                        input.copyOrigin,
                        activity)
                else
                    readAndImport(input.jsonContent, activity, input.encMasterKey)

            return success
        } catch (e: Exception) {
            Log.e("IMP", "cannot read json", e)
            return false
        }
    }



    fun parseVaultFileContent(content: String, context: Context, handleBlob: Boolean = false): JsonObject? {
        try {
            val masterKeySK = Session.getMasterKeySK()
            val rawJson = JsonParser.parseString(content).asJsonObject

            // credentials
            val jsonCredentials = rawJson.get(VaultExportService.JSON_CREDENTIALS)
            if (jsonCredentials != null) {
                if (handleBlob && jsonCredentials.isJsonPrimitive) {
                    val encJsonCredentialsAsPrimitive = jsonCredentials.asJsonPrimitive
                    if (encJsonCredentialsAsPrimitive.isString) {
                        val encJsonCredentialsAsString = encJsonCredentialsAsPrimitive.asString
                        if (masterKeySK != null) {
                            val encJsonCredentials =
                                Encrypted.fromBase64String(encJsonCredentialsAsString)
                            val jsonCredentialsAsString =
                                SecretService.decryptCommonString(masterKeySK, encJsonCredentials)

                            if (jsonCredentialsAsString == Validable.FAILED_STRING) {
                                return null
                            }

                            val jsonCredentials = JsonParser.parseString(jsonCredentialsAsString).asJsonArray
                            rawJson.add(VaultExportService.JSON_CREDENTIALS, jsonCredentials)
                        }
                        else {
                            // remove blob from json content
                            rawJson.remove(VaultExportService.JSON_CREDENTIALS)
                            //stage blob instead to get imported after login with masterkey
                            PreferenceService.putString(PreferenceService.TEMP_BLOB_CREDENTIALS, encJsonCredentialsAsString, context)
                        }
                    }
                }
            }

            // labels
            val jsonLabels = rawJson.get(VaultExportService.JSON_LABELS)
            if (jsonLabels != null) {
                if (handleBlob && jsonLabels.isJsonPrimitive) {
                    val encJsonLabelsAsPrimitive = jsonLabels.asJsonPrimitive
                    if (encJsonLabelsAsPrimitive.isString) {
                        val encJsonLabelsAsString = encJsonLabelsAsPrimitive.asString
                        if (masterKeySK != null) {
                            val encJsonLabels =
                                Encrypted.fromBase64String(encJsonLabelsAsString)
                            val jsonLabelsAsString =
                                SecretService.decryptCommonString(masterKeySK, encJsonLabels)

                            if (jsonLabelsAsString == Validable.FAILED_STRING) {
                                return null
                            }

                            val jsonLabels = JsonParser.parseString(jsonLabelsAsString).asJsonArray
                            rawJson.add(VaultExportService.JSON_LABELS, jsonLabels)
                        }
                        else {
                            // remove blob from json content
                            rawJson.remove(VaultExportService.JSON_LABELS)
                            //stage blob instead to get imported after login with masterkey
                            PreferenceService.putString(PreferenceService.TEMP_BLOB_LABELS, encJsonLabelsAsString, context)
                        }
                    }
                }
            }


            // app settings
            val jsonAppSettings = rawJson.get(VaultExportService.JSON_APP_SETTINGS)
            if (jsonAppSettings != null) {
                if (handleBlob && jsonAppSettings.isJsonPrimitive) {
                    val encJsonAppSettingsAsPrimitive = jsonAppSettings.asJsonPrimitive
                    if (encJsonAppSettingsAsPrimitive.isString) {
                        val encJsonAppSettingsAsString = encJsonAppSettingsAsPrimitive.asString
                        if (masterKeySK != null) {
                            val encJsonAppSettings =
                                Encrypted.fromBase64String(encJsonAppSettingsAsString)
                            val jsonAppSettingsAsString =
                                SecretService.decryptCommonString(masterKeySK, encJsonAppSettings)

                            if (jsonAppSettingsAsString == Validable.FAILED_STRING) {
                                return null
                            }

                            val jsonAppSettings = JsonParser.parseString(jsonAppSettingsAsString).asJsonObject
                            rawJson.add(VaultExportService.JSON_APP_SETTINGS, jsonAppSettings)
                        }
                        else {
                            // remove blob from json content
                            rawJson.remove(VaultExportService.JSON_APP_SETTINGS)
                            //stage blob instead to get imported after login with masterkey
                            PreferenceService.putString(PreferenceService.TEMP_BLOB_SETTINGS, encJsonAppSettingsAsString, context)
                        }
                    }
                }
            }

            return rawJson
        } catch (e: Exception) {
            Log.e("JSON", "cannot parse JSON", e)
            return null
        }
    }
    
    fun importStagedData(activity: BaseActivity) {
        val masterSecretKey = Session.getMasterKeySK()
            ?: throw IllegalStateException("No secret to decrypt staged vault file")

        val stagedEncCredentials = PreferenceService.getAsString(PreferenceService.TEMP_BLOB_CREDENTIALS, activity)
        val stagedEncLabels = PreferenceService.getAsString(PreferenceService.TEMP_BLOB_LABELS, activity)
        val stagedEncAppSettings = PreferenceService.getAsString(PreferenceService.TEMP_BLOB_SETTINGS, activity)

        if (stagedEncCredentials != null) {
            val encCredentials = Encrypted.fromBase64String(stagedEncCredentials)
            val jsonCredentialsAsString = SecretService.decryptCommonString(masterSecretKey, encCredentials)
            if (jsonCredentialsAsString != Validable.FAILED_STRING) {
                val jsonCredentials = JsonParser.parseString(jsonCredentialsAsString).asJsonArray
                persistCredentials(jsonCredentials, activity)
            }
            PreferenceService.delete(PreferenceService.TEMP_BLOB_CREDENTIALS, activity)
        }

        if (stagedEncLabels != null) {
            val encLabels = Encrypted.fromBase64String(stagedEncLabels)
            val jsonLabelsAsString = SecretService.decryptCommonString(masterSecretKey, encLabels)
            if (jsonLabelsAsString != Validable.FAILED_STRING) {
                val jsonLabels = JsonParser.parseString(jsonLabelsAsString).asJsonArray
                persistLabels(jsonLabels, activity)
            }
            PreferenceService.delete(PreferenceService.TEMP_BLOB_LABELS, activity)
        }
        
        if (stagedEncAppSettings != null) {
            val encAppSettings = Encrypted.fromBase64String(stagedEncAppSettings)
            val jsonAppSettingsAsString = SecretService.decryptCommonString(masterSecretKey, encAppSettings)
            if (jsonAppSettingsAsString != Validable.FAILED_STRING) {
                val jsonAppSettings = JsonParser.parseString(jsonAppSettingsAsString).asJsonObject
                persistAppSettings(jsonAppSettings, activity)
            }
            PreferenceService.delete(PreferenceService.TEMP_BLOB_SETTINGS, activity)
        }

    }

    private fun readAndImport(
        jsonContent: JsonObject,
        activity: BaseActivity,
        encMasterKey: String?
    ): Boolean {

        // this is to have a clean baseline
        PreferenceService.deleteAllData(activity)

        val salt = jsonContent.get(VaultExportService.JSON_VAULT_ID)?.asString
        salt?.let {
            SaltService.storeSaltFromBase64String(it, activity)
        }

        val cipherAlgorithm = extractCipherAlgorithm(jsonContent)

        if (Build.VERSION.SDK_INT < cipherAlgorithm.supportedSdkVersion) {
            Log.e("IMPV", "Unsupported cipher algorithm $cipherAlgorithm")
            return false
        }

        val vaultVersion = jsonContent.get(VaultExportService.JSON_VAULT_VERSION)?.asInt
            ?: Constants.INITIAL_VAULT_VERSION
        if (vaultVersion > Constants.CURRENT_VERSION) {
            Log.e("IMPV", "Unsupported vault version $vaultVersion")
            return false
        }
        PreferenceService.putString(PreferenceService.DATA_VAULT_VERSION, vaultVersion.toString(), activity)

        PreferenceService.putString(
            PreferenceService.DATA_CIPHER_ALGORITHM,
            cipherAlgorithm.name,
            activity
        )

        if (encMasterKey != null) {
            val keyForMK = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MK, activity)
            val encEncryptedMasterKey = SecretService.encryptEncrypted(
                keyForMK, Encrypted.fromBase64String(
                    encMasterKey
                )
            )

            PreferenceService.putEncrypted(
                PreferenceService.DATA_ENCRYPTED_MASTER_KEY,
                encEncryptedMasterKey,
                activity
            )
        }

        val encSeedBase64String = jsonContent.get(VaultExportService.JSON_ENC_SEED)?.asString
        if (encSeedBase64String != null) {
            PreferenceService.putEncrypted(
                PreferenceService.DATA_ENCRYPTED_SEED,
                Encrypted.fromBase64String(encSeedBase64String),
                activity
            )
        }

        val credentialsJson = jsonContent.get(VaultExportService.JSON_CREDENTIALS)?.asJsonArray
        if (credentialsJson != null) {
            persistCredentials(credentialsJson, activity)
        }

        val labelsJson = jsonContent.get(VaultExportService.JSON_LABELS)?.asJsonArray
        if (labelsJson != null) {
            persistLabels(labelsJson, activity)
        }

        val appSettings = jsonContent.get(VaultExportService.JSON_APP_SETTINGS)?.asJsonObject
        if (appSettings != null) {
            persistAppSettings(appSettings, activity)
        }

        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_IMPORTED_AT, activity)
        return true
    }

    private fun readAndOverride(
        jsonContent: JsonObject,
        credentialIdsToOverride: Set<Int>,
        labelIdsToOverride: Set<Int>,
        copyOrigin: Boolean,
        activity: SecureActivity
    ): Boolean {
        val app = activity.getApp()
        val credentialsJson = jsonContent.get(VaultExportService.JSON_CREDENTIALS)?.asJsonArray
        if (credentialsJson != null) {
            CoroutineScope(Dispatchers.IO).launch {
                credentialsJson
                    .filterNotNull()
                    .map { json -> EncCredential.fromJson(json) }
                    .filterNotNull()
                    .filter { credentialIdsToOverride.contains(it.id) }
                    .forEach { c ->
                        c.touchModify()
                        val existingC = app.credentialRepository.findByIdSync(c.id!!)
                        if (existingC == null) {
                            app.credentialRepository.insert(c)
                        } else {
                            if (copyOrigin) {
                                val copyOfExistingC = existingC.copy(
                                    id = null,
                                    uid = UUID.randomUUID(),
                                    name = copyName(existingC.name, activity)
                                )
                                copyOfExistingC.touchModify()
                                app.credentialRepository.insert(copyOfExistingC)
                            }
                            app.credentialRepository.update(c)
                        }
                    }
            }
        }

        val labelsJson = jsonContent.get(VaultExportService.JSON_LABELS)?.asJsonArray
        if (labelsJson != null) {
            CoroutineScope(Dispatchers.IO).launch {
                labelsJson
                    .filterNotNull()
                    .map { json -> EncLabel.fromJson(json) }
                    .filterNotNull()
                    .filter { labelIdsToOverride.contains(it.id) }
                    .forEach { l ->
                        val existingL = app.labelRepository.findByIdSync(l.id!!)
                        if (existingL == null) {
                            app.labelRepository.insert(l)
                        } else {
                            if (copyOrigin) {
                                val copyOfExistingL = existingL.copy(
                                    id = null,
                                    uid = UUID.randomUUID(),
                                    name = copyName(existingL.name, activity)
                                )
                                app.labelRepository.insert(copyOfExistingL)
                            }
                            app.labelRepository.update(l)
                        }
                    }
            }
        }

        return true
    }

    fun extractCipherAlgorithm(jsonContent: JsonObject): CipherAlgorithm {
        val cipherAlgorithmString = jsonContent.get(VaultExportService.JSON_CIPHER_ALGORITHM)?.asString
        val cipherAlgorithm =
            if (cipherAlgorithmString != null) CipherAlgorithm.valueOf(cipherAlgorithmString)
            else DEFAULT_CIPHER_ALGORITHM
        return cipherAlgorithm
    }


    private fun copyName(name: Encrypted, activity: SecureActivity): Encrypted {
        val key = activity.masterSecretKey
        if (key != null) {
            val name = SecretService.decryptCommonString(key, name)
            val newName = activity.getString(R.string.copy_of_name, name)
            return SecretService.encryptCommonString(key, newName)
        }
        else {
            return name
        }
    }

    private fun persistAppSettings(
        appSettings: JsonObject,
        activity: BaseActivity
    ) {
        appSettings.entrySet()
            .forEach { (k, v) ->
                if (v.isJsonPrimitive) {
                    if (v.asJsonPrimitive.isString) {
                        PreferenceService.putString(k, v.asString, activity)
                    } else if (v.asJsonPrimitive.isBoolean) {
                        PreferenceService.putBoolean(k, v.asBoolean, activity)
                    }
                } else if (v.isJsonArray) {
                    val stringValues = v.asJsonArray
                        .filter { it.isJsonPrimitive }
                        .filter { it.asJsonPrimitive.isString }
                        .map { v -> v.asJsonPrimitive.asString }
                        .toSet()
                    PreferenceService.putStringSet(k, stringValues, activity)
                }
            }
    }

    private fun persistLabels(
        labelsJson: JsonArray,
        activity: BaseActivity
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            labelsJson
                .filterNotNull()
                .map { json -> EncLabel.fromJson(json) }
                .filterNotNull()
                .forEach { c -> activity.getApp().labelRepository.insert(c) }
        }
    }

    private fun persistCredentials(
        credentialsJson: JsonArray,
        activity: BaseActivity
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            credentialsJson
                .filterNotNull()
                .map { json -> EncCredential.fromJson(json) }
                .filterNotNull()
                .forEach { c -> activity.getApp().credentialRepository.insert(c) }
        }
    }

}