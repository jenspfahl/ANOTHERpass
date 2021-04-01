package de.jepfa.yapm

import de.jepfa.yapm.service.secretgenerator.*
import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.BRUTEFORCE_ATTEMPTS_PENTIUM
import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.BRUTEFORCE_ATTEMPTS_SUPERCOMP
import org.junit.Test

class PassphraseGeneratorTest {

    val passphraseGenerator = PassphraseGenerator()

    @Test
    fun generateAndClearPassphrase() {
        val spec = PassphraseGeneratorSpec(PassphraseStrength.STRONG,
                wordBeginningUpperCase = true, addDigit = false, addSpecialChar = true)
        for (i in 0..100) {
            val passphrase = passphraseGenerator.generate(spec)
            println("passphrase=${passphrase.toStringRepresentation(false)}")
            passphrase.clear()
        }
        val calcCombinationCount = passphraseGenerator.calcCombinationCount(spec)
        println("comp=$calcCombinationCount")

        val calcBruteForceWaitingPentiumSeconds = passphraseGenerator.calcBruteForceWaitingSeconds(calcCombinationCount, BRUTEFORCE_ATTEMPTS_PENTIUM)
        val calcBruteForceWaitingupercompSeconds = passphraseGenerator.calcBruteForceWaitingSeconds(calcBruteForceWaitingPentiumSeconds, BRUTEFORCE_ATTEMPTS_SUPERCOMP)

        println("brute force years for Pentum =${calcBruteForceWaitingPentiumSeconds/60/60/24/365}")
        println("brute force years for Supercomp =${calcBruteForceWaitingupercompSeconds/60/60/24/365}")

    }
}