package de.jepfa.yapm.service.secretgenerator

import android.content.Context
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import java.security.SecureRandom
import kotlin.math.log2

abstract class GeneratorBase<T : GeneratorSpec>(val context: Context?) {

    companion object {
        val BRUTEFORCE_ATTEMPTS_PENTIUM = 100_000 // per second
        val BRUTEFORCE_ATTEMPTS_SUPERCOMP = 1_000_000_000 // per second
    }

    abstract fun generate(spec: T): Password

    abstract fun calcCombinationCount(spec: T): Double

    fun calcEntropy(combinations: Double): Double {
        return log2(combinations)
    }

    fun calcBruteForceWaitingSeconds(combinations: Double, tryPerSec: Int): Double {
        return combinations / tryPerSec
    }

}