package de.jepfa.yapm

import de.jepfa.yapm.service.secretgenerator.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.PassphraseGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
import org.junit.Test

class PassphraseGeneratorTest {

    val passphraseGenerator = PassphraseGenerator()

    @Test
    fun generateAndClearPassphrase() {
        val spec = PassphraseGeneratorSpec(PasswordStrength.STRONG,
                wordBeginningUpperCase = true, addDigit = false, addSpecialChar = true)
        for (i in 0..100) {
            val passphrase = passphraseGenerator.generatePassphrase(spec)
            println("passphrase=${passphrase.debugToString()}")
            passphrase.clear()
        }
        println("comp=${passphraseGenerator.calcCombinationCount(spec)}")

        val calcBruteForceWaitingPentiumSeconds = passphraseGenerator.calcBruteForceWaitingSeconds(spec, passphraseGenerator.BRUTEFORCE_ATTEMPTS_PENTIUM)
        val calcBruteForceWaitingupercompSeconds = passphraseGenerator.calcBruteForceWaitingSeconds(spec, passphraseGenerator.BRUTEFORCE_ATTEMPTS_SUPERCOMP)

        println("brute force years for Pentum =${calcBruteForceWaitingPentiumSeconds/60/60/24/365}")
        println("brute force years for Supercomp =${calcBruteForceWaitingupercompSeconds/60/60/24/365}")

    }
}