package de.jepfa.yapm.model.secret

import android.text.Editable
import android.util.Base64
import de.jepfa.obfusser.util.encrypt.Loop

class Password: Secret, CharSequence {

    enum class PresentationMode {
        IN_WORDS, IN_WORDS_MULTI_LINE, RAW;

        fun prev(): PresentationMode {
            var prevIdx = ordinal - 1
            if (prevIdx < 0) prevIdx = values().size - 1
            return values()[prevIdx]
        }

        fun next(): PresentationMode {
            val nextIdx = (ordinal + 1) % values().size
            return values()[nextIdx]
        }

        fun isMultiLine(): Boolean = this == IN_WORDS_MULTI_LINE

        companion object {
            val DEFAULT = IN_WORDS
            fun createFromFlags(multiLine: Boolean, formatted: Boolean) =
                if (formatted)
                    if (multiLine) IN_WORDS_MULTI_LINE else DEFAULT
                else RAW
        }
    }

    constructor(passwd: String) : super(passwd.toByteArray())
    constructor(passwd: ByteArray) : super(passwd)
    constructor(passwd: CharArray) : super(passwd.map { it.toByte() }.toByteArray())

    fun add(other: Char) {
        val buffer = data + other.toByte()
        clear()
        data = buffer
    }

    fun replace(index: Int, other: Char) {
        data[index] = other.toByte()
    }

    fun obfuscate(key: Key) {
        Loop.loopPassword(this, key, forwards = true)
    }

    fun deobfuscate(key: Key) {
        Loop.loopPassword(this, key, forwards = false)
    }

    fun toStringRepresentation(): String {
        return toStringRepresentation(PresentationMode.DEFAULT, maskPassword = false)
    }

    fun toStringRepresentation(presentationMode: PresentationMode, maskPassword: Boolean): String {
        var presentation = ""
        val multiLine = presentationMode.isMultiLine()
        val raw = presentationMode == PresentationMode.RAW
        val presentationLength = if (maskPassword) 16 else length
        for (i in 0 until presentationLength) {
            if (!raw && i != 0 && i % 4 == 0) {
                if (i % 8 == 0) {
                    if (multiLine) {
                        presentation += System.lineSeparator()
                    }
                    else {
                        presentation += "  "
                    }
                }
                else {
                    presentation += " "
                }
            }

            presentation += if (maskPassword) '*' else get(i)
        }

        return presentation
    }

    override val length: Int
        get() = data.size

    override fun get(index: Int): Char {
        return toCharArray()[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return Password(data.copyOfRange(startIndex, endIndex))
    }

    fun toBase64String(): String {
        return Base64.encodeToString(data, BASE64_FLAGS)
    }

    override fun toString() : String {
        return String(toCharArray())
    }

    companion object {
        private val BASE64_FLAGS = Base64.NO_WRAP or Base64.NO_PADDING

        fun fromEditable(editable: Editable): Password {

            val l = editable.length
            val chararray = CharArray(l)
            editable.getChars(0, l, chararray, 0)
            return Password(chararray)
        }

        fun empty(): Password {
            return Password("")
        }


        fun fromBase64String(string: String): Password {
            val bytes = Base64.decode(string, BASE64_FLAGS)
            return Password(bytes)
        }
    }
}