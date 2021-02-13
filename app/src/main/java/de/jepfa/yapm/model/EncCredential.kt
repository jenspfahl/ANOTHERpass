package de.jepfa.yapm.model

import de.jepfa.yapm.database.entity.EncCredentialEntity

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
                    EncCredentialEntity.base64StringToEncrypted(nameBase64),
                    EncCredentialEntity.base64StringToEncrypted(additionalInfoBase64),
                    EncCredentialEntity.base64StringToEncrypted(passwordBase64),
                    extraPinRequired)
}