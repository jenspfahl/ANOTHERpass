package de.jepfa.yapm.model

data class Credential(private var name: String,
                      private var additionalInfo: String = "",
                      private var password: Password,
                      private var extraPinRequired: Boolean) {
}