package de.jepfa.yapm.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.jepfa.yapm.model.encrypted.Encrypted

/**
 * Encrypted entity for Credential
 */
@Entity
data class EncCredentialEntity (@PrimaryKey(autoGenerate = true) val id: Int?,
                                var name: String,
                                var additionalInfo: String,
                                var user: String,
                                var password: String,
                                var website: String,
                                var labels: String) { // encrypted label ids, comma separated

    constructor(id: Int?,
                name: Encrypted,
                additionalInfo: Encrypted,
                user: Encrypted,
                password: Encrypted,
                website: Encrypted,
                labels: Encrypted
    )
            : this(
        id,
        name.toBase64String(),
        additionalInfo.toBase64String(),
        user.toBase64String(),
        password.toBase64String(),
        website.toBase64String(),
        labels.toBase64String())

}