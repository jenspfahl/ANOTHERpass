package de.jepfa.yapm.model.encrypted

import android.content.Context
import android.util.Base64
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.KdfParameterService
import de.jepfa.yapm.service.secret.KdfParameterService.MAX_ARGON_ITERATIONS
import de.jepfa.yapm.service.secret.KdfParameterService.MAX_ARGON_MIB
import de.jepfa.yapm.service.secret.KdfParameterService.MAX_PBKDF_ITERATIONS
import de.jepfa.yapm.service.secret.KdfParameterService.MIN_ARGON_ITERATIONS
import de.jepfa.yapm.service.secret.KdfParameterService.MIN_ARGON_MIB
import de.jepfa.yapm.service.secret.KdfParameterService.MIN_PBKDF_ITERATIONS
import de.jepfa.yapm.util.DebugInfo
import java.nio.ByteBuffer

private const val NEW_KDF_ENCODING_DELIMITER = '_'

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


    /**
     * Leading empty AA will be removed
     */
    fun toBase64String(): String {
        val memCostAsBase64Trimmed = if (memCostInMiB != null)
            fromInt(memCostInMiB)
        else
            ""
        return "${kdf.id}${fromInt(iterations)}$NEW_KDF_ENCODING_DELIMITER$memCostAsBase64Trimmed"
    }

    private fun fromInt(i: Int): String {
        val bytes = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(i).array()
        val asBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING)
        return asBase64.trimStart('A')
    }

    companion object {
        /**
         * Removed leading AA will be considered
         */
        fun fromBase64String(base64: String): KdfConfig? {
            var kdf = KeyDerivationFunction.BUILT_IN_PBKDF
            if (base64.contains(NEW_KDF_ENCODING_DELIMITER)) {
                val delimiterIndex = base64.indexOf(NEW_KDF_ENCODING_DELIMITER)
                kdf = KeyDerivationFunction.getById(base64.first().toString())
                if (kdf.isArgon2()) {
                    // Argon2
                    val iterationsBase64 = base64.substring(1, delimiterIndex)
                    val iterations = toInt(iterationsBase64, MIN_ARGON_ITERATIONS, MAX_ARGON_ITERATIONS) ?: return null
                    val memCostBase64 = base64.substring(delimiterIndex)
                    val memCost = toInt(memCostBase64, MIN_ARGON_MIB, MAX_ARGON_MIB) ?: return null

                    return KdfConfig(KeyDerivationFunction.BUILT_IN_PBKDF, iterations, memCost)
                }
                else {
                    // PBKDF in new format
                    val iterationsBase64 = base64.substring(1, delimiterIndex)
                    val iterations = toInt(iterationsBase64, MIN_PBKDF_ITERATIONS, MAX_PBKDF_ITERATIONS) ?: return null

                    return KdfConfig(KeyDerivationFunction.BUILT_IN_PBKDF, iterations, null)
                }
            }
            else {
                // read old format / PBKDF only
                val iterations = toInt(base64, MIN_PBKDF_ITERATIONS, MAX_PBKDF_ITERATIONS) ?: return null

                return KdfConfig(KeyDerivationFunction.BUILT_IN_PBKDF, iterations, null)
            }

        }

        private fun toInt(base64: String, min: Int, max: Int): Int? {

            try {
                val base64PaddedTo32 = base64.padStart(6, 'A') // length of 6 is 32 bits
                val bytes = Base64.decode(base64PaddedTo32, Base64.NO_WRAP or Base64.NO_PADDING)

                if (bytes.size > Int.SIZE_BYTES) {
                    return null
                }
                else {
                    val i = ByteBuffer.wrap(bytes).int
                    if (i > max || i < min) {
                        return null
                    }
                    else {
                        return i
                    }
                }
            } catch (e: Exception) {
                DebugInfo.logException("KDF", "cannot decode an int from KDF", e)
                return null
            }

        }
    }
}
