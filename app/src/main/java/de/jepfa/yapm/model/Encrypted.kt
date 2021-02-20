package de.jepfa.yapm.model

import android.util.Base64

data class Encrypted(val iv: ByteArray, val data: ByteArray) {
    fun debugToString(): String {
        return "[iv=${iv.contentToString()}, data=${data.contentToString()}]"
    }

    fun toBase64String(): String {
        return Encrypted.toBase64String(this)
    }

    fun toBase64(): ByteArray {
        return Encrypted.toBase64(this)
    }

    companion object {
        private val BAS64_PAIR_DELIMITOR = ':'
        private val BASE64_FLAGS = Base64.NO_WRAP or Base64.NO_PADDING

        fun toBase64String(encrypted: Encrypted): String {
            val iv = Base64.encodeToString(encrypted.iv, BASE64_FLAGS)
            val data = Base64.encodeToString(encrypted.data, BASE64_FLAGS)
            return iv + BAS64_PAIR_DELIMITOR + data
        }

        fun fromBase64String(string: String): Encrypted {
            val splitted = string.split(BAS64_PAIR_DELIMITOR.toString().toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val iv = Base64.decode(splitted[0], BASE64_FLAGS)
            val data = Base64.decode(splitted[1], BASE64_FLAGS)

            return Encrypted(iv, data)
        }

        fun toBase64(encrypted: Encrypted): ByteArray {
            val iv = Base64.encode(encrypted.iv, BASE64_FLAGS)
            val data = Base64.encode(encrypted.data, BASE64_FLAGS)
            return iv + BAS64_PAIR_DELIMITOR.toByte() + data
        }

        fun fromBase64(byteArray: ByteArray): Encrypted {
            val delimiterIndex = byteArray.indexOf(BAS64_PAIR_DELIMITOR.toByte())
            val iv = Base64.decode(byteArray.copyOf(delimiterIndex), BASE64_FLAGS)
            val data = Base64.decode(byteArray.copyOfRange(delimiterIndex + 1, byteArray.size), BASE64_FLAGS)

            return Encrypted(iv, data)
        }
    }
}