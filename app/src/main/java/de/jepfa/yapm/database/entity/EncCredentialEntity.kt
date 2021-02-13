package de.jepfa.yapm.database.entity

import android.util.Base64
import androidx.room.ColumnInfo
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
            encryptedToBase64String(name),
            encryptedToBase64String(additionalInfo),
            encryptedToBase64String(password), extraPinRequired) {
    }


    companion object {
        private val BAS64_PAIR_DELIMITOR = ":"

        fun encryptedToBase64String(encrypted: Encrypted): String {
            val iv = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP or Base64.NO_PADDING)
            val data = Base64.encodeToString(encrypted.data, Base64.NO_WRAP or Base64.NO_PADDING)
            return iv + BAS64_PAIR_DELIMITOR + data
        }

        fun base64StringToEncrypted(string: String): Encrypted {
            val splitted = string.split(BAS64_PAIR_DELIMITOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val iv = Base64.decode(splitted[0], Base64.NO_WRAP or Base64.NO_PADDING)
            val data = Base64.decode(splitted[1], Base64.NO_WRAP or Base64.NO_PADDING)

            return Encrypted(iv, data)
        }
    }
}