package de.jepfa.yapm.service.secret

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.secret.Password
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecretServiceTest {

    val TAG = "YAPM/SS"

    @Test
    fun bootstrap() {
        val salt = SecretService.generateRandomKey(32)
        val cipherAlgorithm = CipherAlgorithm.CHACHACHA20
        Log.i(TAG, "salt=${salt.debugToString()}")

        val masterPin = Password("1234") // given by the user (knowledge)

        val masterPassword = Password("h9w4mlwmaf") // generated and securelly stored by the user (owning)

        // optionally store masterPassword encrypted in the app4
        val androidSecretKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)
        val encMasterPassword = SecretService.encryptPassword(androidSecretKey, masterPassword)
        Log.i(TAG, "encMasterPassword=${encMasterPassword.debugToString()}")

        val decMasterPassword = SecretService.decryptPassword(androidSecretKey, encMasterPassword)
        Log.i(TAG, "decMasterPassword=${decMasterPassword.toFormattedPassword()}")
        Assert.assertArrayEquals(masterPassword.data, decMasterPassword.data)

        // generate master passphrase used for master key
        val masterPassPhrase = SecretService.conjunctPasswords(masterPin, masterPassword, salt)
        Log.i(TAG, "masterPin + masterPassword=${masterPassword.toFormattedPassword()}) --> masterPassPhrase=${masterPassPhrase.toFormattedPassword()}")
        // not needed anymore
        masterPin.clear()
        masterPassword.clear()

        val masterKey = SecretService.generateRandomKey(128) // key to crypt credentials
        Log.i(TAG, "masterKey=${masterKey.debugToString()}")

        // store masterKey encrypted with masterPassPhrase --> AES
        val masterPassPhraseSK = SecretService.generateStrongSecretKey(masterPassPhrase, salt, cipherAlgorithm)
        Log.i(TAG, "masterPassPhraseSK=${masterPassPhraseSK.secretKey.encoded.contentToString()}")

        val encMasterKey = SecretService.encryptKey(masterPassPhraseSK, masterKey)
        Log.i(TAG, "encMasterKey=${encMasterKey.debugToString()}")

        val decMasterKey = SecretService.decryptKey(masterPassPhraseSK, encMasterKey)
        Log.i(TAG, "decMasterKey=${decMasterKey.debugToString()}")
        Assert.assertArrayEquals(masterKey.data, decMasterKey.data)

        // Credential enc-/decryption with masterKey --> AES
        val credential = Password("9999")
        val masterKeySK = SecretService.generateStrongSecretKey(masterKey, salt, cipherAlgorithm)
        // not needed anymore
        masterKey.clear()

        val encCredential = SecretService.encryptPassword(masterKeySK, credential)
        Log.i(TAG, "credential=${credential.toFormattedPassword()} --> encCredential=${encCredential.debugToString()}")

        val decCredential = SecretService.decryptPassword(masterKeySK, encCredential)
        Log.i(TAG, "decCredential=${decCredential.toFormattedPassword()}")
        Assert.assertArrayEquals(credential.data, decCredential.data)
        
    }
}