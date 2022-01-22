package de.jepfa.yapm.util

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.util.Constants.DF
import de.jepfa.yapm.util.Constants.EDF
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*

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
        .append(data ?: "")
        .append(System.lineSeparator())
}

fun Double.toReadableFormat(): String {
    return DF.format(this)
}

fun Double.toExponentFormat(): String {
    return EDF.format(this)

}

fun UUID.toBase64String(): String {
    val byteArray = ByteBuffer.allocate(16)
        .putLong(this.mostSignificantBits)
        .putLong(this.leastSignificantBits)
        .array()
    return Base64.encodeToString(byteArray, Base64.NO_PADDING or Base64.NO_WRAP)
}

fun String.toUUIDFromBase64String(): UUID {
    try {
        val dec = Base64.decode(this, Base64.NO_PADDING or Base64.NO_WRAP)

        if (dec.size != 16) {
            throw IllegalArgumentException("UUIDs can only be created from 128bit")
        }
        val buf = ByteBuffer.allocate(16).put(dec)
        return UUID(buf.getLong(0), buf.getLong(8))
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid Base64 sequence", e)
    }
}


fun Double.secondsToYear(): Double {
    return this / 60 / 60 / 24 / 365
}

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            observer.onChanged(t)
            removeObserver(this)
        }
    })
}

