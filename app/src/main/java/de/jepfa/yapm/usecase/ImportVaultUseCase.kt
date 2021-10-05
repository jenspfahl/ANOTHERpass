package de.jepfa.yapm.usecase

import android.os.Build
import android.util.Log
import com.google.gson.JsonObject
import de.jepfa.yapm.model.encrypted.*
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_IMPORTED_AT
import de.jepfa.yapm.service.io.JsonService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

object ImportVaultUseCase {

    fun execute(jsonContent: JsonObject, encMasterKey: String?, activity: BaseActivity?): Boolean {
        if (activity == null) return false
        try {
            if (readAndImport(jsonContent, activity, encMasterKey)) return false
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
        val salt = jsonContent.get(JsonService.JSON_VAULT_ID)?.asString
        salt?.let {
            SaltService.storeSaltFromBase64String(it, activity)
        }

        val cipherAlgorithm = extractCipherAlgorithm(jsonContent)

        if (Build.VERSION.SDK_INT < cipherAlgorithm.supportedSdkVersion) {
            return true
        }

        val vaultVersion = jsonContent.get(JsonService.JSON_VAULT_VERSION)?.asString ?: "1"
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

        val credentialsJson = jsonContent.getAsJsonArray(JsonService.JSON_CREDENTIALS)
        CoroutineScope(Dispatchers.IO).launch {
            credentialsJson
                .filterNotNull()
                .map { json -> EncCredential.fromJson(json) }
                .filterNotNull()
                .forEach { c -> activity.getApp().credentialRepository.insert(c) }
        }

        val labelsJson = jsonContent.getAsJsonArray(JsonService.JSON_LABELS)
        CoroutineScope(Dispatchers.IO).launch {
            labelsJson
                .filterNotNull()
                .map { json -> EncLabel.fromJson(json) }
                .filterNotNull()
                .forEach { c -> activity.getApp().labelRepository.insert(c) }
        }

        PreferenceService.putString(
            DATA_VAULT_IMPORTED_AT,
            Constants.SDF_DT_MEDIUM.format(Date()),
            activity
        )
        return false
    }

    fun extractCipherAlgorithm(jsonContent: JsonObject): CipherAlgorithm {
        val cipherAlgorithmString = jsonContent.get(JsonService.JSON_CIPHER_ALGORITHM)?.asString
        val cipherAlgorithm =
            if (cipherAlgorithmString != null) CipherAlgorithm.valueOf(cipherAlgorithmString)
            else DEFAULT_CIPHER_ALGORITHM
        return cipherAlgorithm
    }

}