package de.jepfa.yapm.model.secret

import android.text.Editable
import de.jepfa.obfusser.util.encrypt.Loop

class Password: Secret, CharSequence {
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

    fun toStringRepresentation(multiLine: Boolean): String {
        return toStringRepresentation(multiLine, maskPassword = false)
    }

    fun toStringRepresentation(multiLine: Boolean, maskPassword: Boolean): String {
        var presentation = ""
        var presentationLength = if (maskPassword) 16 else length
        for (i in 0 until presentationLength) {
            if (i != 0 && i % 4 == 0) {
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

    override fun toString() : String {
        return String(toCharArray())
    }

    companion object {
        fun fromEditable(editable: Editable): Password {

            val l = editable.length
            val chararray = CharArray(l)
            editable.getChars(0, l, chararray, 0)
            return Password(chararray)
        }

        fun empty(): Password {
            return Password("")
        }
    }
}