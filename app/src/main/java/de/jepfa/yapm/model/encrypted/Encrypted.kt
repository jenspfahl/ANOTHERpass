package de.jepfa.yapm.model.encrypted

import android.util.Base64

data class Encrypted(val type: String = "", val iv: ByteArray, val data: ByteArray) {

    fun debugToString(): String {
        return "[type=${type}, iv=${iv.contentToString()}, data=${data.contentToString()}]"
    }

    fun toBase64String(): String {
        return toBase64String(this)
    }

    fun toBase64(): ByteArray {
        return toBase64(this)
    }

    companion object {
        const val TYPE_MASTER_PASSWD_TOKEN = "MPT"
        const val TYPE_ENC_MASTER_PASSWD = "EMP"
        const val TYPE_ENC_MASTER_KEY = "EMK"

        private val BAS64_PAIR_DELIMITOR = ':'
        private val BASE64_FLAGS = Base64.NO_WRAP or Base64.NO_PADDING

        fun toBase64String(encrypted: Encrypted): String {
            val iv = Base64.encodeToString(encrypted.iv, BASE64_FLAGS)
            val data = Base64.encodeToString(encrypted.data, BASE64_FLAGS)
            return encrypted.type + BAS64_PAIR_DELIMITOR + iv + BAS64_PAIR_DELIMITOR + data
        }

        fun fromBase64String(string: String): Encrypted {
            val splitted = string.split(BAS64_PAIR_DELIMITOR.toString().toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = splitted[0]
            val iv = Base64.decode(splitted[1], BASE64_FLAGS)
            val data = Base64.decode(splitted[2], BASE64_FLAGS)

            return Encrypted(type, iv, data)
        }

        fun toBase64(encrypted: Encrypted): ByteArray {
            val iv = Base64.encode(encrypted.iv, BASE64_FLAGS)
            val data = Base64.encode(encrypted.data, BASE64_FLAGS)
            return encrypted.type.toByteArray() + BAS64_PAIR_DELIMITOR.toByte() + iv + BAS64_PAIR_DELIMITOR.toByte() + data
        }

        fun fromBase64(byteArray: ByteArray): Encrypted {
            val firstDelimiterIndex = byteArray.indexOf(BAS64_PAIR_DELIMITOR.toByte())
            val lastDelimiterIndex = byteArray.lastIndexOf(BAS64_PAIR_DELIMITOR.toByte())
            val type = String(byteArray.copyOf(firstDelimiterIndex))
            val iv = Base64.decode(byteArray.copyOfRange(firstDelimiterIndex + 1, lastDelimiterIndex), BASE64_FLAGS)
            val data = Base64.decode(byteArray.copyOfRange(lastDelimiterIndex + 1, byteArray.size), BASE64_FLAGS)

            return Encrypted(type, iv, data)
        }

    }
}