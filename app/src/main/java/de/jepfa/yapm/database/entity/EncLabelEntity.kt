package de.jepfa.yapm.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.jepfa.yapm.model.encrypted.Encrypted
import java.util.*

/**
 * Encrypted labels for Credential
 */
@Entity(indices = arrayOf(Index("uid", unique = true)))
data class EncLabelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    val uid: String?,
    var name: String,
    var description: String,
    var color: Int?
) {

    constructor(id: Int?, uid: UUID?, name: Encrypted, description: Encrypted, color: Int?)
            : this(id, uid?.let { it.toString() }, name.toBase64String(), description.toBase64String(), color)

}