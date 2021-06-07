package de.jepfa.yapm.service.secret

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.usecase.CreateVaultUseCase
import de.jepfa.yapm.usecase.LoginUseCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.SecretKey

@RunWith(AndroidJUnit4::class)
class BruteForceTest {

    val pin = Password("123")
    val masterPassword = Password("abcd")

    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        CreateVaultUseCase.execute(pin, masterPassword, false, context)
    }

    @Test
    fun attack() {
        val salt = SaltService.getSalt(context)
        val encMasterKey = PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, context)!!

        for (i in 0 until 1000) {
            val testPin = Password(i.toString())
            val masterKeySK = crack(salt, testPin, masterPassword, encMasterKey)
            val success = masterKeySK != null
            if (success || i % 100 == 0) {
                Log.i("test", "pin=$testPin masterKeySK=$masterKeySK")
                break
            }
        }
    }

    private fun crack(salt: Key, pin: Password, masterPassword: Password, encMasterKey: Encrypted): SecretKey? {
        val masterPassPhraseSK = MasterKeyService.getMasterPassPhraseSK(pin, masterPassword, salt)
        return MasterKeyService.getMasterSK(masterPassPhraseSK, salt, encMasterKey)
    }
}