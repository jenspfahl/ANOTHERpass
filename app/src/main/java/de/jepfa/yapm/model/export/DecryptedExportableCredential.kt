package de.jepfa.yapm.model.export

import android.net.Uri
import com.google.gson.JsonElement
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.OtpData
import de.jepfa.yapm.model.encrypted.PasswordData
import de.jepfa.yapm.model.encrypted.TimeData
import de.jepfa.yapm.model.otp.OtpAlgorithm
import de.jepfa.yapm.model.otp.OtpConfig
import de.jepfa.yapm.model.otp.OtpConfig.Companion.stringToBase32Key
import de.jepfa.yapm.model.otp.OtpMode
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.toUUIDFromBase64String


data class DecryptedExportableCredential(
    val i: Int, // id
    val ui: String?, // uid
    val n: String, // name
    val aI: String, // additional info
    val u: String, // user
    val p: Password, // password
    val w: String, // website
    val l: String, // labelIds, comma-separated
    val e: Long?, // Date.getTime()
    val o: String?, // shortened OTP
    val v: Boolean, // veiled
    val m: Long?, // modify timestamp
) {
    fun toEncCredential(key: SecretKeyHolder): EncCredential {
        val encName = SecretService.encryptCommonString(key, n)
        val encAdditionalInfo = SecretService.encryptCommonString(key, aI)
        val encUser = SecretService.encryptCommonString(key, u)
        val encPasswd = SecretService.encryptPassword(key, p)
        val encWebsite = SecretService.encryptCommonString(key, w)
        val encExpiresAt = if (e != null) SecretService.encryptLong(key, e) else SecretService.encryptLong(key, 0L)
        val encLabels = SecretService.encryptCommonString(key, l)
        val encOtpAuthUri = OtpConfig.createFromPacked(o, n, u)?.let {  SecretService.encryptCommonString(key, it.toString()) }

        p.clear()
        return EncCredential(
            i,
            ui?.toUUIDFromBase64String(),
            encName,
            encWebsite,
            encUser,
            encAdditionalInfo,
            encLabels,
            PasswordData(
                encPasswd,
                v,
                null,
                null,
            ),
            TimeData(
                m,
                encExpiresAt,
            ),
            if (encOtpAuthUri != null) OtpData(encOtpAuthUri) else null,
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
        const val ATTRIB_LABEL_IDS = "l"
        const val ATTRIB_EXPIRES_AT = "e"
        const val ATTRIB_OTP = "o"
        const val ATTRIB_VEILED = "v"
        const val ATTRIB_MODIFIED_AT = "m"

        fun fromJson(json: JsonElement): DecryptedExportableCredential? {
            try {
                val jsonObject = json.asJsonObject
                return DecryptedExportableCredential(
                    jsonObject.get(ATTRIB_ID).asInt,
                    jsonObject.get(ATTRIB_UID)?.asString,
                    jsonObject.get(ATTRIB_NAME).asString,
                    jsonObject.get(ATTRIB_ADDITIONAL_INFO).asString,
                    jsonObject.get(ATTRIB_USER).asString,
                    Password.fromBase64String(jsonObject.get(ATTRIB_PASSWORD).asString),
                    jsonObject.get(ATTRIB_WEBSITE).asString,
                    jsonObject.get(ATTRIB_LABEL_IDS).asString,
                    jsonObject.get(ATTRIB_EXPIRES_AT)?.asLong,
                    jsonObject.get(ATTRIB_OTP)?.asString,
                    jsonObject.get(ATTRIB_VEILED).asBoolean,
                    jsonObject.get(ATTRIB_MODIFIED_AT)?.asLong,
                )
            } catch (e: Exception) {
                DebugInfo.logException("PCR", "cannot parse json container", e)
                return null
            }
        }


    }
}

