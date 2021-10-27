package de.jepfa.yapm.model.secret

import android.text.Editable
import javax.crypto.SecretKey

/**
 * Represents a key which is secret to others.
 */
class Key : Secret {

    constructor(data: ByteArray): super(data)

    fun debugToString(): String {
        return data.contentToString()
    }
}