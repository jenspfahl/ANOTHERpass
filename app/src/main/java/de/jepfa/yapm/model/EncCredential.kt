package de.jepfa.yapm.model

data class EncCredential(var id: Int?,
                         var name: Encrypted,
                         var additionalInfo: Encrypted,
                         var password: Encrypted,
                         var extraPinRequired: Boolean = false) {
}