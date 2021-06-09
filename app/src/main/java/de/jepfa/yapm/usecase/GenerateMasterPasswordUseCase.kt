package de.jepfa.yapm.usecase

import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secretgenerator.*
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.password.PasswordGenerator
import de.jepfa.yapm.service.secretgenerator.password.PasswordGeneratorSpec

object GenerateMasterPasswordUseCase {

    private var passphraseGenerator = PassphraseGenerator()
    private var passwordGenerator = PasswordGenerator()

    fun execute(usePseudoPhrase: Boolean): Password {

        if (usePseudoPhrase) {
            return passphraseGenerator.generate(
                    PassphraseGeneratorSpec(
                            strength = SecretStrength.HYPER)
            )
        }
        else {
            return passwordGenerator.generate(
                    PasswordGeneratorSpec(
                            strength = SecretStrength.HYPER)
            )
        }

    }

}