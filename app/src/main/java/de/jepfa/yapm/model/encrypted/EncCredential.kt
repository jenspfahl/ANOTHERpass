package de.jepfa.yapm.model.encrypted

import android.content.Intent
import com.google.gson.JsonElement
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.getEncryptedExtra
import de.jepfa.yapm.util.putEncryptedExtra
import java.util.*

data class PasswordData(
    var password: Encrypted,
    var isObfuscated: Boolean,
    var lastPassword: Encrypted?,
    var isLastPasswordObfuscated: Boolean?
    ) {

    fun backupForRestore() {
        backupForRestore(this)
    }

    fun backupForRestore(other: PasswordData) {
        lastPassword = other.password
        isLastPasswordObfuscated = other.isObfuscated
    }

    fun restore() {
        lastPassword?.let {
            password = it
        }
        isLastPasswordObfuscated?.let {
            isObfuscated = it
        }
    }
}

data class TimeData(
    var modifyTimestamp: Long?,
    var expiresAt: Encrypted,  //enc(Date.getTime() or 0L) or enc.empty
    ) {
    fun touchModify() {
        modifyTimestamp = System.currentTimeMillis()
    }
}

data class OtpData(
    var encOtpAuthUri: Encrypted,
)

data class EncCredential(
    val id: Int?,
    var uid: UUID?,
    override var name: Encrypted,
    var website: Encrypted,
    var user: Encrypted,
    var additionalInfo: Encrypted,
    var labels: Encrypted, // enc(comma-separated labelIds or "")
    val passwordData: PasswordData,
    val timeData: TimeData,
    var otpData: OtpData?
): EncNamed {

    constructor(id: Int?,
                uid: String?,
                nameBase64: String,
                additionalInfoBase64: String,
                userBase64: String,
                passwordBase64: String,
                lastPasswordBase64: String?,
                websiteBase64: String,
                labelsBase64: String,
                expiresAtBase64: String?,
                isObfuscated: Boolean,
                isLastPasswordObfuscated: Boolean?,
                modifyTimestamp: Long?) :
            this(
                id,
                uid?.let { UUID.fromString(uid) },
                Encrypted.fromBase64String(nameBase64),
                Encrypted.fromBase64String(websiteBase64),
                Encrypted.fromBase64String(userBase64),
                Encrypted.fromBase64String(additionalInfoBase64),
                Encrypted.fromBase64String(labelsBase64),
                PasswordData(
                    Encrypted.fromBase64String(passwordBase64),
                    isObfuscated,
                    lastPasswordBase64?.run { Encrypted.fromBase64String(lastPasswordBase64) },
                    isLastPasswordObfuscated,
                ),
                TimeData(
                    modifyTimestamp,
                    if (expiresAtBase64 != null) Encrypted.fromBase64String(expiresAtBase64) else Encrypted.empty(),
                ),
                null // TODO OtpData
            )



    fun isPersistent(): Boolean {
        return id != null
    }

    fun copyData(other: EncCredential) {
        name = other.name
        labels = other.labels
        user = other.user
        website = other.website
        additionalInfo = other.additionalInfo
        timeData.expiresAt = other.timeData.expiresAt
        passwordData.isObfuscated = other.passwordData.isObfuscated
        passwordData.password = other.passwordData.password
    }


    fun applyExtras(intent: Intent) {
        intent.putExtra(EXTRA_CREDENTIAL_ID, id)
        intent.putExtra(EXTRA_CREDENTIAL_UID, uid?.toString())
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_NAME, name)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_ADDITIONAL_INFO, additionalInfo)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_USER, user)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_PASSWORD, passwordData.password)
        passwordData.lastPassword?.let { intent.putEncryptedExtra(EXTRA_CREDENTIAL_LAST_PASSWORD, it) }
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_WEBSITE, website)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_LABELS, labels)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_EXPIRES_AT, timeData.expiresAt)
        intent.putExtra(EXTRA_CREDENTIAL_IS_OBFUSCATED, passwordData.isObfuscated)
        intent.putExtra(EXTRA_CREDENTIAL_IS_LAST_PASSWORD_OBFUSCATED, passwordData.isLastPasswordObfuscated)
        intent.putExtra(EXTRA_CREDENTIAL_MODIFY_TIMESTAMP, timeData.modifyTimestamp)

    }

    fun isExpired(key: SecretKeyHolder): Boolean {
        val expiresAt = SecretService.decryptLong(key, timeData.expiresAt)
        if (expiresAt != null && expiresAt > 0) {
            val expiryDate = Date(expiresAt)
            val now = Date()
            return expiryDate.before(now)
        }
        else {
            return false
        }
    }

    // Equals not by last? dates and timestamp
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncCredential

        if (id != other.id) return false
        if (uid != other.uid) return false
        if (name != other.name) return false
        if (additionalInfo != other.additionalInfo) return false
        if (user != other.user) return false
        if (passwordData.password != other.passwordData.password) return false
        if (website != other.website) return false
        if (labels != other.labels) return false
        if (timeData.expiresAt != other.timeData.expiresAt) return false
        if (passwordData.isObfuscated != other.passwordData.isObfuscated) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + uid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + additionalInfo.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + passwordData.password.hashCode()
        result = 31 * result + website.hashCode()
        result = 31 * result + labels.hashCode()
        result = 31 * result + timeData.expiresAt.hashCode()
        result = 31 * result + passwordData.isObfuscated.hashCode()
        return result
    }


    companion object {
        const val EXTRA_CREDENTIAL_ID = "de.jepfa.yapm.ui.credential.id"
        const val EXTRA_CREDENTIAL_UID = "de.jepfa.yapm.ui.credential.uid"
        const val EXTRA_CREDENTIAL_NAME = "de.jepfa.yapm.ui.credential.name"
        const val EXTRA_CREDENTIAL_ADDITIONAL_INFO = "de.jepfa.yapm.ui.credential.additionalInfo"
        const val EXTRA_CREDENTIAL_USER = "de.jepfa.yapm.ui.credential.user"
        const val EXTRA_CREDENTIAL_PASSWORD = "de.jepfa.yapm.ui.credential.password"
        const val EXTRA_CREDENTIAL_LAST_PASSWORD = "de.jepfa.yapm.ui.credential.lastpassword"
        const val EXTRA_CREDENTIAL_WEBSITE = "de.jepfa.yapm.ui.credential.website"
        const val EXTRA_CREDENTIAL_LABELS = "de.jepfa.yapm.ui.credential.labels"
        const val EXTRA_CREDENTIAL_EXPIRES_AT = "de.jepfa.yapm.ui.credential.expiresAt"
        const val EXTRA_CREDENTIAL_IS_OBFUSCATED = "de.jepfa.yapm.ui.credential.isObfuscated"
        const val EXTRA_CREDENTIAL_IS_LAST_PASSWORD_OBFUSCATED = "de.jepfa.yapm.ui.credential.isLastPasswordObfuscated"
        const val EXTRA_CREDENTIAL_MODIFY_TIMESTAMP = "de.jepfa.yapm.ui.credential.modifyTimestamp"

        const val ATTRIB_ID = "id"
        const val ATTRIB_UID = "uid"
        const val ATTRIB_NAME = "name"
        const val ATTRIB_ADDITIONAL_INFO = "additionalInfo"
        const val ATTRIB_USER = "user"
        const val ATTRIB_PASSWORD = "password"
        const val ATTRIB_LAST_PASSWORD = "lastPassword"
        const val ATTRIB_WEBSITE = "website"
        const val ATTRIB_LABELS = "labels"
        const val ATTRIB_EXPIRES_AT = "expiresAt"
        const val ATTRIB_IS_OBFUSCATED = "isObfuscated"
        const val ATTRIB_IS_LAST_PASSWORD_OBFUSCATED = "isLastPasswordObfuscated"
        const val ATTRIB_MODIFY_TIMESTAMP = "modifyTimestamp"

        fun fromIntent(intent: Intent, createUuid: Boolean = false): EncCredential {
            var id: Int? = null
            val idExtra = intent.getIntExtra(EXTRA_CREDENTIAL_ID, -1)
            if (idExtra != -1) {
                id = idExtra
            }
            var uid = intent.getStringExtra(EXTRA_CREDENTIAL_UID)?.let { UUID.fromString(it) }
            val encName = intent.getEncryptedExtra(EXTRA_CREDENTIAL_NAME, Encrypted.empty())
            val encAdditionalInfo = intent.getEncryptedExtra(EXTRA_CREDENTIAL_ADDITIONAL_INFO, Encrypted.empty())
            val encUser = intent.getEncryptedExtra(EXTRA_CREDENTIAL_USER, Encrypted.empty())
            val encPassword = intent.getEncryptedExtra(EXTRA_CREDENTIAL_PASSWORD, Encrypted.empty())
            val encLastPassword = intent.getEncryptedExtra(EXTRA_CREDENTIAL_LAST_PASSWORD)
            val encWebsite = intent.getEncryptedExtra(EXTRA_CREDENTIAL_WEBSITE, Encrypted.empty())
            val encLabels = intent.getEncryptedExtra(EXTRA_CREDENTIAL_LABELS, Encrypted.empty())
            val encExpiresAt = intent.getEncryptedExtra(EXTRA_CREDENTIAL_EXPIRES_AT, Encrypted.empty())
            val isObfuscated = intent.getBooleanExtra(EXTRA_CREDENTIAL_IS_OBFUSCATED, false)
            val isLastPasswordObfuscated = intent.getBooleanExtra(
                EXTRA_CREDENTIAL_IS_LAST_PASSWORD_OBFUSCATED, false)
            var modifyTimestamp = intent.getLongExtra(EXTRA_CREDENTIAL_MODIFY_TIMESTAMP, 0)

            if (uid == null && createUuid) {
                uid = UUID.randomUUID()
            }

            return EncCredential(
                id,
                uid,
                encName,
                encWebsite,
                encUser,
                encAdditionalInfo,
                encLabels,
                PasswordData(
                    encPassword,
                    isObfuscated,
                    encLastPassword,
                    isLastPasswordObfuscated,
                ),
                TimeData(
                    modifyTimestamp,
                    encExpiresAt,
                )
                , null //TODO OTP
            )
        }

        fun fromJson(json: JsonElement): EncCredential? {
            try {
                val jsonObject = json.asJsonObject
                return EncCredential(
                    jsonObject.get(ATTRIB_ID).asInt,
                    jsonObject.get(ATTRIB_UID)?.asString,
                    jsonObject.get(ATTRIB_NAME).asString,
                    jsonObject.get(ATTRIB_ADDITIONAL_INFO).asString,
                    jsonObject.get(ATTRIB_USER).asString,
                    jsonObject.get(ATTRIB_PASSWORD).asString,
                    jsonObject.get(ATTRIB_LAST_PASSWORD)?.asString,
                    jsonObject.get(ATTRIB_WEBSITE).asString,
                    jsonObject.get(ATTRIB_LABELS).asString,
                    jsonObject.get(ATTRIB_EXPIRES_AT)?.asString,
                    jsonObject.get(ATTRIB_IS_OBFUSCATED)?.asBoolean ?: false,
                    jsonObject.get(ATTRIB_IS_LAST_PASSWORD_OBFUSCATED)?.asBoolean ?: false,
                    jsonObject.get(ATTRIB_MODIFY_TIMESTAMP)?.asLong
                )
            } catch (e: Exception) {
                DebugInfo.logException("ENCC", "cannot parse json container", e)
                return null
            }
        }
    }
}