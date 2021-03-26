package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.secretgenerator.*

object GenerateMasterPasswordUseCase {

    private var passphraseGenerator = PassphraseGenerator()
    private var passwordGenerator = PasswordGenerator()

    fun execute(usePseudoPhrase: Boolean): Password {

        if (usePseudoPhrase) {
            return passphraseGenerator.generate(
                    PassphraseGeneratorSpec(
                            strength = PassphraseStrength.EXTREME))
        }
        else {
            return passwordGenerator.generate(
                    PasswordGeneratorSpec(
                            strength = PasswordStrength.SUPER_STRONG))
        }

    }

}