package de.jepfa.yapm.model.encrypted

data class EncryptedType(val type: Types, val payload: String? = null) {

    enum class Types(val code: String, val hiddenPayload: Boolean) {
        MASTER_PASSWD_TOKEN("MPT", hiddenPayload = false),
        ENC_MASTER_PASSWD("EMP", hiddenPayload = false),
        ENC_MASTER_KEY("EMK", hiddenPayload = true),
        ENC_SALT("SLT", hiddenPayload = false);

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

    fun serialize(): String {
        return if (payload != null) {
            type.code + ADD_ON_SEPARATOR + payload
        } else {
            type.code
        }
    }
    override fun toString(): String {
        return if (!type.hiddenPayload && payload != null) {
            type.code + ADD_ON_SEPARATOR + payload
        } else {
            type.code
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