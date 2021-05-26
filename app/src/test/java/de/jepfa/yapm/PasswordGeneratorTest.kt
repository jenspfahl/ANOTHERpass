package de.jepfa.yapm

import de.jepfa.yapm.service.secretgenerator.PasswordGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.PasswordGenerator
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
import org.junit.Test

class PasswordGeneratorTest {

    val passwordGenerator = PasswordGenerator()

    @Test
    fun generatePassword() {
        val spec = PasswordGeneratorSpec(PasswordStrength.EASY,
            noDigits = false, excludeSpecialChars = false, onlyLowerCase = false)
        for (i in 1..10) {
            val password = passwordGenerator.generate(spec)
            println("password=${password.toStringRepresentation(false)}")
            password.clear()
        }


    }
}