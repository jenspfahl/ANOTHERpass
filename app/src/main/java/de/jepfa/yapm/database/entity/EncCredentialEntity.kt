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
                                var lastPassword: String?,
                                var website: String,
                                var labels: String, // encrypted label ids, comma separated
                                var isObfuscated: Boolean,
                                var isLastPasswordObfuscated: Boolean) {

    constructor(id: Int?,
                name: Encrypted,
                additionalInfo: Encrypted,
                user: Encrypted,
                password: Encrypted,
                lastPassword: Encrypted?,
                website: Encrypted,
                labels: Encrypted,
                isObfuscated: Boolean,
                isLastPasswordObfuscated: Boolean?
    ) : this(
        id,
        name.toBase64String(),
        additionalInfo.toBase64String(),
        user.toBase64String(),
        password.toBase64String(),
        lastPassword?.toBase64String(),
        website.toBase64String(),
        labels.toBase64String(),
        isObfuscated,
        isLastPasswordObfuscated?:false)

}