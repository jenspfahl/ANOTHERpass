package de.jepfa.yapm.usecase

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.jepfa.yapm.model.encrypted.*
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_IMPORTED_AT
import de.jepfa.yapm.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

object ImportVaultUseCase {

    fun execute(jsonContent: JsonObject, encMasterKey: String?, activity: BaseActivity?): Boolean {
        if (activity == null) return false
        val salt = jsonContent.get(FileIOService.JSON_VAULT_ID)?.asString
        salt?.let {
            SaltService.storeSaltFromBase64String(it, activity)
        }

        val cipherAlgorithmString = jsonContent.get(FileIOService.JSON_CIPHER_ALGORITHM)?.asString
        val cipherAlgorith =
            if (cipherAlgorithmString != null) CipherAlgorithm.valueOf(cipherAlgorithmString)
            else DEFAULT_CIPHER_ALGORITHM

        PreferenceService.putString(PreferenceService.DATA_CIPHER_ALGORITHM, cipherAlgorith.name, activity)

        if (encMasterKey != null) {
            val keyForMK = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)
            val encEncryptedMasterKey = SecretService.encryptEncrypted(keyForMK, Encrypted.fromBase64String(
                encMasterKey
            ))

            PreferenceService.putEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, encEncryptedMasterKey, activity)
        }

        val credentialsJson = jsonContent.getAsJsonArray(FileIOService.JSON_CREDENTIALS)
        CoroutineScope(Dispatchers.IO).launch {
            credentialsJson
                .map{ json -> deserializeCredential(json)}
                .filterNotNull()
                .forEach { c -> activity.getApp().credentialRepository.insert(c) }
        }

        val labelsJson = jsonContent.getAsJsonArray(FileIOService.JSON_LABELS)
        CoroutineScope(Dispatchers.IO).launch {
            labelsJson
                .map{ json -> deserializeLabel(json)}
                .filterNotNull()
                .forEach { c -> activity.getApp().labelRepository.insert(c) }
        }

        PreferenceService.putString(
            DATA_VAULT_IMPORTED_AT,
            Constants.SDF_DT_MEDIUM.format(Date()),
            activity)

        return true
    }


    private fun deserializeCredential(json: JsonElement?): EncCredential? {
        if (json != null) {
            val jsonObject = json.asJsonObject
            val jsonLastPassword = jsonObject.get(EncCredential.ATTRIB_LAST_PASSWORD)
            val jsonIsObfuscated = jsonObject.get(EncCredential.ATTRIB_IS_OBFUSCATED)
            val jsonIsLastPasswordObfuscated = jsonObject.get(EncCredential.ATTRIB_IS_LAST_PASSWORD_OBFUSCATED)
            return EncCredential(
                jsonObject.get(EncCredential.ATTRIB_ID).asInt,
                jsonObject.get(EncCredential.ATTRIB_NAME).asString,
                jsonObject.get(EncCredential.ATTRIB_ADDITIONAL_INFO).asString,
                jsonObject.get(EncCredential.ATTRIB_USER).asString,
                jsonObject.get(EncCredential.ATTRIB_PASSWORD).asString,
                if (jsonLastPassword != null) jsonLastPassword.asString else null,
                jsonObject.get(EncCredential.ATTRIB_WEBSITE).asString,
                jsonObject.get(EncCredential.ATTRIB_LABELS).asString,
                if (jsonIsObfuscated != null) jsonIsObfuscated.asBoolean else false,
                if (jsonIsLastPasswordObfuscated != null) jsonIsLastPasswordObfuscated.asBoolean else false

            )
        }
        else {
            return null
        }
    }

    private fun deserializeLabel(json: JsonElement?): EncLabel? {
        if (json != null) {
            val jsonObject = json.asJsonObject
            val color = jsonObject.get(EncLabel.ATTRIB_COLOR)
            return EncLabel(
                jsonObject.get(EncLabel.ATTRIB_ID).asInt,
                jsonObject.get(EncLabel.ATTRIB_NAME).asString,
                jsonObject.get(EncLabel.ATTRIB_DESC).asString,
                if (color == null) null else color.asInt
            )
        }
        else {
            return null
        }
    }

}