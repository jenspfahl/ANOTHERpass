package de.jepfa.yapm.usecase.secret

import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secretgenerator.SecretStrength
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.password.PasswordGenerator
import de.jepfa.yapm.service.secretgenerator.password.PasswordGeneratorSpec
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.UseCase
import de.jepfa.yapm.usecase.UseCaseOutput

object GenerateMasterPasswordUseCase: UseCase<Boolean, Password, BaseActivity> {

    private var passphraseGenerator = PassphraseGenerator()
    private var passwordGenerator = PasswordGenerator()

    override fun execute(usePseudoPhrase: Boolean, activity: BaseActivity): UseCaseOutput<Password> {

        val generated = generate(usePseudoPhrase)

        return UseCaseOutput(generated)
    }

    private fun generate(usePseudoPhrase: Boolean): Password {
        if (usePseudoPhrase) {
            return passphraseGenerator.generate(
                PassphraseGeneratorSpec(
                    strength = SecretStrength.HYPER
                )
            )
        } else {
            return passwordGenerator.generate(
                PasswordGeneratorSpec(
                    strength = SecretStrength.HYPER
                )
            )
        }
    }

}