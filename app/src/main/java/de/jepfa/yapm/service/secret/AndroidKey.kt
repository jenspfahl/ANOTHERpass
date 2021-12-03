package de.jepfa.yapm.service.secret

enum class AndroidKey(val alias: String, val boxed: Boolean = true, val requireUserAuth: Boolean = false) {

    ALIAS_KEY_TRANSPORT("YAPM/keyAlias:TRANS"),
    ALIAS_KEY_SALT("YAPM/keyAlias:SALT"),
    ALIAS_KEY_MK("YAPM/keyAlias:MK"),
    ALIAS_KEY_MP("YAPM/keyAlias:MP", requireUserAuth = false),
    ALIAS_KEY_MP_WITH_AUTH("YAPM/keyAlias:MP_WA", requireUserAuth = true),
    ALIAS_KEY_MP_TOKEN("YAPM/keyAlias:MPT")
}