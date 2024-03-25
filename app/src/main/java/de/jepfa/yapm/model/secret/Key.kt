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
        .substring(0, 6)
        .lowercase()

        return f.substring(0, 3) + "-" + f.substring(3, 6)
    }
}