package de.jepfa.yapm.util

import android.content.Intent
import android.os.Bundle
import de.jepfa.yapm.model.encrypted.Encrypted
import java.math.BigDecimal
import java.math.RoundingMode

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

fun Intent.getEncryptedExtra(key: String, default: Encrypted): Encrypted {
    val value = this.getStringExtra(key) ?: return default
    return Encrypted.fromBase64String(value)
}

fun Intent.getEncryptedExtra(key: String): Encrypted? {
    val value = this.getStringExtra(key) ?: return null
    return Encrypted.fromBase64String(value)
}

fun Intent.putEncryptedExtra(key: String, encrypted: Encrypted) {
    this.putExtra(key, encrypted.toBase64String())
}

fun Intent.getIntExtra(key: String): Int? {
    val value = this.getIntExtra(key, -1)
    return if (value != -1) value else null
}

fun StringBuilder.addFormattedLine(label: String, data: Any?) {
    append(label)
        .append(": ")
        .append(data)
        .append(System.lineSeparator())
}

fun Double.toReadableFormat(scale: Int): String {
  //  return Constants.DF.format(this)
    return BigDecimal(this).setScale(scale, RoundingMode.HALF_EVEN).toString()
}


fun Double.secondsToYear(): Double {
    return this / 60 / 60 / 24 / 365
}

