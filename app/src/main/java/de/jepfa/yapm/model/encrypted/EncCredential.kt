package de.jepfa.yapm.model.encrypted

import android.content.Intent
import android.util.Log
import com.google.gson.JsonElement
import de.jepfa.yapm.util.getEncryptedExtra
import de.jepfa.yapm.util.putEncryptedExtra
import java.util.*

data class EncCredential(val id: Int?,
                         val uid: UUID?,
                         override var name: Encrypted,
                         var additionalInfo: Encrypted,
                         var user: Encrypted,
                         var password: Encrypted,
                         var lastPassword: Encrypted?,
                         var website: Encrypted,
                         var labels: Encrypted,  // enc(comma-separated labelIds or "")
                         var expiresAt: Encrypted,  //enc(Date.getTime() or 0L) or enc.empty
                         var isObfuscated: Boolean,
                         var isLastPasswordObfuscated: Boolean?,
                         var modifyTimestamp: Long?
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
            this(id,
                uid?.let { UUID.fromString(uid) },
                Encrypted.fromBase64String(nameBase64),
                Encrypted.fromBase64String(additionalInfoBase64),
                Encrypted.fromBase64String(userBase64),
                Encrypted.fromBase64String(passwordBase64),
                lastPasswordBase64?.run { Encrypted.fromBase64String(lastPasswordBase64) },
                Encrypted.fromBase64String(websiteBase64),
                Encrypted.fromBase64String(labelsBase64),
                if (expiresAtBase64 != null) Encrypted.fromBase64String(expiresAtBase64) else Encrypted.empty(),
                isObfuscated,
                isLastPasswordObfuscated,
                modifyTimestamp
            )



    fun isPersistent(): Boolean {
        return id != null
    }

    fun backupForRestore() {
        backupForRestore(this)
    }

    fun backupForRestore(other: EncCredential) {
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

    fun copyData(other: EncCredential) {
        name = other.name
        labels = other.labels
        user = other.user
        website = other.website
        additionalInfo = other.additionalInfo
        expiresAt = other.expiresAt
        isObfuscated = other.isObfuscated
        password = other.password
    }

    fun touchModify() {
        modifyTimestamp = System.currentTimeMillis()
    }

    fun applyExtras(intent: Intent) {
        intent.putExtra(EXTRA_CREDENTIAL_ID, id)
        intent.putExtra(EXTRA_CREDENTIAL_UID, uid?.toString())
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_NAME, name)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_ADDITIONAL_INFO, additionalInfo)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_USER, user)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_PASSWORD, password)
        lastPassword?.let { intent.putEncryptedExtra(EXTRA_CREDENTIAL_LAST_PASSWORD, it) }
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_WEBSITE, website)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_LABELS, labels)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_EXPIRES_AT, expiresAt)
        intent.putExtra(EXTRA_CREDENTIAL_IS_OBFUSCATED, isObfuscated)
        intent.putExtra(EXTRA_CREDENTIAL_IS_LAST_PASSWORD_OBFUSCATED, isLastPasswordObfuscated)
        intent.putExtra(EXTRA_CREDENTIAL_MODIFY_TIMESTAMP, modifyTimestamp)

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
        if (password != other.password) return false
        if (website != other.website) return false
        if (labels != other.labels) return false
        if (expiresAt != other.expiresAt) return false
        if (isObfuscated != other.isObfuscated) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + uid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + additionalInfo.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + website.hashCode()
        result = 31 * result + labels.hashCode()
        result = 31 * result + expiresAt.hashCode()
        result = 31 * result + isObfuscated.hashCode()
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
                id, uid, encName, encAdditionalInfo, encUser, encPassword, encLastPassword, encWebsite, encLabels,
                encExpiresAt, isObfuscated, isLastPasswordObfuscated, modifyTimestamp)
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
                    jsonObject.get(ATTRIB_EXPIRES_AT).asString,
                    jsonObject.get(ATTRIB_IS_OBFUSCATED)?.asBoolean ?: false,
                    jsonObject.get(ATTRIB_IS_LAST_PASSWORD_OBFUSCATED)?.asBoolean ?: false,
                    jsonObject.get(ATTRIB_MODIFY_TIMESTAMP)?.asLong
                )
            } catch (e: Exception) {
                Log.e("ENCC", "cannot parse json container", e)
                return null
            }
        }
    }
}