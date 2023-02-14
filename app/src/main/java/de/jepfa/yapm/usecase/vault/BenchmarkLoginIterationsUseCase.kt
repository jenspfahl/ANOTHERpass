package de.jepfa.yapm.usecase.vault

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY
import de.jepfa.yapm.service.PreferenceService.DATA_MPT_CREATED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_EXPORTED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_MODIFIED_AT
import de.jepfa.yapm.service.PreferenceService.STATE_LOGIN_DENIED_AT
import de.jepfa.yapm.service.PreferenceService.STATE_MASTER_PASSWD_TOKEN_COUNTER
import de.jepfa.yapm.service.PreferenceService.STATE_PREVIOUS_LOGIN_ATTEMPTS
import de.jepfa.yapm.service.PreferenceService.STATE_PREVIOUS_LOGIN_SUCCEEDED_AT
import de.jepfa.yapm.service.secret.PbkdfIterationService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.usecase.UseCase
import de.jepfa.yapm.usecase.UseCaseOutput
import de.jepfa.yapm.util.*
import kotlin.time.Duration.Companion.milliseconds

object BenchmarkLoginIterationsUseCase: UseCase<BenchmarkLoginIterationsUseCase.Input, Long, BaseActivity> {

    data class Input(val iterations: Int, val cipherAlgorithm: CipherAlgorithm)

    override suspend fun execute(input: Input, activity: BaseActivity): UseCaseOutput<Long> {

        val startMillis = System.currentTimeMillis()
        SecretService.generatePBESecretKey(
            Password("dummydummydummydummydummydummy"),
            Key("saltysaltysaltysaltysaltysalty".toByteArray()),
            input.iterations,
            input.cipherAlgorithm
        )

        return UseCaseOutput(System.currentTimeMillis() - startMillis)
    }

    fun openStartBenchmarkingDialog(input: Input, activity: BaseActivity, startedHandler: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("Benchmark") //(activity.getString(R.string.title_reset_all))
            .setMessage("Running a login benchmark may take some time, from some seconds or less up to some minutes depending on the CPU power of your device, the higher the iterations are." +
                    "Try to find a value starting from the smallest one which takes an acceptable time, e.g. 1-3 seconds. This will be your iterations amount.") // R.string.pbkdf_iterations
            .setPositiveButton("Start") { _, _ ->
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
            .setTitle("Benchmark") //(activity.getString(R.string.title_reset_all))
            .setMessage("Used iterations: ${input.iterations.toReadableFormat()} \nElapsed time: ${elapsedTime.milliseconds}") // R.string.pbkdf_iterations
            .setNegativeButton(R.string.close, null)
            .show()
    }

}