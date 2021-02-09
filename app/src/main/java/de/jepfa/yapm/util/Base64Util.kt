package de.jepfa.yapm.util

import android.util.Base64
import de.jepfa.yapm.model.Encrypted

object Base64Util {

    private val BAS64_PAIR_DELIMITOR = ":"

    fun encryptedToBase64String(encrypted: Encrypted): String {
        val iv = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP or Base64.NO_PADDING)
        val data = Base64.encodeToString(encrypted.data, Base64.NO_WRAP or Base64.NO_PADDING)
        return iv + BAS64_PAIR_DELIMITOR + data
    }

    fun base64StringToEncrypted(string: String): Encrypted {
        val splitted = string.split(BAS64_PAIR_DELIMITOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val iv = Base64.decode(splitted[0], Base64.NO_WRAP or Base64.NO_PADDING)
        val data = Base64.decode(splitted[1], Base64.NO_WRAP or Base64.NO_PADDING)

        return Encrypted(iv, data)
    }
}
