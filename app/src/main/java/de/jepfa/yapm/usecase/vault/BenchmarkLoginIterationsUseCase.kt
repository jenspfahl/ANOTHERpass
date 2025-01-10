package de.jepfa.yapm.usecase.vault

import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.UseCase
import de.jepfa.yapm.usecase.UseCaseOutput
import de.jepfa.yapm.util.toReadableFormat
import kotlin.time.Duration.Companion.milliseconds

object BenchmarkLoginIterationsUseCase: UseCase<BenchmarkLoginIterationsUseCase.Input, Long, BaseActivity> {

    data class Input(val iterations: Int, val cipherAlgorithm: CipherAlgorithm)

    override suspend fun execute(input: Input, activity: BaseActivity): UseCaseOutput<Long> {

        val startMillis = System.currentTimeMillis()
        SecretService.generatePBESecretKeyForGivenIterations(
            Password("dummydummydummydummydummydummy"),
            Key("saltysaltysaltysaltysaltysalty".toByteArray()),
            input.iterations,
            input.cipherAlgorithm,
            activity
        )

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
        AlertDialog.Builder(activity)
            .setTitle(R.string.result)
            .setMessage("${activity.getString(R.string.login_iterations)}: ${input.iterations.toReadableFormat()} \n\n${activity.getString(R.string.elapsed_time)}: ${elapsedTime.milliseconds}")
            .setNegativeButton(R.string.close, null)
            .show()
    }

}