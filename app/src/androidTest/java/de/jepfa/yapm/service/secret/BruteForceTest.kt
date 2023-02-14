package de.jepfa.yapm.service.secret

import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.login.LoginActivity
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.usecase.vault.CreateVaultUseCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BruteForceTest {

    val TAG = "YAPM/BF"

    val maxPins = 1000 // 10_000_000
    var currentAttempt = 0
    val pin = Password("123")
    val masterPassword = Password("abcd")
    val cipherAlgorithm = CipherAlgorithm.AES_256

    lateinit var loginScenario: ActivityScenario<LoginActivity>


    @Before
    fun setup() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)
        loginScenario = launchActivity(intent)

    }

    @Test
    fun testBruteForce() {
        loginScenario.onActivity { loginActivity ->

            val input = CreateVaultUseCase.Input(LoginData(pin, masterPassword), PbkdfIterationService.DEFAULT_PBKDF_ITERATIONS, cipherAlgorithm)
            UseCaseBackgroundLauncher(CreateVaultUseCase)
                .launch(loginActivity, input)
                { output ->
                    Assert.assertTrue(output.success)
                    attack(loginActivity)
                }
        }

    }

    private fun attack(activity: BaseActivity) {
        if (currentAttempt >= maxPins) {
            Log.i(TAG, "Game over")
        }
        else {
            val testPin = Password(currentAttempt.toString())
            val input = LoginData(pin, masterPassword)
            UseCaseBackgroundLauncher(LoginUseCase)
                .launch(activity, input)
                { output ->
                    if (!output.success) {
                        currentAttempt++
                        attack(activity)
                    } else {
                        // cracked!
                        Log.i(TAG, "CRACKED!!!!!! pin=$testPin")
                    }
                }
        }
    }

}