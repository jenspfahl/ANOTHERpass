package de.jepfa.yapm.usecase

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.EncLabel
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.util.PreferenceUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ImportVaultUseCase {

    fun execute(jsonContent: JsonObject, encMasterKey: String?, activity: BaseActivity): Boolean {
        val salt = jsonContent.get(FileIOService.JSON_VAULT_ID)?.asString
        salt?.let { PreferenceUtil.put(PreferenceUtil.DATA_SALT, salt, activity) }

        if (encMasterKey != null) {
            val keyForMK = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)
            val encEncryptedMasterKey = SecretService.encryptEncrypted(keyForMK, Encrypted.fromBase64String(encMasterKey!!))

            PreferenceUtil.putEncrypted(PreferenceUtil.DATA_ENCRYPTED_MASTER_KEY, encEncryptedMasterKey, activity)
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

        return true
    }


    private fun deserializeCredential(json: JsonElement?): EncCredential? {
        if (json != null) {
            val jsonObject = json.asJsonObject
            return EncCredential(
                jsonObject.get(EncCredential.ATTRIB_ID).asInt,
                jsonObject.get(EncCredential.ATTRIB_NAME).asString,
                jsonObject.get(EncCredential.ATTRIB_ADDITIONAL_INFO).asString,
                jsonObject.get(EncCredential.ATTRIB_USER).asString,
                jsonObject.get(EncCredential.ATTRIB_PASSWORD).asString,
                jsonObject.get(EncCredential.ATTRIB_WEBSITE).asString,
                jsonObject.get(EncCredential.ATTRIB_LABELS).asString

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