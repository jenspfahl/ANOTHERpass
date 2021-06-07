package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.BRUTEFORCE_ATTEMPTS_PENTIUM
import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.BRUTEFORCE_ATTEMPTS_SUPERCOMP
import de.jepfa.yapm.util.secondsToYear
import org.junit.Test

class PassphraseGeneratorTest {

    val passphraseGenerator = PassphraseGenerator()

    @Test
    fun generateAndClearPassphrase() {
        val spec = PassphraseGeneratorSpec(PassphraseStrength.STRONG,
                wordBeginningUpperCase = true, addDigit = false, addSpecialChar = true)
        val hits = HashSet<String>()
        for (i in 0..100) {
            val passphrase = passphraseGenerator.generate(spec)
            println("passphrase = ${passphrase.toStringRepresentation(false)}")
            hits.add(passphrase.toStringRepresentation(false))
            passphrase.clear()
        }

     //   println("hits=$hits")
        println("counted=${hits.size}")

        val calcCombinationCount = passphraseGenerator.calcCombinationCount(spec)
        println("combinations: $calcCombinationCount")

        println("ratio to real counted: ${hits.size/calcCombinationCount}")


        val calcBruteForceWaitingPentiumSeconds = passphraseGenerator.calcBruteForceWaitingSeconds(calcCombinationCount, BRUTEFORCE_ATTEMPTS_PENTIUM)
        val calcBruteForceWaitingSupercompSeconds = passphraseGenerator.calcBruteForceWaitingSeconds(calcCombinationCount, BRUTEFORCE_ATTEMPTS_SUPERCOMP)

        println("brute force years for Pentum: ${calcBruteForceWaitingPentiumSeconds.secondsToYear()}")
        println("brute force years for Supercomp: ${calcBruteForceWaitingSupercompSeconds.secondsToYear()}")

    }
}