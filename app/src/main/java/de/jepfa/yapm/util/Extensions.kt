package de.jepfa.yapm.util

import android.os.Bundle
import de.jepfa.yapm.model.Encrypted

fun Bundle?.getEncrypted(key: String): Encrypted? {

    val value = this?.getString(key)

    if (value != null) {
        return Encrypted.fromBase64String(value)
    }
    else {
        return null
    }
}
