package de.jepfa.yapm.model

import java.util.*

data class Encrypted(val iv: ByteArray, val data: ByteArray) {
    fun debugToString(): String {
        return "[iv=${iv.contentToString()}, data=${data.contentToString()}]"
    }
}