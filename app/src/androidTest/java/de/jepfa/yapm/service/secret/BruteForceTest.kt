package de.jepfa.yapm.service.secret

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.usecase.CreateVaultUseCase
import de.jepfa.yapm.usecase.LoginUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.SecretKey

@RunWith(AndroidJUnit4::class)
class BruteForceTest {

    val TAG = "YAPM/BF"

    val pin = Password("123")
    val masterPassword = Password("abcd")
    val cipherAlgorithm = CipherAlgorithm.AES_256

    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        CreateVaultUseCase.execute(pin, masterPassword, false, cipherAlgorithm, context)
    }

    @Test
    fun attack() {
        val maxPin = 10000000
        for (i in 0 until maxPin) {
            val testPin = Password(i.toString())
            val success = login(testPin, masterPassword)

            if (i % 100 == 0 && i > 0) {
                Log.i(TAG, "current pin=$testPin max pin=$maxPin")
            }
            if (success) {
                Log.i(TAG, "CRACKED!!!!!! pin=$testPin")
                break
            }
        }
    }

    private fun login(pin: Password, masterPassword: Password): Boolean {
        return LoginUseCase.execute(pin, masterPassword, context)
    }
}