package de.jepfa.yapm.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.jepfa.yapm.model.encrypted.Encrypted

/**
 * Encrypted labels for Credential
 */
@Entity
data class EncLabelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    var name: String,
    var description: String,
    var color: Int?
) {

    constructor(id: Int?, name: Encrypted, description: Encrypted, color: Int?)
            : this(id, name.toBase64String(), description.toBase64String(), color)

}