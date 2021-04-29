package de.jepfa.yapm.util

import android.content.Intent
import android.os.Bundle
import de.jepfa.yapm.model.Encrypted
import java.util.*
import kotlin.collections.ArrayList

fun Bundle?.getEncrypted(key: String): Encrypted? {

    val value = this?.getString(key)

    if (value != null) {
        return Encrypted.fromBase64String(value)
    }
    else {
        return null
    }
}

fun Bundle.putEncrypted(key: String, encrypted: Encrypted) {
    this.putString(key, encrypted.toBase64String())
}

fun Intent.getEncryptedExtra(key: String): Encrypted {

    val value = this.getStringExtra(key)
    return Encrypted.fromBase64String(value)
}

fun Intent.putEncryptedExtra(key: String, encrypted: Encrypted) {
    this.putExtra(key, encrypted.toBase64String())
}

fun Intent.getIntExtra(key: String): Int? {
    val value = this.getIntExtra(key, -1)
    return if (value != -1) value else null
}


