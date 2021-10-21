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
import de.jepfa.yapm.ui.login.LoginActivity
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.usecase.vault.CreateVaultUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BruteForceTest {

    val TAG = "YAPM/BF"

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
            CreateVaultUseCase.execute(
                CreateVaultUseCase.Input(LoginData(pin, masterPassword), false, cipherAlgorithm),
                loginActivity)
            attack(loginActivity)
        }

    }

    private fun attack(activity: BaseActivity) {
        val maxPin = 10000000
        for (i in 0 until maxPin) {
            val testPin = Password(i.toString())
            val success = login(testPin, masterPassword, activity)

            if (i % 100 == 0 && i > 0) {
                Log.i(TAG, "current pin=$testPin max pin=$maxPin")
            }
            if (success) {
                Log.i(TAG, "CRACKED!!!!!! pin=$testPin")
                break
            }
        }
    }

    private fun login(pin: Password, masterPassword: Password, activity: BaseActivity): Boolean {
        return LoginUseCase.execute(LoginData(pin, masterPassword), activity).success
    }
}