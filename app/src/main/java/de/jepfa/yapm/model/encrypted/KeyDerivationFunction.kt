package de.jepfa.yapm.model.encrypted

import android.os.Build
import de.jepfa.yapm.R

val DEFAULT_KDF = KeyDerivationFunction.BUILD_IN_PBKDF
val PREFERRED_KDF = KeyDerivationFunction.ARGON2_ID

enum class KeyDerivationFunction(
    val id: String,
    val uiLabel: Int,
    val description: Int) {
    BUILD_IN_PBKDF("", R.string.KDF_PBKDF, R.string.KDF_PBKDF_desc),
    ARGON2_ID("a", R.string.KDF_ARGON2_ID, R.string.KDF_ARGON2_ID_desc),
    ARGON2_I("i", R.string.KDF_ARGON2_I, R.string.KDF_ARGON2_I_desc),
    ARGON2_D("d", R.string.KDF_ARGON2_D, R.string.KDF_ARGON2_D_desc),
    ;


    companion object {
        fun getById(id: String): KeyDerivationFunction {
            return entries.first { it.id == id }
        }

    }
}