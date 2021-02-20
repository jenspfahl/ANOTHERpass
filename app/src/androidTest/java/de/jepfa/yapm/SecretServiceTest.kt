package de.jepfa.yapm

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecretServiceTest {

    val TAG = "YAPM/SS"
    val secretService = SecretService()

    @Test
    fun bootstrap() {
        val salt = secretService.generateKey(32)
        Log.i(TAG, "salt=${salt.debugToString()}")

        val masterPin = Password("1234") // given by the user (knowledge)
        val masterPinHash = secretService.hashPassword(masterPin, salt) // stored in the app to check PIN for correctness
        Log.i(TAG, "masterPin=${masterPin.debugToString()} --> hash=${masterPinHash.debugToString()}")

        val masterPassword = Password("h9w4mlwmaf") // generated and securelly stored by the user (owning)

        // optionally store masterPassword encrypted in the app4
        val androidSecretKey = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_MK)
        val encMasterPassword = secretService.encryptPassword(androidSecretKey, masterPassword)
        Log.i(TAG, "encMasterPassword=${encMasterPassword.debugToString()}")

        val decMasterPassword = secretService.decryptPassword(androidSecretKey, encMasterPassword)
        Log.i(TAG, "decMasterPassword=${decMasterPassword.debugToString()}")
        Assert.assertArrayEquals(masterPassword.data, decMasterPassword.data)

        // generate master passphrase used for master key
        val masterPassPhrase = secretService.conjunctPasswords(masterPin, masterPassword, salt)
        Log.i(TAG, "masterPin + masterPassword=${masterPassword.debugToString()}) --> masterPassPhrase=${masterPassPhrase.debugToString()}")
        // not needed anymore
        masterPin.clear()
        masterPassword.clear()

        val masterKey = secretService.generateKey(128) // key to crypt credentials
        Log.i(TAG, "masterKey=${masterKey.debugToString()}")

        // store masterKey encrypted with masterPassPhrase --> AES
        val masterPassPhraseSK = secretService.generateSecretKey(masterPassPhrase, salt)
        Log.i(TAG, "masterPassPhraseSK=${masterPassPhraseSK.encoded.contentToString()}")

        val encMasterKey = secretService.encryptKey(masterPassPhraseSK, masterKey)
        Log.i(TAG, "encMasterKey=${encMasterKey.debugToString()}")

        val decMasterKey = secretService.decryptKey(masterPassPhraseSK, encMasterKey)
        Log.i(TAG, "decMasterKey=${decMasterKey.debugToString()}")
        Assert.assertArrayEquals(masterKey.data, decMasterKey.data)

        // Credential enc-/decryption with masterKey --> AES
        val credential = Password("9999")
        val masterKeySK = secretService.generateSecretKey(masterKey, salt)
        // not needed anymore
        masterKey.clear()

        val encCredential = secretService.encryptPassword(masterKeySK, credential)
        Log.i(TAG, "credential=${credential.debugToString()} --> encCredential=${encCredential.debugToString()}")

        val decCredential = secretService.decryptPassword(masterKeySK, encCredential)
        Log.i(TAG, "decCredential=${decCredential.debugToString()}")
        Assert.assertArrayEquals(credential.data, decCredential.data)
        
    }
}