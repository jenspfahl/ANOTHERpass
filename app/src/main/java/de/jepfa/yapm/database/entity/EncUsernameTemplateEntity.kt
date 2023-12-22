package de.jepfa.yapm.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.jepfa.yapm.model.encrypted.Encrypted
import java.util.*

/**
 * Encrypted templates for usernames
 */
@Entity
data class EncUsernameTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    var username: String,
    var generatorType: String
) {

    constructor(id: Int?, username: Encrypted, generatorType: Encrypted)
            : this(id, username.toBase64String(), generatorType.toBase64String())

}