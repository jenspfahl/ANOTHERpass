package de.jepfa.yapm.model.encrypted

import com.lambdapioneer.argon2kt.Argon2Mode
import de.jepfa.yapm.R

val DEFAULT_KDF = KeyDerivationFunction.BUILT_IN_PBKDF
val PREFERRED_KDF = KeyDerivationFunction.ARGON2_ID

enum class KeyDerivationFunction(
    val id: String,
    val uiLabel: Int,
    val description: Int,
    val argon2Mode: Argon2Mode?
) {

    BUILT_IN_PBKDF("", R.string.KDF_PBKDF, R.string.KDF_PBKDF_desc, null),
    ARGON2_ID("a", R.string.KDF_ARGON2_ID, R.string.KDF_ARGON2_ID_desc, Argon2Mode.ARGON2_ID),
    ARGON2_I("i", R.string.KDF_ARGON2_I, R.string.KDF_ARGON2_I_desc, Argon2Mode.ARGON2_I),
    ARGON2_D("d", R.string.KDF_ARGON2_D, R.string.KDF_ARGON2_D_desc, Argon2Mode.ARGON2_D),
    ;


    companion object {
        fun getById(id: String): KeyDerivationFunction {
            return entries.first { it.id == id }
        }

    }
}