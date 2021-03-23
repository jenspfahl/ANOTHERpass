package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.model.Password
import java.security.SecureRandom

class PasswordGenerator : GeneratorBase<PasswordGeneratorSpec>() {

    val ALPHA_CHARS_LOWER_CASE = "abcdefghijklmnopqrstuvwxyz"
    val ALPHA_CHARS_UPPER_CASE = ALPHA_CHARS_LOWER_CASE.toUpperCase()
    val DIGITS = "0123456789"
    val SPECIAL_CHARS_1 = "!?-_,.;:/"
    val SPECIAL_CHARS_2 = "$%&()[]{}"

    override fun generate(spec: PasswordGeneratorSpec): Password {
        val buffer = CharArray(spec.strength.passwordLength)
        val material = extractMaterial(spec)

        for (i in 0 until buffer.size) {
            buffer[i] = random(material)
        }
        return Password(buffer)
    }

    override fun calcCombinationCount(spec: PasswordGeneratorSpec): Double {
        val material = extractMaterial(spec)

        return Math.pow(material.length.toDouble(), spec.strength.passwordLength.toDouble())
    }

    private fun extractMaterial(spec: PasswordGeneratorSpec): String {
        var material = ALPHA_CHARS_LOWER_CASE
        if (!spec.noDigits) {
            material += DIGITS
        }
        if (!spec.excludeSpecialChars) {
            material += SPECIAL_CHARS_1
            if (!spec.onlyCommonSpecialChars) {
                material += SPECIAL_CHARS_2
            }
        }
        if (!spec.onlyLowerCase) {
            material += ALPHA_CHARS_UPPER_CASE
        }
        return material
    }

    private fun random(material: String): Char {
        val index = random.nextInt(material.length)

        return material[index]
    }

}