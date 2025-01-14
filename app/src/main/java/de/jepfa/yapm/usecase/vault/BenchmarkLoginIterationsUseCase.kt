package de.jepfa.yapm.usecase.vault

import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.encrypted.KdfConfig
import de.jepfa.yapm.model.encrypted.KeyDerivationFunction
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.Argon2Service
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.generateSecretKey
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.UseCase
import de.jepfa.yapm.usecase.UseCaseOutput
import de.jepfa.yapm.util.toReadableFormat
import kotlin.time.Duration.Companion.milliseconds

object BenchmarkLoginIterationsUseCase: UseCase<BenchmarkLoginIterationsUseCase.Input, Long, BaseActivity> {

    data class Input(val kdfConfig: KdfConfig, val cipherAlgorithm: CipherAlgorithm)

    override suspend fun execute(input: Input, activity: BaseActivity): UseCaseOutput<Long> {

        val startMillis = System.currentTimeMillis()
        if (input.kdfConfig.isArgon2()) {
            val argonDerivedKey = Argon2Service.derive(
                Password("dummydummydummydummydummydummy"),
                Key("saltysaltysaltysaltysaltysalty".toByteArray()),
                input.kdfConfig
            )
            generateSecretKey(argonDerivedKey, input.cipherAlgorithm, activity)
        }
        else {
            SecretService.generatePBESecretKeyForGivenIterations(
                Password("dummydummydummydummydummydummy"),
                Key("saltysaltysaltysaltysaltysalty".toByteArray()),
                input.kdfConfig.iterations,
                input.cipherAlgorithm,
                activity
            )
        }

        return UseCaseOutput(System.currentTimeMillis() - startMillis)
    }

    fun openStartBenchmarkingDialog(input: Input, activity: BaseActivity, startedHandler: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.benchmark_login_iterations)
            .setMessage(R.string.benchmark_login_iterations_explanation)
            .setPositiveButton(R.string.start) { _, _ ->
                UseCaseBackgroundLauncher(BenchmarkLoginIterationsUseCase)
                    .launch(activity, input)
                    { output ->
                        openResultDialog(input, output.data, activity)
                        startedHandler()
                    }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }


    fun openResultDialog(input: Input, elapsedTime: Long, activity: BaseActivity) {
        val sb = StringBuilder()
        sb.append("${activity.getString(R.string.login_iterations)}: ${input.kdfConfig.iterations.toReadableFormat()}").append(System.lineSeparator()).append(System.lineSeparator())
        if (input.kdfConfig.isArgon2()) {
            sb.append("${activity.getString(R.string.login_argon2_memcost_unit)}: ${input.kdfConfig.memCostInMiB!!.toReadableFormat()}").append(System.lineSeparator()).append(System.lineSeparator())
        }
        sb.append("${activity.getString(R.string.elapsed_time)}: ${elapsedTime.milliseconds}").append(System.lineSeparator())
        AlertDialog.Builder(activity)
            .setTitle(R.string.result)
            .setMessage(sb.toString())
            .setNegativeButton(R.string.close, null)
            .show()
    }

}