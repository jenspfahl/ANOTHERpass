package de.jepfa.yapm.model.export

import android.util.Log
import com.google.gson.JsonElement
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.util.toBase64String
import de.jepfa.yapm.util.toUUIDFromBase64String

data class EncExportableCredential(val i: Int?,
                                   val ui: String?,
                                   val n: Encrypted,
                                   val aI: Encrypted,
                                   val u: Encrypted,
                                   val p: Encrypted,
                                   val w: Encrypted,
                                   val l: Encrypted,
                                   val o: Boolean,
) {

    constructor(credential: EncCredential) :
            this(
                credential.id,
                credential.uid?.toBase64String(),
                credential.name,
                credential.additionalInfo,
                credential.user,
                credential.password,
                credential.website,
                credential.labels,
                credential.isObfuscated,
            )

    constructor(id: Int?,
                uidBase64: String?,
                nameBase64: String,
                additionalInfoBase64: String,
                userBase64: String,
                passwordBase64: String,
                websiteBase64: String,
                labelsBase64: String,
                isObfuscated: Boolean) :
            this(id,
                uidBase64,
                Encrypted.fromBase64String(nameBase64),
                Encrypted.fromBase64String(additionalInfoBase64),
                Encrypted.fromBase64String(userBase64),
                Encrypted.fromBase64String(passwordBase64),
                Encrypted.fromBase64String(websiteBase64),
                Encrypted.fromBase64String(labelsBase64),
                isObfuscated
            )

    fun toEncCredential(): EncCredential {
        return EncCredential(
            i,
            ui?.toUUIDFromBase64String(),
            n,
            aI,
            u,
            p,
            null,
            w,
            l,
            o,
            null,
            null
        )
    }

    companion object {
        const val ATTRIB_ID = "i"
        const val ATTRIB_UID = "ui"
        const val ATTRIB_NAME = "n"
        const val ATTRIB_ADDITIONAL_INFO = "aI"
        const val ATTRIB_USER = "u"
        const val ATTRIB_PASSWORD = "p"
        const val ATTRIB_WEBSITE = "w"
        const val ATTRIB_LABELS = "l"
        const val ATTRIB_IS_OBFUSCATED = "o"

        fun fromJson(json: JsonElement): EncExportableCredential? {
            return try {
                val jsonObject = json.asJsonObject
                EncExportableCredential(
                    jsonObject.get(ATTRIB_ID).asInt,
                    jsonObject.get(ATTRIB_UID)?.asString,
                    jsonObject.get(ATTRIB_NAME).asString,
                    jsonObject.get(ATTRIB_ADDITIONAL_INFO).asString,
                    jsonObject.get(ATTRIB_USER).asString,
                    jsonObject.get(ATTRIB_PASSWORD).asString,
                    jsonObject.get(ATTRIB_WEBSITE).asString,
                    jsonObject.get(ATTRIB_LABELS).asString,
                    jsonObject.get(ATTRIB_IS_OBFUSCATED)?.asBoolean ?: false,
                )
            } catch (e: Exception) {
                Log.e("ECR", "cannot parse json container", e)
                null
            }
        }
    }

}
