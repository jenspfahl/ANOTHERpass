package de.jepfa.yapm.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.jepfa.yapm.model.encrypted.Encrypted
import java.util.*

/**
 * Encrypted entity for Credential
 */
@Entity(indices = arrayOf(Index("uid", unique = true)))
data class EncCredentialEntity (@PrimaryKey(autoGenerate = true) val id: Int?,
                                val uid: String?,
                                var name: String,
                                var additionalInfo: String,
                                var user: String,
                                var password: String,
                                var lastPassword: String?,
                                var website: String,
                                var labels: String, // encrypted label ids, comma separated
                                var expiresAt: String?,
                                var isObfuscated: Boolean,
                                var isLastPasswordObfuscated: Boolean,
                                val otpData: String?,
                                var modifyTimestamp: Long,
                                var pinned: Boolean,
    ) {

    constructor(id: Int?,
                uid: UUID?,
                name: Encrypted,
                additionalInfo: Encrypted,
                user: Encrypted,
                password: Encrypted,
                lastPassword: Encrypted?,
                website: Encrypted,
                labels: Encrypted,
                expiresAt: Encrypted,
                isObfuscated: Boolean,
                isLastPasswordObfuscated: Boolean?,
                otpData: Encrypted?,
                modifyTimestamp: Long,
                pinned: Boolean,
    ) : this(
        id,
        uid?.let { it.toString() },
        name.toBase64String(),
        additionalInfo.toBase64String(),
        user.toBase64String(),
        password.toBase64String(),
        lastPassword?.toBase64String(),
        website.toBase64String(),
        labels.toBase64String(),
        expiresAt.toBase64String(),
        isObfuscated,
        isLastPasswordObfuscated?:false,
        otpData?.toBase64String(),
        modifyTimestamp,
        pinned,

        )

}