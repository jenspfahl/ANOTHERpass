package de.jepfa.yapm.model.export

import com.google.gson.JsonElement
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.OtpData
import de.jepfa.yapm.model.encrypted.PasswordData
import de.jepfa.yapm.model.encrypted.TimeData
import de.jepfa.yapm.model.otp.OtpConfig.Companion.createFromPacked
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.toUUIDFromBase64String


data class PlainShareableCredential(val ui: String?, // uid
                                    val n: String, // name
                                    val aI: String, // additional info
                                    val u: String, // user
                                    val p: Password, // password
                                    val w: String, // website
                                    val e: Long?, // Date.getTime()
                                    val o: String? // shortened OTP
) {
    fun toEncCredential(key: SecretKeyHolder): EncCredential {
        val encName = SecretService.encryptCommonString(key, n)
        val encAdditionalInfo = SecretService.encryptCommonString(key, aI)
        val encUser = SecretService.encryptCommonString(key, u)
        val encPasswd = SecretService.encryptPassword(key, p)
        val encWebsite = SecretService.encryptCommonString(key, w)
        val encExpiresAt = if (e != null) SecretService.encryptLong(key, e) else SecretService.encryptLong(key, 0L)
        val encLabels = SecretService.encryptCommonString(key, "")
        val encOtpAuthUri = createFromPacked(o, n, u)?.let {  SecretService.encryptCommonString(key, it.toString()) }

        p.clear()
        return EncCredential(
            null,
            ui?.toUUIDFromBase64String(),
            encName,
            encWebsite,
            encUser,
            encAdditionalInfo,
            encLabels,
            PasswordData(
                encPasswd,
                false,
                null,
                null,
            ),
            TimeData(
                null,
                encExpiresAt,
            ),
            if (encOtpAuthUri != null) OtpData(encOtpAuthUri) else null,
            false,
        )
    }


    companion object {
        const val ATTRIB_UID = "ui"
        const val ATTRIB_NAME = "n"
        const val ATTRIB_ADDITIONAL_INFO = "aI"
        const val ATTRIB_USER = "u"
        const val ATTRIB_PASSWORD = "p"
        const val ATTRIB_WEBSITE = "w"
        const val ATTRIB_EXPIRES_AT = "e"
        const val ATTRIB_OTP = "o"

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
                    jsonObject.get(ATTRIB_EXPIRES_AT)?.asLong,
                    jsonObject.get(ATTRIB_OTP)?.asString,
                )
            } catch (e: Exception) {
                DebugInfo.logException("PCR", "cannot parse json container", e)
                return null
            }
        }

    }
}

