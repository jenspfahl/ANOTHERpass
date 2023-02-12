package de.jepfa.yapm.service.secret

import android.util.Base64
import android.util.Log
import de.jepfa.yapm.service.PreferenceService
import java.nio.ByteBuffer
import kotlin.math.roundToInt

object PbkdfIterationService {

    const val MIN_PBKDF_ITERATIONS = 10_001
    const val LEGACY_PBKDF_ITERATIONS = 65_536
    const val DEFAULT_PBKDF_ITERATIONS = 100_001
    const val MAX_PBKDF_ITERATIONS = 1_000_001


    fun getStoredPbkdfIterations(): Int {
        val i = PreferenceService.getAsInt(PreferenceService.DATA_PBKDF_ITERATIONS, null)
        Log.d("ITERATIONS", "found iterations=$i")

        return if (i > 0) i else LEGACY_PBKDF_ITERATIONS
    }

    fun storePbkdfIterations(iterations: Int) {
        PreferenceService.putInt(PreferenceService.DATA_PBKDF_ITERATIONS, iterations, null)
    }

    fun mapPercentageToIterations(percentValue: Int): Int {
        val base = MAX_PBKDF_ITERATIONS - MIN_PBKDF_ITERATIONS
        return (base * percentValue.toDouble() / 100).roundToInt() + MIN_PBKDF_ITERATIONS
    }

    fun mapIterationsToPercentage(iterations: Int): Int {
        val base = MAX_PBKDF_ITERATIONS - MIN_PBKDF_ITERATIONS
        return (100 * (iterations.toDouble() - MIN_PBKDF_ITERATIONS) / base).roundToInt()
    }

    fun fromBase64String(base64: String): Int? {
        val bytes = Base64.decode(base64, Base64.NO_WRAP or Base64.NO_PADDING)

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

    fun toBase64String(iterations: Int): String {
        val bytes = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(iterations).array()
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING)
    }
    
}