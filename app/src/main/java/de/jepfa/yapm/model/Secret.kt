package de.jepfa.yapm.model

import android.text.Editable
import de.jepfa.yapm.util.PreferenceUtil
import java.lang.reflect.TypeVariable
import java.nio.CharBuffer
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
        return data.map { it.toChar() }.toCharArray();
    }

    override fun clear() {
        data.fill(0, 0, data.size)
    }

}