package de.jepfa.yapm.model

data class EncCredential(var id: Int?,
                         var name: Encrypted,
                         var additionalInfo: Encrypted,
                         var password: Encrypted,
                         var extraPinRequired: Boolean = false) {

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
}