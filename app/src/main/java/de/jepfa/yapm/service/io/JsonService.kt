package de.jepfa.yapm.service.io

import android.util.Log
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.export.*
import de.jepfa.yapm.model.secret.Password
import java.lang.reflect.Type


object JsonService {

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

    fun parse(content: String): JsonObject? {
        return try {
            JsonParser.parseString(content).asJsonObject
        } catch (e: Exception) {
            Log.e("JSON", "cannot parse JSON", e)
            null
        }
    }
}