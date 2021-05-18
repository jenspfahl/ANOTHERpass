package de.jepfa.yapm.ui.nfc

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log
import de.jepfa.yapm.model.secret.Key
import java.io.IOException
import java.util.*

/**
 * Inspired by https://proandroiddev.com/working-with-nfc-tags-on-android-c1e5af47a3db
 */
class WritableTag @Throws(FormatException::class) constructor(val tag: Tag, val data: String?) {
    private val NDEF = Ndef::class.java.canonicalName
    private val NDEF_FORMATABLE = NdefFormatable::class.java.canonicalName

    private val ndef: Ndef?
    private val ndefFormatable: NdefFormatable?

    val tagId: String?
        get() {
            if (ndef != null) {
                return bytesToHexString(ndef.tag.id)
            } else if (ndefFormatable != null) {
                return bytesToHexString(ndefFormatable.tag.id)
            }
            return null
        }

    init {
        val technologies = tag.techList
        val tagTechs = Arrays.asList(*technologies)
        if (tagTechs.contains(NDEF)) {
            Log.i("WritableTag", "contains ndef")
            ndef = Ndef.get(tag)
            ndefFormatable = null
        } else if (tagTechs.contains(NDEF_FORMATABLE)) {
            Log.i("WritableTag", "contains ndef_formatable")
            ndefFormatable = NdefFormatable.get(tag)
            ndef = null
        } else {
            throw FormatException("Tag doesn't support ndef")
        }
    }

    fun getUID(): Key {
        return Key(tag.id)
    }

    fun getMaxSize(): Int? {
        return ndef?.maxSize ?: null
    }

    fun getSize(): Int? {
        return data?.length
    }

    @Throws(IOException::class, FormatException::class)
    fun writeData(message: NdefMessage): Boolean {
        if (ndef != null) {
            ndef.connect()
            if (ndef.isConnected) {
                ndef.writeNdefMessage(message)
                return true
            }
        } else if (ndefFormatable != null) {
            ndefFormatable.connect()
            if (ndefFormatable.isConnected) {
                ndefFormatable.format(message)
                return true
            }
        }
        return false
    }

    @Throws(IOException::class)
    fun close() {
        ndef?.close() ?: ndefFormatable?.close()
    }

    companion object {
        fun bytesToHexString(src: ByteArray): String? {
            if (src.isEmpty()) {
                return null
            }
            val sb = StringBuilder()
            for (b in src) {
                sb.append(String.format("%02X", b))
            }
            return sb.toString()
        }
    }
}