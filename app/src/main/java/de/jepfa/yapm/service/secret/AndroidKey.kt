package de.jepfa.yapm.service.secret

enum class AndroidKey(val alias: String, val boxed: Boolean, val requireUserAuth: Boolean = false) {

    ALIAS_KEY_TRANSPORT("YAPM/keyAlias:TRANSPORT", boxed = false),
    ALIAS_KEY_SALT("YAPM/keyAlias:SALT", boxed = true),
    ALIAS_KEY_MK("YAPM/keyAlias:MK", boxed = true),
    ALIAS_KEY_MP("YAPM/keyAlias:MP", boxed = true),
    ALIAS_KEY_MP_WITH_AUTH("YAPM/keyAlias:MP_WA", boxed = true, requireUserAuth = true),
    ALIAS_KEY_MP_TOKEN("YAPM/keyAlias:MPT", boxed = true)
}