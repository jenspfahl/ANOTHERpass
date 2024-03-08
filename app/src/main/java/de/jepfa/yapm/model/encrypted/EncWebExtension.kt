package de.jepfa.yapm.model.encrypted

data class EncWebExtension(val id: Int?,
                           val webClientId: Encrypted,
                           var title: Encrypted,
                           var extensionPublicKey: Encrypted?,
                           var linked: Boolean,
                           var enabled: Boolean,
                           var bypassIncomingRequests: Boolean,
                           var lastUsedTimestamp: Long?,
) {

    constructor(
        id: Int?,
        webClientIdBase64: String,
        titleBase64: String,
        extensionPublicKeyAliasBase64: String?,
        linked: Boolean,
        enabled: Boolean,
        bypassIncomingRequests: Boolean,
        lastUsedTimestamp: Long?,
    ) : this(
        id,
        Encrypted.fromBase64String(webClientIdBase64),
        Encrypted.fromBase64String(titleBase64),
        extensionPublicKeyAliasBase64?.let { Encrypted.fromBase64String(it) },
        linked,
        enabled,
        bypassIncomingRequests,
        lastUsedTimestamp
    )

    fun isPersistent(): Boolean {
        return id != null
    }

    companion object {
        const val EXTRA_WEB_EXTENSION_ID = "de.jepfa.yapm.ui.web_extension.id"

    }
}