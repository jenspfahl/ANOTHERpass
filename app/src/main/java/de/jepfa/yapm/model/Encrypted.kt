package de.jepfa.yapm.model

import android.util.Base64

data class Encrypted(val iv: ByteArray, val data: ByteArray) {
    fun debugToString(): String {
        return "[iv=${iv.contentToString()}, data=${data.contentToString()}]"
    }

    fun toBase64String(): String {
        return Encrypted.toBase64String(this)
    }

    companion object {
        private val BAS64_PAIR_DELIMITOR = ":"

        fun toBase64String(encrypted: Encrypted): String {
            val iv = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP or Base64.NO_PADDING)
            val data = Base64.encodeToString(encrypted.data, Base64.NO_WRAP or Base64.NO_PADDING)
            return iv + BAS64_PAIR_DELIMITOR + data
        }

        fun fromBase64String(string: String): Encrypted {
            val splitted = string.split(BAS64_PAIR_DELIMITOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val iv = Base64.decode(splitted[0], Base64.NO_WRAP or Base64.NO_PADDING)
            val data = Base64.decode(splitted[1], Base64.NO_WRAP or Base64.NO_PADDING)

            return Encrypted(iv, data)
        }
    }
}