package de.jepfa.yapm.service.secretgenerator.password

import de.jepfa.yapm.service.secretgenerator.GeneratorBase
import de.jepfa.yapm.service.secretgenerator.SecretStrength
import de.jepfa.yapm.util.secondsToYear
import org.junit.Assert
import org.junit.Test
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger

class PasswordGeneratorTest {

    val rnd = SecureRandom()
    val passwordGenerator = PasswordGenerator(context = null, secureRandom = rnd)

    @Test
    fun generatePassword() {
        val spec = PasswordGeneratorSpec(
            SecretStrength.NORMAL,
            noDigits = false, noSpecialChars = false, noUpperCase = false)
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
    fun testCalcCombinationsLowerCaseWord() {
        val spec = PasswordGeneratorSpec(noUpperCase = true, noSpecialChars = true,
            noDigits = true, strength = SecretStrength.ONE_WORD)

        calcAndGenAllCombinations(spec, 50)
    }

    @Test
    fun testCalcCombinationsAllCaseWord() {
        val spec = PasswordGeneratorSpec(noSpecialChars = true,
            noDigits = true, strength = SecretStrength.ONE_WORD)

        calcAndGenAllCombinations(spec, 50)
    }

    @Test
    fun testCalcCombinationsLowerCaseDigitsWord() {
        val spec = PasswordGeneratorSpec(noSpecialChars = true,
            noUpperCase = true, strength = SecretStrength.ONE_WORD)

        calcAndGenAllCombinations(spec, 50)
    }

    @Test
    fun testCalcCombinationsLowerCaseSpecialCharWord() {
        val spec = PasswordGeneratorSpec(noDigits = true,
            noUpperCase = true, strength = SecretStrength.ONE_WORD)

        calcAndGenAllCombinations(spec, 50)
    }

    @Test
    fun testCalcCombinationsLowerExtendedCaseSpecialCharWord() {
        val spec = PasswordGeneratorSpec(noDigits = true,
            noUpperCase = true, useExtendedSpecialChars = true, strength = SecretStrength.ONE_WORD)

        calcAndGenAllCombinations(spec, 50)
    }

    @Test
    fun testCalcCombinationsNoUpperCaseWord() {
        val spec = PasswordGeneratorSpec(noUpperCase = true, strength = SecretStrength.ONE_WORD)

        calcAndGenAllCombinations(spec, 50)
    }

    @Test
    fun testCalcCombinationsNoDigitsWord() {
        val spec = PasswordGeneratorSpec(noDigits = true, strength = SecretStrength.ONE_WORD)

        calcAndGenAllCombinations(spec, 50)
    }

    @Test
    fun testCalcCombinationsNoSpecialCharsWord() {
        val spec = PasswordGeneratorSpec(noSpecialChars = true, strength = SecretStrength.ONE_WORD)

        calcAndGenAllCombinations(spec, 50)
    }

    @Test
    fun testCalcCombinationsAllWord() {
        val spec = PasswordGeneratorSpec(strength = SecretStrength.ONE_WORD)

        calcAndGenAllCombinations(spec, 10)

    }

    private fun calcAndGenAllCombinations(spec: PasswordGeneratorSpec, keepGoingTimes: Int) {
        val passwordGenerator = PasswordGenerator(
            upperCase = "ABCDEFG",
            lowerCase = "abcdef",
            digits = "01234",
            specialChars = "!.#%",
            extendedSpecialChars = "()[]",
            context = null,
            secureRandom = rnd
        )

        val combinationCount = passwordGenerator.calcCombinationCount(spec)
        println("calculated combinations: $combinationCount")

        val iteration = 1000// 100000
        val hits = HashSet<String>()
        val counter = AtomicInteger()
        var mark = 0;
        var keepGoing = false;
        while (hits.size.toLong() < combinationCount.toLong() || keepGoing) {
            val password = passwordGenerator.generate(spec)
            val new = hits.add(password.toString())
            if (counter.incrementAndGet() % iteration == 0) {
                val percentage = (hits.size / combinationCount) * 100
                var keepGoingString = ""
                if (keepGoing) {
                    val keepGoingPercentage = (mark.toDouble() / (iteration * keepGoingTimes).toDouble()) * 100
                    keepGoingString="keepGoing - ${keepGoingPercentage.toInt()}%"
                }
                println("current attempt: ${counter.get()} (${hits.size} / ${combinationCount.toLong()} = ${percentage.toInt()}%): $password isNew=$new $keepGoingString")
            }
            password.clear()
            if (hits.size.toLong() == combinationCount.toLong()) {
                keepGoing = true
            }
            if (keepGoing) {
                mark++
                keepGoing = mark <= iteration * keepGoingTimes
            }
        }

        println("real combinations: ${hits.size}")

        Assert.assertEquals(combinationCount, hits.size.toDouble(), 0.1)
    }
}