package de.jepfa.yapm.model.export

import android.util.Log
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.toUUIDFromBase64String
import java.util.*


data class PlainShareableCredential(val ui: String?,
                                    val n: String,
                                    val aI: String,
                                    val u: String,
                                    val p: Password,
                                    val w: String,
) {
    fun toEncCredential(key: SecretKeyHolder): EncCredential {
        val encName = SecretService.encryptCommonString(key, n)
        val encAdditionalInfo = SecretService.encryptCommonString(key, aI)
        val encUser = SecretService.encryptCommonString(key, u)
        val encPasswd = SecretService.encryptPassword(key, p)
        val encWebsite = SecretService.encryptCommonString(key, w)
        val encLabels = SecretService.encryptCommonString(key, "")

        p.clear()
        return EncCredential(
            null,
            ui?.toUUIDFromBase64String() ?: UUID.randomUUID(),
            encName,
            encAdditionalInfo,
            encUser,
            encPasswd,
            null,
            encWebsite,
            encLabels,
            false,
            null,
            null
        )
    }

    companion object {
        const val ATTRIB_UID = "ui"
        const val ATTRIB_NAME = "n"
        const val ATTRIB_ADDITIONAL_INFO = "aI"
        const val ATTRIB_USER = "u"
        const val ATTRIB_PASSWORD = "p"
        const val ATTRIB_WEBSITE = "w"

        fun fromJson(json: JsonElement): PlainShareableCredential? {
            try {
                val jsonObject = json.asJsonObject
                return PlainShareableCredential(
                    jsonObject.get(ATTRIB_UID)?.asString,
                    jsonObject.get(ATTRIB_NAME).asString,
                    jsonObject.get(ATTRIB_ADDITIONAL_INFO).asString,
                    jsonObject.get(ATTRIB_USER).asString,
                    Password.fromBase64String(jsonObject.get(ATTRIB_PASSWORD).asString),
                    jsonObject.get(ATTRIB_WEBSITE).asString,
                )
            } catch (e: Exception) {
                Log.e("PCR", "cannot parse json container", e)
                return null
            }
        }
    }
}

