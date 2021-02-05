package de.jepfa.yapm.model

import java.nio.CharBuffer

data class Password(val data: CharArray) : Clearable {
    constructor(passwd: String) : this(passwd.toCharArray()) {
    }

    constructor(passwd: ByteArray) : this(passwd.map { it.toChar() }.toCharArray()) {
    }

    fun add(other: Password): Password {
        val buffer = data + other.data
        return Password(buffer)
    }
    fun add(other: Char): Password {
        val buffer = data + other
        return Password(buffer)
    }

    fun toByteArray(): ByteArray {
        return data.map { it.toByte() }.toByteArray();
    }

    fun debugToString(): String {
        var presentation = "";
        for (i in 0 until data.size) {
            if (i != 0 && i % 4 == 0) {
                presentation += "-"
            }
            presentation += data[i]
        }
        return presentation
    }

    override fun clear() {
        data.fill('0', 0, data.size)
    }
}