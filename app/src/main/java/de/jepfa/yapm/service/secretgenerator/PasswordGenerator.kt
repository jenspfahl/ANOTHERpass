package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.model.Password
import java.security.SecureRandom

class PasswordGenerator {
    val ALPHA_CHARS_LOWER_CASE = "abcdefghijklmnopqrstuvwxyz"
    val ALPHA_CHARS_UPPER_CASE = ALPHA_CHARS_LOWER_CASE.toUpperCase()
    val DIGITS = "0123456789"
    val SPECIAL_CHARS_1 = "!?-_,.;:/"
    val SPECIAL_CHARS_2 = "$%&()[]{}"

    private val random = SecureRandom()

    fun generatePassword(spec: PasswordGeneratorSpec): Password {
        val buffer = CharArray(spec.strength.passwordLength)
        val material = extractMaterial(spec)

        for (i in 0 until buffer.size) {
            buffer[i] = random(material)
        }
        return Password(buffer)
    }

    private fun extractMaterial(spec: PasswordGeneratorSpec): String {
        var material = ALPHA_CHARS_UPPER_CASE + DIGITS
        if (!spec.excludeSpecialChars) {
            material += SPECIAL_CHARS_1
            if (!spec.onlyCommonSpecialChars) {
                material += SPECIAL_CHARS_2
            }
        }
        if (!spec.onlyUpperCase) {
            material += ALPHA_CHARS_LOWER_CASE
        }
        return material
    }

    private fun random(material: String): Char {
        val index = random.nextInt(material.length)

        return material[index]
    }

}