package de.jepfa.yapm.model.secret

import android.text.Editable
import android.util.Base64
import de.jepfa.obfusser.util.encrypt.Loop
import de.jepfa.yapm.model.Clearable
import java.nio.charset.Charset

/**
 * Represents a real password.
 *
 * Passwords are internally stored as ByteArray, not as String. This is due the VM may cache
 * all Strings internally and would them make visible in heap dumps. To at least reduce that risk
 * Strings are only created by the UI framework when displaying it (this is not in our hand unfortunately).
 * Furthermore Password instances should be cleared (#clear) if not anymore needed.
 *
 * To convert it to a readable CharSequence the ByteArray has first to be converted
 * to a CharArray. This happens without Charset encoding, which means, all non-ASCII chars
 * may be displayed wrong. To get a CharArray with system default Charset (e.g. UTF8), use #toCharArrayDecoded.
 */
class Password: Secret, CharSequence {

    enum class FormattingStyle {
        IN_WORDS, IN_WORDS_MULTI_LINE, RAW;

        fun prev(): FormattingStyle {
            var prevIdx = ordinal - 1
            if (prevIdx < 0) prevIdx = values().size - 1
            return values()[prevIdx]
        }

        fun next(): FormattingStyle {
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

    /**
     * Represents a formatted real password to increase readability.
     */
    class FormattedPassword(): CharSequence, Clearable {
        private val charList = ArrayList<Char>(32)

        private constructor(charList: MutableList<Char>): this() {
            charList.addAll(charList)
        }

        override val length: Int
            get() = charList.size

        override fun get(index: Int): Char {
            return charList[index]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return FormattedPassword(charList.subList(startIndex, endIndex))
        }


        operator fun plus(string: String): FormattedPassword {
            charList.addAll(string.toCharArray().asList())
            return this
        }

        operator fun plus(char: Char): FormattedPassword {
            charList.add(char)
            return this
        }

        override fun clear() {
            charList.clear()
        }

        override fun toString(): String {
            return String(charList.toCharArray())
        }

        companion object {
            fun create(
                formattingStyle: FormattingStyle,
                maskPassword: Boolean,
                decoded: CharArray
            ): FormattedPassword {
                val multiLine = formattingStyle.isMultiLine()
                val raw = formattingStyle == FormattingStyle.RAW
                val formattedPasswordLength = if (maskPassword) 16 else decoded.size
                val formattedPassword = FormattedPassword()
                for (i in 0 until formattedPasswordLength) {
                    if (!raw && i != 0 && i % 4 == 0) {
                        if (i % 8 == 0) {
                            if (multiLine) {
                                formattedPassword + System.lineSeparator()
                            } else {
                                formattedPassword + "  "
                            }
                        } else {
                            formattedPassword + " "
                        }
                    }

                    formattedPassword + (if (maskPassword) '*' else decoded[i])
                }

                return formattedPassword
            }
        }
    }

    constructor(passwd: String) : this(passwd.toByteArray())
    constructor(passwd: CharArray) : this(passwd.map { it.toByte() }.toByteArray())
    constructor(passwd: ByteArray) : super(passwd)

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

    fun toFormattedPassword(): FormattedPassword {
        return toFormattedPassword(FormattingStyle.DEFAULT, maskPassword = false)
    }

    fun toFormattedPassword(formattingStyle: FormattingStyle, maskPassword: Boolean): FormattedPassword {
        val decoded = toCharArrayDecoded()
        return FormattedPassword.create(formattingStyle, maskPassword, decoded)
    }

    fun toRawFormattedPassword() =
        toFormattedPassword(FormattingStyle.RAW, maskPassword = false)
    /**
     * Returns the encoded length of this password.
     * Use #toCharArrayDecoded to work with non-ASCII passwords
     */
    override val length: Int
        get() = data.size

    /**
     * Returns the char at position index of the encoded password.
     * Use #toCharArrayDecoded to work with non-ASCII passwords
     */
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
        return toRawFormattedPassword().toString()
    }

    fun toCharArrayDecoded() = toCharArray(Charset.defaultCharset())

    companion object {
        private const val BASE64_FLAGS = Base64.NO_WRAP or Base64.NO_PADDING

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