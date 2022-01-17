package de.jepfa.yapm.usecase.vault

import android.os.Build
import android.util.Log
import com.google.gson.JsonObject
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.*
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ImportVaultUseCase: InputUseCase<ImportVaultUseCase.Input, SecureActivity>() {

    data class Input(
        val jsonContent: JsonObject,
        val encMasterKey: String?,
        val override: Boolean,
        val credentialIdsToOverride: Set<Int>? = null,
        val labelIdsToOverride: Set<Int>? = null,
        val copyOrigin: Boolean = false,
    )

    override fun doExecute(input: Input, activity: SecureActivity): Boolean {
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
            return false
        }

        val vaultVersion = jsonContent.get(VaultExportService.JSON_VAULT_VERSION)?.asString ?: "1"
        PreferenceService.putString(PreferenceService.DATA_VAULT_VERSION, vaultVersion, activity)

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

        val credentialsJson = jsonContent.getAsJsonArray(VaultExportService.JSON_CREDENTIALS)
        CoroutineScope(Dispatchers.IO).launch {
            credentialsJson
                .filterNotNull()
                .map { json -> EncCredential.fromJson(json) }
                .filterNotNull()
                .forEach { c -> activity.getApp().credentialRepository.insert(c) }
        }

        val labelsJson = jsonContent.getAsJsonArray(VaultExportService.JSON_LABELS)
        CoroutineScope(Dispatchers.IO).launch {
            labelsJson
                .filterNotNull()
                .map { json -> EncLabel.fromJson(json) }
                .filterNotNull()
                .forEach { c -> activity.getApp().labelRepository.insert(c) }
        }

        val appSettings = jsonContent.get(VaultExportService.JSON_APP_SETTINGS)?.asJsonObject
        if (appSettings != null) {
            appSettings.entrySet()
                .forEach { (k,v) ->
                    if (v.isJsonPrimitive) {
                        if (v.asJsonPrimitive.isString) {
                            PreferenceService.putString(k, v.asString, activity)
                        } else if (v.asJsonPrimitive.isBoolean) {
                            PreferenceService.putBoolean(k, v.asBoolean, activity)
                        }
                    }
                    else if (v.isJsonArray) {
                        val stringValues = v.asJsonArray
                            .filter { it.isJsonPrimitive }
                            .filter { it.asJsonPrimitive.isString }
                            .map { v -> v.asJsonPrimitive.asString }
                            .toSet()
                        PreferenceService.putStringSet(k, stringValues, activity)
                    }
            }

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
        val credentialsJson = jsonContent.getAsJsonArray(VaultExportService.JSON_CREDENTIALS)
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
                    }
                    else {
                        if (copyOrigin) {
                            val copyOfExistingC = existingC.copy(
                                id = null, name = copyName(existingC.name, activity))
                            copyOfExistingC.touchModify()
                            app.credentialRepository.insert(copyOfExistingC)
                        }
                        app.credentialRepository.update(c)
                    }
                }
        }

        val labelsJson = jsonContent.getAsJsonArray(VaultExportService.JSON_LABELS)
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
                    }
                    else {
                        if (copyOrigin) {
                            val copyOfExistingL = existingL.copy(
                                id = null, name = copyName(existingL.name, activity))
                            app.labelRepository.insert(copyOfExistingL)
                        }
                        app.labelRepository.update(l)
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

}