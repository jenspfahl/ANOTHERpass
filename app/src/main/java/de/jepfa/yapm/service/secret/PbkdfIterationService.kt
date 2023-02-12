package de.jepfa.yapm.service.secret

import android.util.Base64
import android.util.Log
import de.jepfa.yapm.service.PreferenceService
import java.nio.ByteBuffer
import kotlin.math.roundToInt

object PbkdfIterationService {

    const val MIN_PBKDF_ITERATIONS = 10_000
    const val LEGACY_PBKDF_ITERATIONS = 65_536
    const val DEFAULT_PBKDF_ITERATIONS = 100_000
    const val MAX_PBKDF_ITERATIONS = 2_000_000 + MIN_PBKDF_ITERATIONS


    fun getStoredPbkdfIterations(): Int {
        val i = PreferenceService.getAsInt(PreferenceService.DATA_PBKDF_ITERATIONS, null)
        Log.d("ITERATIONS", "found iterations=$i")

        return if (i > 0) i else LEGACY_PBKDF_ITERATIONS
    }

    fun storePbkdfIterations(iterations: Int) {
        PreferenceService.putInt(PreferenceService.DATA_PBKDF_ITERATIONS, iterations, null)
    }

    fun mapPercentageToIterations(percentValue: Float): Int {
        val base = MAX_PBKDF_ITERATIONS - MIN_PBKDF_ITERATIONS
        return (base * percentValue).roundToInt() + MIN_PBKDF_ITERATIONS
    }

    fun mapIterationsToPercentage(iterations: Int): Float {
        val base = MAX_PBKDF_ITERATIONS - MIN_PBKDF_ITERATIONS
        return (iterations - MIN_PBKDF_ITERATIONS).toFloat() / base
    }

    /**
     * Removed leading AA will be considered
     */
    fun fromBase64String(base64: String): Int? {
        val base64Padded = base64.padStart(6, 'A')
        val bytes = Base64.decode(base64Padded, Base64.NO_WRAP or Base64.NO_PADDING)

        if (bytes.size > 4) {
            return null
        }

        try {
            val iterations = ByteBuffer.wrap(bytes).int

            if (iterations > MAX_PBKDF_ITERATIONS || iterations < MIN_PBKDF_ITERATIONS) {
                return null
            }
            return iterations
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Leading empty AA will be removed
     */
    fun toBase64String(iterations: Int): String {
        val bytes = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(iterations).array()
        val s = Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING)
        return s.trimStart('A')
    }

}