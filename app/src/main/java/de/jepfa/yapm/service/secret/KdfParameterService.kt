package de.jepfa.yapm.service.secret

import android.util.Base64
import android.util.Log
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import java.nio.ByteBuffer
import kotlin.math.roundToInt

object KdfParameterService {

    const val MIN_PBKDF_ITERATIONS = 10_000
    const val LEGACY_PBKDF_ITERATIONS = 65_536
    const val MPT_PBKDF_ITERATIONS = 65_536
    const val DEFAULT_PBKDF_ITERATIONS = 100_000
    const val MAX_PBKDF_ITERATIONS = 2_000_000 + MIN_PBKDF_ITERATIONS

    const val MIN_ARGON_ITERATIONS = 1
    const val DEFAULT_ARGON_ITERATIONS = 5
    const val MAX_ARGON_ITERATIONS = 10

    const val MIN_ARGON_MIB = 12
    const val DEFAULT_ARGON_MIB = 64
    const val MAX_ARGON_MIB = 256 + MIN_ARGON_MIB


    fun getStoredPbkdfIterations(): Int {
        val i = PreferenceService.getAsInt(PreferenceService.DATA_PBKDF_ITERATIONS, null)
        Log.d(LOG_PREFIX + "ITERATIONS", "found iterations=$i")

        return if (i > 0) i else LEGACY_PBKDF_ITERATIONS
    }

    fun storePbkdfIterations(iterations: Int) {
        PreferenceService.putInt(PreferenceService.DATA_PBKDF_ITERATIONS, iterations, null)
    }

    fun mapPercentageToPbkdfIterations(percentValue: Float): Int {
        val base = MAX_PBKDF_ITERATIONS - MIN_PBKDF_ITERATIONS
        return (base * percentValue).roundToInt() + MIN_PBKDF_ITERATIONS
    }

    fun mapPbkdfIterationsToPercentage(iterations: Int): Float {
        val base = MAX_PBKDF_ITERATIONS - MIN_PBKDF_ITERATIONS
        val p = (iterations - MIN_PBKDF_ITERATIONS).toFloat() / base
        return (p * 100).roundToInt() / 100.0F
    }


}