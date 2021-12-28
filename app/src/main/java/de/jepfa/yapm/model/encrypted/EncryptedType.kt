package de.jepfa.yapm.model.encrypted

data class EncryptedType(val type: Types, val payload: String? = null) {

    enum class Types(val code: String) {
        MASTER_PASSWD_TOKEN("MPT"),
        ENC_MASTER_PASSWD("EMP"),
        ENC_MASTER_KEY("EMK"),
        ENC_SALT("SLT");

        companion object {
            fun of(type: String): Types? {
                val mapped = values().filter { it.code == type }.toList()
                if (mapped.isEmpty()) {
                    return null
                }
                else {
                    return mapped.first()
                }
            }
        }
    }

    override fun toString(): String {
        if (payload != null) {
           return type.code + ADD_ON_SEPARATOR + payload
        }
        else {
            return type.code
        }
    }

    companion object {
        const val ADD_ON_SEPARATOR = '#'

        fun of(typeAndPayload: String): EncryptedType? {
            if (typeAndPayload.isEmpty()) {
                return null
            }
            val splitted = typeAndPayload.split(ADD_ON_SEPARATOR)
            val typeValue = splitted[0]
            val payload = if (splitted.size > 1) splitted[1] else null
            val type = Types.of(typeValue)
            if (type == null) {
                return null
            }
            return EncryptedType(type, payload)
        }
    }
}