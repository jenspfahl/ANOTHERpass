package de.jepfa.yapm.model.encrypted

data class EncWebExtension(val id: Int?,
                           val webClientId: Encrypted,
                           var title: Encrypted,
                           var extensionPublicKey: Encrypted,
                           var sharedBaseKey: Encrypted,
                           var linked: Boolean,
                           var enabled: Boolean,
                           var bypassIncomingRequests: Boolean,
                           var lastUsedTimestamp: Long?,
) {

    constructor(
        id: Int?,
        webClientIdBase64: String,
        titleBase64: String,
        extensionPublicKeyAliasBase64: String,
        sharedBaseKeyBase64: String,
        linked: Boolean,
        enabled: Boolean,
        bypassIncomingRequests: Boolean,
        lastUsedTimestamp: Long?,
    ) : this(
        id,
        Encrypted.fromBase64String(webClientIdBase64),
        Encrypted.fromBase64String(titleBase64),
        Encrypted.fromBase64String(extensionPublicKeyAliasBase64),
        Encrypted.fromBase64String(sharedBaseKeyBase64),
        linked,
        enabled,
        bypassIncomingRequests,
        lastUsedTimestamp
    )

    fun isPersistent(): Boolean {
        return id != null
    }

    fun getServerKeyPairAlias(): String = "$SERVER_KEY_PAIR_ALIAS_PREFIX$id"

    companion object {
        const val EXTRA_WEB_EXTENSION_ID = "de.jepfa.yapm.ui.web_extension.id"
        const val SERVER_KEY_PAIR_ALIAS_PREFIX = "webExt/curr/"

    }
}