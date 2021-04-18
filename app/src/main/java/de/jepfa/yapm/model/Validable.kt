package de.jepfa.yapm.model

interface Validable {

    fun isValid(): Boolean

    companion object {
        val FAILED_BYTE_ARRAY = "<<LOCKED>>".toByteArray()
    }
}