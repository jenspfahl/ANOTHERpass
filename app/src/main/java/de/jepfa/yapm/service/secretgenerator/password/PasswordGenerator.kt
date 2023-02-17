package de.jepfa.yapm.service.secretgenerator.password

import android.content.Context
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secretgenerator.GeneratorBase
import java.security.SecureRandom
import java.util.*

class PasswordGenerator(
    val upperCase: String = DEFAULT_ALPHA_CHARS_UPPER_CASE,
    val lowerCase: String = DEFAULT_ALPHA_CHARS_LOWER_CASE,
    val digits: String = DEFAULT_DIGITS,
    val specialChars: String = DEFAULT_SPECIAL_CHARS,
    val extendedSpecialChars: String = EXTENDED_SPECIAL_CHARS,
    context: Context?,
    secureRandom: SecureRandom? = null,
) : GeneratorBase<PasswordGeneratorSpec>(context, secureRandom) {


    override fun generate(spec: PasswordGeneratorSpec): Password {
        while(true){
            val buffer = generatePassword(spec, context)
            if (matchSpec(spec, buffer)) {
                return Password(buffer)
            }
        }
    }

    override fun calcCombinationCount(spec: PasswordGeneratorSpec): Double {
        val material = extractMaterial(spec)
        val specialChars = getSpecialCharsToUse(spec)
        var combinations = Math.pow(material.length.toDouble(), spec.strength.ordinaryPasswordLength.toDouble())


        // step 1: always remove all with no lower case

        val withNoLowerCaseCombinations = Math.pow(material.length.toDouble() - lowerCase.length, spec.strength.ordinaryPasswordLength.toDouble())
        combinations -= withNoLowerCaseCombinations

        // step 2: dynamic in-/exclusion

        // if with upper case
        if (!spec.noUpperCase) {
            // remove all with no upper case
            val noUpperCaseCombinations = Math.pow(material.length.toDouble() - upperCase.length, spec.strength.ordinaryPasswordLength.toDouble())
            combinations -= noUpperCaseCombinations

            // add back all with no upper case AND no lower case (still removed at step 1)
            val noLowerCaseAndUpperCaseCombinations = Math.pow(material.length.toDouble() - lowerCase.length - upperCase.length, spec.strength.ordinaryPasswordLength.toDouble())
            combinations += noLowerCaseAndUpperCaseCombinations
        }

        // if with digits
        if (!spec.noDigits) {
            // remove all with no digits
            val noDigitsCombinations = Math.pow(material.length.toDouble() - digits.length, spec.strength.ordinaryPasswordLength.toDouble())
            combinations -= noDigitsCombinations

            // add back all with no digits AND no lower case (still removed at step 1)
            val noLowerCaseAndDigitsCombinations = Math.pow(material.length.toDouble() - lowerCase.length - digits.length, spec.strength.ordinaryPasswordLength.toDouble())
            combinations += noLowerCaseAndDigitsCombinations
        }

        // if with special chars
        if (!spec.noSpecialChars) {

            // remove all with no special chars
            val noSpecialCharsCombinations = Math.pow(material.length.toDouble() - specialChars.length, spec.strength.ordinaryPasswordLength.toDouble())
            combinations -= noSpecialCharsCombinations

            // add back all with no special chars AND no lower case (still removed at step 1)
            val noLowerCaseAndSpecialCharsCombinations = Math.pow(material.length.toDouble() - lowerCase.length - specialChars.length, spec.strength.ordinaryPasswordLength.toDouble())
            combinations += noLowerCaseAndSpecialCharsCombinations


        }

        // step 3: inclusion, add back to much removed

        if (!spec.noUpperCase && !spec.noDigits) {
            // if with upper case AND digits, add back all with no upper case AND no digit (still removed at step 2)
            val noUpperCaseAndNoDigitsCombinations = Math.pow(material.length.toDouble() - upperCase.length - digits.length, spec.strength.ordinaryPasswordLength.toDouble())
            combinations += noUpperCaseAndNoDigitsCombinations
        }
        if (!spec.noUpperCase && !spec.noSpecialChars) {
            // if with upper case AND special chars, add back all with no upper case AND no special char (still removed at step 2)
            val noUpperCaseAndNoSpecialCharsCombinations = Math.pow(material.length.toDouble() - upperCase.length - specialChars.length, spec.strength.ordinaryPasswordLength.toDouble())
            combinations += noUpperCaseAndNoSpecialCharsCombinations
        }
        if (!spec.noDigits && !spec.noSpecialChars) {
            // if with digits AND special chars, add back all with no digits AND no special char (still removed at step 2)
            val noDigitsAndNoSpecialCharsCombinations = Math.pow(material.length.toDouble() - digits.length - specialChars.length, spec.strength.ordinaryPasswordLength.toDouble())
            combinations += noDigitsAndNoSpecialCharsCombinations
        }

        // step 4: exclusion again

        // if no exclusion at all
        if (!spec.noUpperCase && !spec.noDigits && !spec.noSpecialChars) {

            // remove only with lower case
            val onlyLowerCaseCombinations = Math.pow(lowerCase.length.toDouble(), spec.strength.ordinaryPasswordLength.toDouble())
            combinations -= onlyLowerCaseCombinations

            // remove all with only upper case
            val onlyUpperCaseCombinations = Math.pow(upperCase.length.toDouble(), spec.strength.ordinaryPasswordLength.toDouble())
            combinations -= onlyUpperCaseCombinations

            // remove all with only digits
            val onlyDigitsCombinations = Math.pow(digits.length.toDouble(), spec.strength.ordinaryPasswordLength.toDouble())
            combinations -= onlyDigitsCombinations

            // remove all with only special chars
            val onlySpecialCharsCombinations = Math.pow(specialChars.length.toDouble(), spec.strength.ordinaryPasswordLength.toDouble())
            combinations -= onlySpecialCharsCombinations
        }

        return combinations;
    }

    private fun getSpecialCharsToUse(spec: PasswordGeneratorSpec): String {
        return if (spec.useExtendedSpecialChars) {
            specialChars + extendedSpecialChars
        } else {
            specialChars
        }
    }

    private fun matchSpec(spec: PasswordGeneratorSpec, buffer: CharArray): Boolean {
        val specialChars = getSpecialCharsToUse(spec)

        if (!containsChar(buffer, lowerCase)) {
            // must always have lower case
            return false
        }
        if (!spec.noUpperCase && !containsChar(buffer, upperCase)) {
            // only lower case not selected so must have upper case
            return false
        }
        if (!spec.noDigits && !containsChar(buffer, digits)) {
            // no digits not selected so must have digits
            return false
        }
        if (!spec.noSpecialChars && !containsChar(buffer, specialChars)) {
            // exclude special chars not selected so must have special chars
            return false
        }

        return true
    }

    private fun containsChar(buffer: CharArray, material: String): Boolean {
        return buffer.filter { c -> material.contains(c) }.isNotEmpty()
    }

    private fun generatePassword(spec: PasswordGeneratorSpec, context: Context?): CharArray {
        val buffer = CharArray(spec.strength.ordinaryPasswordLength)
        val material = extractMaterial(spec)

        for (i in buffer.indices) {
            buffer[i] = random(material)
        }
        return buffer
    }

    private fun extractMaterial(spec: PasswordGeneratorSpec): String {
        var material = lowerCase
        if (!spec.noDigits) {
            material += digits
        }
        if (!spec.noSpecialChars) {
            material += specialChars
            if (spec.useExtendedSpecialChars) {
                material += extendedSpecialChars
            }
        }
        if (!spec.noUpperCase) {
            material += upperCase
        }
        return material
    }

}