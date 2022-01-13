package de.jepfa.yapm.service.secretgenerator.password

import de.jepfa.yapm.service.secretgenerator.GeneratorBase
import de.jepfa.yapm.service.secretgenerator.SecretStrength
import de.jepfa.yapm.util.secondsToYear
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class PasswordGeneratorTest {

    val passwordGenerator = PasswordGenerator()

    @Test
    fun generatePassword() {
        val spec = PasswordGeneratorSpec(
            SecretStrength.NORMAL,
            noDigits = false, excludeSpecialChars = false, onlyLowerCase = false)
        for (i in 1..10) {
            val password = passwordGenerator.generate(spec)
            println("password = ${password.toFormattedPassword()}")
            password.clear()
        }

        val calcCombinationCount = passwordGenerator.calcCombinationCount(spec)
        println("combinations: $calcCombinationCount")

        val calcBruteForceWaitingPentiumSeconds = passwordGenerator.calcBruteForceWaitingSeconds(calcCombinationCount,
            GeneratorBase.BRUTEFORCE_ATTEMPTS_PENTIUM
        )
        val calcBruteForceWaitingSupercompSeconds = passwordGenerator.calcBruteForceWaitingSeconds(calcCombinationCount,
            GeneratorBase.BRUTEFORCE_ATTEMPTS_SUPERCOMP
        )

        println("brute force years for Pentum: ${calcBruteForceWaitingPentiumSeconds.secondsToYear()}")
        println("brute force years for Supercomp: ${calcBruteForceWaitingSupercompSeconds.secondsToYear()}")

    }

    @Test
    fun testCalcCombinationsPlainWord() {
        val spec = PasswordGeneratorSpec(onlyLowerCase = true, excludeSpecialChars = true,
            noDigits = true, strength = SecretStrength.ONE_WORD)
        val passwordGenerator = PasswordGenerator()

        val combinationCount = passwordGenerator.calcCombinationCount(spec)
        println("calculated combinations: $combinationCount")

        val hits = HashSet<String>()
        val counter = AtomicInteger()
        while (hits.size < combinationCount.toLong()) {
            val password = passwordGenerator.generate(spec)
            val new = hits.add(password.toString())
            if (new || counter.incrementAndGet() % 100000 == 0) {
                println("current attempt: ${counter.get()} (${hits.size} < ${combinationCount.toLong()}): $password isNew=$new")
            }
            password.clear()

        }

        println("real combinations: ${hits.size}")

        Assert.assertEquals(hits.size.toDouble(), combinationCount, 0.1)

    }


    @Test
    fun testCalcCombinationsAllWord() {
        val spec = PasswordGeneratorSpec(onlyLowerCase = false, excludeSpecialChars = false,
            noDigits = false, strength = SecretStrength.ONE_WORD)
        val passwordGenerator = PasswordGenerator()

        val combinationCount = passwordGenerator.calcCombinationCount(spec)
        println("calculated combinations: $combinationCount")

        val hits = HashSet<String>()
        val counter = AtomicInteger()
        while (hits.size < combinationCount.toLong()) {
            val password = passwordGenerator.generate(spec)
            val new = hits.add(password.toString())
            if (counter.incrementAndGet() % 100000 == 0) {
                println("current attempt: ${counter.get()} (${hits.size} < ${combinationCount.toLong()}): $password isNew=$new")
            }
            password.clear()

        }

        println("real combinations: ${hits.size}")

        Assert.assertEquals(hits.size.toDouble(), combinationCount, 0.1)

    }
}