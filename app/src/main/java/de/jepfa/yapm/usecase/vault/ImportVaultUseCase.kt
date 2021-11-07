package de.jepfa.yapm.usecase.vault

import android.os.Build
import android.util.Log
import com.google.gson.JsonObject
import de.jepfa.yapm.model.encrypted.*
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.InputUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ImportVaultUseCase: InputUseCase<ImportVaultUseCase.Input, BaseActivity>() {

    data class Input(val jsonContent: JsonObject, val encMasterKey: String?)

    override fun doExecute(input: Input, activity: BaseActivity): Boolean {
        try {
            if (readAndImport(input.jsonContent, activity, input.encMasterKey)) return false
        } catch (e: Exception) {
            Log.e("IMP", "cannot read json", e)
            return false
        }

        return true
    }

    private fun readAndImport(
        jsonContent: JsonObject,
        activity: BaseActivity,
        encMasterKey: String?
    ): Boolean {
        val salt = jsonContent.get(VaultExportService.JSON_VAULT_ID)?.asString
        salt?.let {
            SaltService.storeSaltFromBase64String(it, activity)
        }

        val cipherAlgorithm = extractCipherAlgorithm(jsonContent)

        if (Build.VERSION.SDK_INT < cipherAlgorithm.supportedSdkVersion) {
            return true
        }

        val vaultVersion = jsonContent.get(VaultExportService.JSON_VAULT_VERSION)?.asString ?: "1"
        PreferenceService.putString(PreferenceService.DATA_VAULT_VERSION, vaultVersion, activity)

        PreferenceService.putString(
            PreferenceService.DATA_CIPHER_ALGORITHM,
            cipherAlgorithm.name,
            activity
        )

        if (encMasterKey != null) {
            val keyForMK = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)
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
        return false
    }

    fun extractCipherAlgorithm(jsonContent: JsonObject): CipherAlgorithm {
        val cipherAlgorithmString = jsonContent.get(VaultExportService.JSON_CIPHER_ALGORITHM)?.asString
        val cipherAlgorithm =
            if (cipherAlgorithmString != null) CipherAlgorithm.valueOf(cipherAlgorithmString)
            else DEFAULT_CIPHER_ALGORITHM
        return cipherAlgorithm
    }


}