package de.jepfa.yapm.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.jepfa.yapm.model.encrypted.Encrypted

/**
 * Encrypted linked web extensions
 */
@Entity
data class EncWebExtensionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    var webClientId: String,
    var title: String?,
    var extensionPublicKeyAlias: String?,
    var serverKeyPairAlias: String?,
    var linked: Boolean,
    var enabled: Boolean,
    var lastUsedTimestamp: Long?
) {

    constructor(
        id: Int?,
        webClientId: Encrypted,
        title: Encrypted?,
        extensionPublicKeyAlias: Encrypted?,
        serverKeyPairAlias: Encrypted?,
        linked: Boolean,
        enabled: Boolean,
        lastUsedTimestamp: Long?
    ) : this(
        id,
        webClientId.toBase64String(),
        title?.toBase64String(),
        extensionPublicKeyAlias?.toBase64String(),
        serverKeyPairAlias?.toBase64String(),
        linked,
        enabled,
        lastUsedTimestamp
    )

}