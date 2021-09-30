package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import java.security.SecureRandom

abstract class GeneratorBase<T : GeneratorSpec> {

    companion object {
        val BRUTEFORCE_ATTEMPTS_PENTIUM = 100_000 // per second
        val BRUTEFORCE_ATTEMPTS_SUPERCOMP = 1_000_000_000 // per second
    }

    protected var random = SecureRandom()

    abstract fun generate(spec: T): Password

    abstract fun calcCombinationCount(spec: T): Double

    fun calcBruteForceWaitingSeconds(combinations: Double, tryPerSec: Int): Double {
        return combinations / tryPerSec
    }

    fun maybeResetPRNG() {
        if (random.nextInt(5) <= 0) {
            random = SecureRandom()
        }
    }
}