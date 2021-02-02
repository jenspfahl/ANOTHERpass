package de.jepfa.yapm.model

data class Password(val data: CharArray) : Clearable {
    constructor(passwd: String) : this(passwd.toCharArray()) {
    }

    fun toByteArray(): ByteArray {
        return data.map { it.toByte() }.toByteArray();
    }

    fun debugToString(): String {
        return String(data)
    }

    override fun clear() {
        data.fill('0', 0, data.size)
    }
}