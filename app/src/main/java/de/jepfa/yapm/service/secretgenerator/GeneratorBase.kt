package de.jepfa.yapm.service.secretgenerator

import android.content.Context
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.SecretService
import java.security.SecureRandom
import kotlin.math.log2

abstract class GeneratorBase<T : GeneratorSpec>(
    val context: Context?,
    private val secureRandom: SecureRandom? = null
) {

    companion object {
        /*
         Attention: Changing any of the default sets here can break de-obfuscation!!!!!! See Loop.kt what is actually used.
         */
        const val DEFAULT_ALPHA_CHARS_LOWER_CASE = "abcdefghijklmnopqrstuvwxyz"
        val DEFAULT_ALPHA_CHARS_UPPER_CASE = DEFAULT_ALPHA_CHARS_LOWER_CASE.uppercase()
        const val DEFAULT_DIGITS = "0123456789"
        const val DEFAULT_SPECIAL_CHARS = "!?-,.:/$&@#_;+*"
        const val DEFAULT_OBFUSCATIONABLE_SPECIAL_CHARS  = "!?-,.:/$%&@#"

        const val EXTENDED_SPECIAL_CHARS = "()[]{}<>\"'=%\\~|"

        const val BRUTEFORCE_ATTEMPTS_PENTIUM = 100_000L // per second
        const val BRUTEFORCE_ATTEMPTS_SUPERCOMP = 1_000_000_000_000L // 1  billion per second
    }

    abstract fun generate(spec: T): Password

    abstract fun calcCombinationCount(spec: T): Double

    fun calcEntropy(combinations: Double): Double {
        return log2(combinations)
    }

    fun calcBruteForceWaitingSeconds(combinations: Double, tryPerSec: Long): Double {
        return combinations / tryPerSec
    }

    internal fun random(material: String): Char {
        val rand = secureRandom ?: SecretService.getSecureRandom(context)
        val index = rand.nextInt(material.length)

        return material[index]
    }

}