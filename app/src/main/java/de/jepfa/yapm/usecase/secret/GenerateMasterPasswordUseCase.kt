package de.jepfa.yapm.usecase.secret

import android.content.Context
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

    override suspend fun execute(usePseudoPhrase: Boolean, activity: BaseActivity): UseCaseOutput<Password> {

        val generated = generate(usePseudoPhrase, activity)

        return UseCaseOutput(generated)
    }

    private fun generate(usePseudoPhrase: Boolean, context : Context): Password {
        if (usePseudoPhrase) {
            return PassphraseGenerator(context = context).generate(
                PassphraseGeneratorSpec(
                    strength = SecretStrength.HYPER
                )
            )
        } else {
            return PasswordGenerator(context = context).generate(
                PasswordGeneratorSpec(
                    strength = SecretStrength.HYPER
                )
            )
        }
    }

}