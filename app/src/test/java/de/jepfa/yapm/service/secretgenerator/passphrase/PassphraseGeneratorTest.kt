package de.jepfa.yapm.service.secretgenerator.passphrase

import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.BRUTEFORCE_ATTEMPTS_PENTIUM
import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.BRUTEFORCE_ATTEMPTS_SUPERCOMP
import de.jepfa.yapm.service.secretgenerator.SecretStrength
import de.jepfa.yapm.util.secondsToYear
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class PassphraseGeneratorTest {

    @Test
    fun generateAndClearPassphrase() {
        val spec = PassphraseGeneratorSpec(
            SecretStrength.STRONG,
            wordBeginningUpperCase = true, addDigit = false, addSpecialChar = true)
        val passphraseGenerator = PassphraseGenerator()

        for (i in 0..100) {
            val passphrase = passphraseGenerator.generate(spec)
            println("passphrase = ${passphrase.toFormattedPassword()}")
            passphrase.clear()
        }

        val calcCombinationCount = passphraseGenerator.calcCombinationCount(spec)
        println("combinations: $calcCombinationCount")

        val calcBruteForceWaitingPentiumSeconds = passphraseGenerator.calcBruteForceWaitingSeconds(calcCombinationCount, BRUTEFORCE_ATTEMPTS_PENTIUM)
        val calcBruteForceWaitingSupercompSeconds = passphraseGenerator.calcBruteForceWaitingSeconds(calcCombinationCount, BRUTEFORCE_ATTEMPTS_SUPERCOMP)

        println("brute force years for Pentum: ${calcBruteForceWaitingPentiumSeconds.secondsToYear()}")
        println("brute force years for Supercomp: ${calcBruteForceWaitingSupercompSeconds.secondsToYear()}")
    }

    @Test
    fun testCalcCombinations() {
        val spec = PassphraseGeneratorSpec(SecretStrength.ONE_WORD)
        val passphraseGenerator = PassphraseGenerator(vocals = "ai", consonants = "hst")

        val hits = HashSet<String>()
        for (i in 0..500000) {
            val passphrase = passphraseGenerator.generate(spec)
            hits.add(passphrase.toString())
            passphrase.clear()
        }

        val combinationCount = passphraseGenerator.calcCombinationCount(spec)

        println("hits=$hits")
        println("real combinations: ${hits.size}")
        println("calculated combinations: $combinationCount")

        Assert.assertEquals(hits.size.toDouble(), combinationCount, 0.1)

    }

    @Test
    fun testCalcCombinationsRealWord() {
        val spec = PassphraseGeneratorSpec(SecretStrength.ONE_WORD)
        val passphraseGenerator = PassphraseGenerator()

        val combinationCount = passphraseGenerator.calcCombinationCount(spec)
        println("calculated combinations: $combinationCount")

        val hits = HashSet<String>()
        val counter = AtomicInteger()
        while (hits.size < combinationCount.toLong()) {
            val passphrase = passphraseGenerator.generate(spec)
            val new = hits.add(passphrase.toString())
            if (new || counter.incrementAndGet() % 100000 == 0) {
                println("current attempt: ${counter.get()} (${hits.size} < ${combinationCount.toLong()}): $passphrase isNew=$new")
            }
            passphrase.clear()

        }

        println("real combinations: ${hits.size}")

        Assert.assertEquals(hits.size.toDouble(), combinationCount, 0.1)

    }
}