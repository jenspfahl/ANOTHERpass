package de.jepfa.yapm.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.jepfa.yapm.model.Encrypted

/**
 * Encrypted entity for Credential
 */
@Entity
data class EncLabelEntity (@PrimaryKey(autoGenerate = false) val name: String,
                           var color: Int?) {

    constructor(name: Encrypted, color: Int?)
            : this(name.toBase64String(), color)

}