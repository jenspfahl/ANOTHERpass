package de.jepfa.yapm.model.encrypted

import android.content.Intent
import de.jepfa.yapm.util.getEncryptedExtra
import de.jepfa.yapm.util.putEncryptedExtra

data class EncCredential(var id: Int?,
                         var name: Encrypted,
                         var additionalInfo: Encrypted,
                         var user: Encrypted,
                         var password: Encrypted,
                         var lastPassword: Encrypted?,
                         var website: Encrypted,
                         var labels: Encrypted
) {

    constructor(id: Int?,
                nameBase64: String,
                additionalInfoBase64: String,
                userBase64: String,
                passwordBase64: String,
                lastPasswordBase64: String?,
                websiteBase64: String,
                labelsBase64: String) :
            this(id,
                Encrypted.fromBase64String(nameBase64),
                Encrypted.fromBase64String(additionalInfoBase64),
                Encrypted.fromBase64String(userBase64),
                Encrypted.fromBase64String(passwordBase64),
                lastPasswordBase64?.run { Encrypted.fromBase64String(lastPasswordBase64) },
                Encrypted.fromBase64String(websiteBase64),
                Encrypted.fromBase64String(labelsBase64)
            )



    fun isPersistent(): Boolean {
        return id != null
    }

    fun applyExtras(intent: Intent) {
        intent.putExtra(EXTRA_CREDENTIAL_ID, id)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_NAME, name)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_ADDITIONAL_INFO, additionalInfo)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_USER, user)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_PASSWORD, password)
        lastPassword?.let { intent.putEncryptedExtra(EXTRA_CREDENTIAL_LAST_PASSWORD, it) }
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_WEBSITE, website)
        intent.putEncryptedExtra(EXTRA_CREDENTIAL_LABELS, labels)
    }


    companion object {
        const val EXTRA_CREDENTIAL_ID = "de.jepfa.yapm.ui.credential.id"
        const val EXTRA_CREDENTIAL_NAME = "de.jepfa.yapm.ui.credential.name"
        const val EXTRA_CREDENTIAL_ADDITIONAL_INFO = "de.jepfa.yapm.ui.credential.additionalInfo"
        const val EXTRA_CREDENTIAL_USER = "de.jepfa.yapm.ui.credential.user"
        const val EXTRA_CREDENTIAL_PASSWORD = "de.jepfa.yapm.ui.credential.password"
        const val EXTRA_CREDENTIAL_LAST_PASSWORD = "de.jepfa.yapm.ui.credential.lastpassword"
        const val EXTRA_CREDENTIAL_WEBSITE = "de.jepfa.yapm.ui.credential.website"
        const val EXTRA_CREDENTIAL_LABELS = "de.jepfa.yapm.ui.credential.labels"

        const val ATTRIB_ID = "id"
        const val ATTRIB_NAME = "name"
        const val ATTRIB_ADDITIONAL_INFO = "additionalInfo"
        const val ATTRIB_USER = "user"
        const val ATTRIB_PASSWORD = "password"
        const val ATTRIB_WEBSITE = "website"
        const val ATTRIB_LABELS = "labels"

        fun fromIntent(intent: Intent): EncCredential {
            var id: Int? = null
            val idExtra = intent.getIntExtra(EXTRA_CREDENTIAL_ID, -1)
            if (idExtra != -1) {
                id = idExtra
            }
            val encName = intent.getEncryptedExtra(EXTRA_CREDENTIAL_NAME, Encrypted.empty())
            val encAdditionalInfo = intent.getEncryptedExtra(EXTRA_CREDENTIAL_ADDITIONAL_INFO, Encrypted.empty())
            val encUser = intent.getEncryptedExtra(EXTRA_CREDENTIAL_USER, Encrypted.empty())
            val encPassword = intent.getEncryptedExtra(EXTRA_CREDENTIAL_PASSWORD, Encrypted.empty())
            val encLastPassword = intent.getEncryptedExtra(EXTRA_CREDENTIAL_LAST_PASSWORD)
            val encWebsite = intent.getEncryptedExtra(EXTRA_CREDENTIAL_WEBSITE, Encrypted.empty())
            val encLabels = intent.getEncryptedExtra(EXTRA_CREDENTIAL_LABELS, Encrypted.empty())

            return EncCredential(
                id, encName, encAdditionalInfo, encUser, encPassword, encLastPassword, encWebsite, encLabels)
        }
    }
}