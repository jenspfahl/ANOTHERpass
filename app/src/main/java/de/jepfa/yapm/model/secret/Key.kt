package de.jepfa.yapm.model.secret

/**
 * Represents a key which is secret to others.
 */
class Key : Secret {

    constructor(data: ByteArray): super(data)

    fun debugToString(): String {
        return data.contentToString()
    }
}