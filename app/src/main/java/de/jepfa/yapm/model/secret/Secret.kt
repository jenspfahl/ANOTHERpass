package de.jepfa.yapm.model.secret

import android.util.Base64
import de.jepfa.yapm.model.Clearable
import de.jepfa.yapm.model.Validable
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*

open class Secret(var data: ByteArray) : Clearable, Validable {

    fun isEmpty() : Boolean {
        return data.isEmpty()
    }

    fun isEqual(other: Secret): Boolean {
        return Arrays.equals(data, other.data)
    }

    override fun isValid(): Boolean {
        return !Arrays.equals(toByteArray(), Validable.FAILED_BYTE_ARRAY)
    }

    fun add(other: Secret) {
        val buffer = data + other.data
        clear()
        other.clear()
        data = buffer
    }

    fun toByteArray(): ByteArray {
        return data
    }

    fun toCharArray(): CharArray {
        return data.map { it.toChar() }.toCharArray()
    }

    fun toCharArray(charset: Charset): CharArray {
        val charBuffer = charset.decode(ByteBuffer.wrap(data))
        return charBuffer.array()
    }

    override fun clear() {
        data.fill(0, 0, data.size)
    }

}