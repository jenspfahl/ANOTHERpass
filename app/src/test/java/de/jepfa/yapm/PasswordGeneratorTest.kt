package de.jepfa.yapm

import de.jepfa.yapm.model.Credential
import de.jepfa.yapm.service.secretgenerator.PasswordGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.PasswordGenerator
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
import org.junit.Test

class PasswordGeneratorTest {

    val passwordGenerator = PasswordGenerator()

    @Test
    fun generateAndClearPassword() {
        val spec = PasswordGeneratorSpec(PasswordStrength.SUPER_STRONG, onlyCommonSpecialChars = true)
        val password = passwordGenerator.generatePassword(spec)
        println("password=${password.debugToString()}")

        val credGoogle = Credential("Google", "test", password, false)
        println(credGoogle)

        password.clear()
        println("password=${password.debugToString()}")

    }
}