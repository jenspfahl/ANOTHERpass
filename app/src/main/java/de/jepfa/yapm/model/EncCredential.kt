package de.jepfa.yapm.model

data class EncCredential(var id: Int?,
                         var name: Encrypted,
                         var additionalInfo: Encrypted,
                         var password: Encrypted,
                         var extraPinRequired: Boolean = false) {

    fun isPersistent(): Boolean {
        return id != null
    }

    constructor(id: Int?,
                nameBase64: String,
                additionalInfoBase64: String,
                passwordBase64: String,
                extraPinRequired: Boolean) :
            this(id,
                    Encrypted.fromBase64String(nameBase64),
                    Encrypted.fromBase64String(additionalInfoBase64),
                    Encrypted.fromBase64String(passwordBase64),
                    extraPinRequired)

    companion object {
        const val EXTRA_CREDENTIAL_ID = "de.jepfa.yapm.ui.save_credential.id"
        const val EXTRA_CREDENTIAL_NAME = "de.jepfa.yapm.ui.save_credential.name"
        const val EXTRA_CREDENTIAL_ADDITIONAL_INFO = "de.jepfa.yapm.ui.save_credential.additionalInfo"
        const val EXTRA_CREDENTIAL_PASSWORD = "de.jepfa.yapm.ui.save_credential.password"
    }
}