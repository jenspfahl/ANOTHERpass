package de.jepfa.yapm.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.jepfa.yapm.model.encrypted.Encrypted

/**
 * Encrypted linked web extensions
 */
@Entity(indices = arrayOf(Index("webClientId", unique = true)))
data class EncWebExtensionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    var webClientId: String,
    var title: String,
    var extensionPublicKeyAlias: String,
    var linked: Boolean,
    var enabled: Boolean,
    var bypassIncomingRequests: Boolean,
    var lastUsedTimestamp: Long?
) {

    constructor(
        id: Int?,
        webClientId: Encrypted,
        title: Encrypted,
        extensionPublicKey: Encrypted,
        linked: Boolean,
        enabled: Boolean,
        bypassIncomingRequests: Boolean,
        lastUsedTimestamp: Long?
    ) : this(
        id,
        webClientId.toBase64String(),
        title.toBase64String(),
        extensionPublicKey.toBase64String(),
        linked,
        enabled,
        bypassIncomingRequests,
        lastUsedTimestamp
    )

}