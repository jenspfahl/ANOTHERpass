package de.jepfa.yapm.model.secret

import android.util.Base64
import de.jepfa.yapm.model.encrypted.Encrypted

/**
 * Represents a key which is secret to others.
 */
class Key : Secret {

    constructor(data: ByteArray): super(data)

    fun debugToString(): String {
        return data.contentToString()
    }

    fun toBase64String(): String {
        return Base64.encodeToString(this.data, 0)
    }

    fun toShortenedFingerprint(): String {
        val f = toBase64String()
        .replace(Regex("[^a-z]", RegexOption.IGNORE_CASE), "")
        .substring(0, 7)
        .uppercase()

        return f.substring(0, 2) + "-" + f.substring(2, 5) + "-" + f.substring(5, 7)
    }

    companion object {
        fun empty(): Key {
            return Key(ByteArray(0))
        }
    }
}