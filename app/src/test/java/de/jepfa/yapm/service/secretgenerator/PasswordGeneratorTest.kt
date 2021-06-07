package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.service.secretgenerator.PasswordGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.PasswordGenerator
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
import de.jepfa.yapm.util.secondsToYear
import org.junit.Test

class PasswordGeneratorTest {

    val passwordGenerator = PasswordGenerator()

    @Test
    fun generatePassword() {
        val spec = PasswordGeneratorSpec(PasswordStrength.NORMAL,
            noDigits = false, excludeSpecialChars = false, onlyLowerCase = false)
        for (i in 1..10) {
            val password = passwordGenerator.generate(spec)
            println("password = ${password.toStringRepresentation(false)}")
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
}