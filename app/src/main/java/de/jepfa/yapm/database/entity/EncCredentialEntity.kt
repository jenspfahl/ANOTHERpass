package de.jepfa.yapm.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.jepfa.yapm.model.Encrypted

/**
 * Encrypted entity for Credential
 */
@Entity
data class EncCredentialEntity (@PrimaryKey(autoGenerate = true) val id: Int?,
                                var name: String,
                                var additionalInfo: String,
                                var password: String,
                                var extraPinRequired: Boolean) {

    constructor(id: Int?,
                name: Encrypted,
                additionalInfo: Encrypted,
                password: Encrypted,
                extraPinRequired: Boolean)
            : this(
            id,
            name.toBase64String(),
            additionalInfo.toBase64String(),
            password.toBase64String(),
            extraPinRequired) {
    }

}