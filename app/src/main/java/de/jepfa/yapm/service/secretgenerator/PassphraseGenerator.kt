package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.model.secret.Password
import java.lang.Math.ceil
import java.lang.Math.pow

class PassphraseGenerator: GeneratorBase<PassphraseGeneratorSpec>() {

    private val VOCALS = "aeiouy"
    private val CONSONANTS = "bcdfghjklmnpqrstvwxz"
    private val ALPHABET = VOCALS + CONSONANTS
    private val DIGITS = "1234567890"
    private val SPECIAL_CHARS = "!?-,.:/$%&@#"


    override fun generate(spec: PassphraseGeneratorSpec): Password {
        var buffer = Password(CharArray(0));

        for (i in 0 until strengthToWordCount(spec.strength)) {
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
        val recentEndsWithVocal = isVocal(word.last())
        val next = generateTuple(recentEndsWithVocal)

        word.add(next)

        return word
    }

    private fun generateTuple(recentEndsWithVocal: Boolean = false): Password {
        val buffer = CharArray(2)
        buffer[0] = random(ALPHABET)
        val char = buffer[0]
        var material = when(isVocal(char)) {
            true ->  ALPHABET
            else -> VOCALS
        }
        if (recentEndsWithVocal && isConsonant(char)) {
            material += char
        }

        buffer[1] = random(material)
        return Password(buffer)
    }

    override fun calcCombinationCount(spec: PassphraseGeneratorSpec): Double {
        val vocalLength = VOCALS.length.toDouble()
        val consonantLength = CONSONANTS.length.toDouble()
        val alphabetLength = ALPHABET.length.toDouble()

        val vocalChance = vocalLength / alphabetLength
        val consonantChance = consonantLength / alphabetLength

        val tupleCombinations = alphabetLength * ((vocalChance * alphabetLength) + (consonantChance * vocalLength))
        val tupleCombinationsWithDuplicateConsonants = alphabetLength * ((vocalChance * alphabetLength) + (consonantChance * (vocalLength + 1)))

        val wordCombinations = tupleCombinations *
                ((consonantChance * tupleCombinations) + (vocalChance * tupleCombinationsWithDuplicateConsonants))
        var combinations = ceil(pow(wordCombinations, strengthToWordCount(spec.strength).toDouble()))

        if (spec.addDigit) {
            combinations *= DIGITS.length
        }
        if (spec.addSpecialChar) {
            combinations *= SPECIAL_CHARS.length
        }
        return combinations;
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

    private fun strengthToWordCount(strength: PassphraseStrength): Int {
        return strength.passwordLength / 4
    }

}