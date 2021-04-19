package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.model.Password
import java.security.SecureRandom

class PassphraseGenerator: GeneratorBase<PassphraseGeneratorSpec>() {

    private val VOCALS = "aeiouy"
    private val CONSONANTS = "bcdfghjklmnpqrstvwxz"
    private val ALPHABET = VOCALS + CONSONANTS
    private val DIGITS = "1234567890"
    private val SPECIAL_CHARS = ".:!?"

    override fun generate(spec: PassphraseGeneratorSpec): Password {
        var buffer = Password(CharArray(0));

        for (i in 0 until spec.strength.passwordLength/4) {
            val word = generateWord()
            buffer.add(word)
        }

        if (spec.wordBeginningUpperCase) {
            buffer.replace(0, buffer.get(0).toUpperCase())
        }

        if (spec.addDigit) {
            buffer.add(random(DIGITS))
        }

        if (spec.addSpecialChar) {
            buffer.add(random(SPECIAL_CHARS))
        }

        return buffer
    }

    private fun generateWord(): Password {
        val word = generateTuple()
        val allowDuplicateConsonants = isVocal(word.get(word.lastIndex))
        val next = generateTuple(true, allowDuplicateConsonants)

        word.add(next)

        return word
    }

    private fun generateTuple(allowDuplicateVocals: Boolean = false,
                              allowDuplicateConsonants: Boolean = false): Password {
        val buffer = CharArray(2)
        buffer[0] = random(ALPHABET)
        var material = when(isVocal(buffer[0])) {
            true ->  CONSONANTS
            else -> VOCALS
        }
        if (allowDuplicateVocals && isVocal(buffer[0])) {
            material += buffer[0]
        }
        if (allowDuplicateConsonants && isConsonant(buffer[0])) {
            material += buffer[0]
        }

        buffer[1] = random(material)
        return Password(buffer)
    }

    override fun calcCombinationCount(spec: PassphraseGeneratorSpec): Double {
        val vocalChance = VOCALS.length / ALPHABET.length.toDouble()
        val consonantChance = CONSONANTS.length / ALPHABET.length.toDouble()
        val normalTubleComp = VOCALS.length * CONSONANTS.length
        val doubleTubleComp = normalTubleComp + (consonantChance * VOCALS.length) + (vocalChance * ALPHABET.length)
        val normalWordComp = normalTubleComp + doubleTubleComp
        val wordsComp = Math.pow(normalWordComp, spec.strength.passwordLength/2.toDouble())

        var comp = wordsComp;
        if (spec.addDigit) {
            comp *= DIGITS.length
        }
        if (spec.addSpecialChar) {
            comp *= SPECIAL_CHARS.length
        }
        return comp;
    }

    private fun random(material: String): Char {
        val index = random.nextInt(material.length)

        return material[index]
    }

    private fun isVocal(char: Char): Boolean {
        return VOCALS.contains(char, true);
    }

    private fun isConsonant(char: Char): Boolean {
            return CONSONANTS.contains(char, true);
    }

}