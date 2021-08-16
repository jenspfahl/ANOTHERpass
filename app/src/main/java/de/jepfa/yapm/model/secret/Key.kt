package de.jepfa.yapm.model.secret

import android.text.Editable

class Key : Secret {

    constructor(data: ByteArray): super(data)

    fun debugToString(): String {
        return data.contentToString()
    }
}