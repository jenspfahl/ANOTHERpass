package de.jepfa.yapm.model.encrypted

import android.util.Base64
import android.util.Log
import de.jepfa.yapm.util.Constants.LOG_PREFIX

data class Encrypted(val type: EncryptedType?, val iv: ByteArray, val data: ByteArray, val cipherAlgorithm: CipherAlgorithm) {

    fun isType(otherType: EncryptedType.Types): Boolean {
        if (type == null) {
            return false
        }
        return type.type == otherType
    }

    fun isEmpty() = data.isEmpty() && iv.isEmpty()

    fun debugToString(): String {
        return "[type=${type}, iv=${iv.contentToString()}, data=${data.contentToString()}]"
    }

    fun toBase64String(): String {
        return toBase64String(this)
    }

    fun toBase64(): ByteArray {
        return toBase64(this)
    }

    override fun toString(): String {
        return toBase64String()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Encrypted

        if (type != other.type) return false
        if (!iv.contentEquals(other.iv)) return false
        if (!data.contentEquals(other.data)) return false
        if (cipherAlgorithm != other.cipherAlgorithm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + cipherAlgorithm.hashCode()
        return result
    }

    companion object {

        private val BASE64_PAIR_DELIMITOR = ':'
        private val BASE64_FLAGS = Base64.NO_WRAP or Base64.NO_PADDING

        fun empty(): Encrypted {
            return Encrypted(null, ByteArray(0), ByteArray(0), DEFAULT_CIPHER_ALGORITHM)
        }


        fun toBase64String(encrypted: Encrypted): String {
            val type = encrypted.type?.serialize() ?: ""
            val iv = Base64.encodeToString(encrypted.iv, BASE64_FLAGS)
            val data = Base64.encodeToString(encrypted.data, BASE64_FLAGS)
            return type + BASE64_PAIR_DELIMITOR + iv + BASE64_PAIR_DELIMITOR + data + BASE64_PAIR_DELIMITOR + encrypted.cipherAlgorithm.ordinal
        }

        fun fromEncryptedBase64StringWithCheck(string: String): Encrypted? {
            try {
                return fromBase64String(string)
            } catch (e: Exception) {
                Log.w(LOG_PREFIX + "ENC", "cannot read Encrypted", e)
                return null
            }
        }

        fun fromBase64String(string: String): Encrypted {
            val splitted = string.split(BASE64_PAIR_DELIMITOR.toString().toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = splitted[0]
            val iv = Base64.decode(splitted[1], BASE64_FLAGS)
            val data = Base64.decode(splitted[2], BASE64_FLAGS)
            var algo = DEFAULT_CIPHER_ALGORITHM
            if (splitted.size >= 4) {
                val algoIdx = splitted[3].toInt()
                algo = CipherAlgorithm.values()[algoIdx]
            }

            return Encrypted(EncryptedType.of(type), iv, data, algo)
        }

        fun toBase64(encrypted: Encrypted): ByteArray {
            return toBase64String(encrypted).toByteArray()
        }

        fun fromBase64(byteArray: ByteArray): Encrypted {
            return fromBase64String(String(byteArray))
        }

    }
}