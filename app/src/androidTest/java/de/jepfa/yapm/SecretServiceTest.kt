package de.jepfa.yapm

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecretServiceTest {

    val TAG = "YAPM/SS"
    val secretService = SecretService()

    @Test
    fun bootstrap() {
        val salt = secretService.generateKey(16)
        Log.i(TAG, "salt=${salt.debugToString()}")

        val masterPin = Password("1234")
        val masterPinSecret = secretService.hashPassword(masterPin, salt)
        Log.i(TAG, "masterPin=${masterPin.debugToString()} --> secret=${masterPinSecret.debugToString()}")

        val masterPassPhrase = Password("abcdef")
        val masterSecret = secretService.conjunctPasswords(masterPin, masterPassPhrase, salt)
        Log.i(TAG, "masterPin + masterPassPhrase=${masterPassPhrase.debugToString()}) --> masterSecret=${masterSecret.debugToString()}")

        val masterKey = secretService.generateKey(128)
        Log.i(TAG, "masterKey=${masterKey.debugToString()}")

        // store masterKey encrypted with masterSecret --> AES
        val masterSecretKey = secretService.generateSecretKey(masterSecret, salt)
        Log.i(TAG, "masterSecretKey=${masterSecretKey.encoded.contentToString()}")

        val encMasterKey = secretService.encryptData(masterSecretKey, masterKey.data)
        Log.i(TAG, "encMasterKey=${encMasterKey.debugToString()}")

        val decMasterKey = secretService.decryptData(masterSecretKey, encMasterKey)
        Log.i(TAG, "decMasterKey=${decMasterKey.debugToString()}")

        // Credential enc-/decryption with masterKey --> AES
        val credential = Password("9999")
        val androidSecretKey = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_CREDENTIALS)

        val encCredential = secretService.encryptData(androidSecretKey, credential.toByteArray())
        Log.i(TAG, "credential=${credential.debugToString()} --> encCredential=${encCredential.debugToString()}")

        val decCredential = secretService.decryptData(androidSecretKey, encCredential)
        Log.i(TAG, "decCredential=${decCredential.encodeToString()}")
    }
}