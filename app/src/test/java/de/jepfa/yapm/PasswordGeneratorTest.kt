package de.jepfa.yapm

import de.jepfa.yapm.service.secretgenerator.PasswordGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.PasswordGenerator
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
import org.junit.Test

class PasswordGeneratorTest {

    val passwordGenerator = PasswordGenerator()

    @Test
    fun generateAndClearPassword() {
        val spec = PasswordGeneratorSpec(PasswordStrength.SUPER_STRONG)
        val password = passwordGenerator.generate(spec)
        println("password=${password.toStringRepresentation(false)}")

        password.clear()
        println("password=${password.toStringRepresentation(false)}")

    }
}