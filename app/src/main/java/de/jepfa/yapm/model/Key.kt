package de.jepfa.yapm.model

data class Key(val data: ByteArray) : Clearable {

    fun toCharArray(): CharArray {
        return data.map { it.toChar() }.toCharArray();
    }

    fun encodeToString(): String {
        return String(data)
    }

    fun debugToString(): String {
        return data.contentToString()
    }

    override fun clear() {
        data.fill(0, 0, data.size)
    }
}