package de.jepfa.yapm.model

import de.jepfa.yapm.service.secret.SecretService
import java.util.*

class Key : Secret {

    constructor(data: ByteArray): super(data)

    fun debugToString(): String {
        return data.contentToString()
    }

}