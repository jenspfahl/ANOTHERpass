package de.jepfa.yapm.model.encrypted

import android.content.Context
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.KdfParameterService

data class KdfConfig(
    val kdf: KeyDerivationFunction,
    val iterations: Int,
    val memCostInMiB: Int?) {

    fun isArgon2() = kdf != KeyDerivationFunction.BUILT_IN_PBKDF

    fun persist(context: Context) {
        PreferenceService.putString(PreferenceService.DATA_USED_KDF_ID, kdf.id, context)
        if (isArgon2()) {
            PreferenceService.putInt(PreferenceService.DATA_ARGON2_ITERATIONS, iterations, context)
            PreferenceService.putInt(PreferenceService.DATA_ARGON2_MIB, memCostInMiB!!, context)
        }
        else {
            KdfParameterService.storePbkdfIterations(iterations)
        }
    }
}
