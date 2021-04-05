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

fun Intent.getEncryptedSetExtra(key: String): Set<Encrypted> {
    val value = this.getStringArrayListExtra(key)

    if (value != null) {
        return value.map { Encrypted.fromBase64String(it) }.toSet()
    }
    else {
        return Collections.emptySet()
    }
}

fun Intent.putEncryptedSetExtra(key: String, encryptedSet: Set<Encrypted>) {
    val encryptedBase64Set = encryptedSet.map { it.toBase64String() }
    this.putStringArrayListExtra(key, ArrayList(encryptedBase64Set))
}

