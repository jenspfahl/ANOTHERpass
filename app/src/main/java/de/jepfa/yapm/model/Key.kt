package de.jepfa.yapm.model

import de.jepfa.yapm.service.secret.SecretService
import java.util.*

data class Key(val data: ByteArray) : Clearable, Validable {

    fun toCharArray(): CharArray {
        return data.map { it.toChar() }.toCharArray();
    }

    fun encodeToString(): String {
        return String(data)
    }

    override fun isValid(): Boolean {
        return Arrays.equals(data, Validable.FAILED_BYTE_ARRAY)
    }

    fun debugToString(): String {
        return data.contentToString()
    }

    override fun clear() {
        data.fill(0, 0, data.size)
    }

}