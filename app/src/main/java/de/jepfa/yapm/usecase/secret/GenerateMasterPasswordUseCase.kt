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

object GenerateMasterPasswordUseCase: UseCase<Boolean, Pair<Password, Double>, BaseActivity> {

    override suspend fun execute(usePseudoPhrase: Boolean, activity: BaseActivity): UseCaseOutput<Pair<Password, Double>> {

        val generated = generate(usePseudoPhrase, activity)

        return UseCaseOutput(generated)
    }

    private fun generate(usePseudoPhrase: Boolean, context : Context): Pair<Password, Double> {
        return if (usePseudoPhrase) {
            val generator = PassphraseGenerator(context = context)
            val spec = PassphraseGeneratorSpec(strength = SecretStrength.HYPER)

            Pair(generator.generate(spec), generator.calcCombinationCount(spec))
        } else {
            val generator = PasswordGenerator(context = context)
            val spec = PasswordGeneratorSpec(strength = SecretStrength.HYPER)

            Pair(generator.generate(spec), generator.calcCombinationCount(spec))
        }
    }

}