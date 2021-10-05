package de.jepfa.yapm.model

interface Validable {

    fun isValid(): Boolean

    companion object {
        const val FAILED_STRING = "<<LOCKED>>"
        val FAILED_BYTE_ARRAY = FAILED_STRING.toByteArray()
    }
}