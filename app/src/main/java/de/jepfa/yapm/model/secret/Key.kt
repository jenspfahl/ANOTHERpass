package de.jepfa.yapm.model.secret

class Key : Secret {

    constructor(data: ByteArray): super(data)

    fun debugToString(): String {
        return data.contentToString()
    }

}