package de.jepfa.yapm.service.secret

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.Validable.Companion.FAILED_BYTE_ARRAY
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.Password
import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

object SecretService {

    val ALIAS_KEY_TRANSPORT = "YAPM/keyAlias:TRANS"
    val ALIAS_KEY_SALT = "YAPM/keyAlias:SALT"
    val ALIAS_KEY_MK = "YAPM/keyAlias:MK"
    val ALIAS_KEY_MP = "YAPM/keyAlias:MP"
    val ALIAS_KEY_MP_TOKEN = "YAPM/keyAlias:MPT"

    private val CIPHER_AES_GCM = "AES/GCM/NoPadding"
    private val ANDROID_KEY_STORE = "AndroidKeyStore"

    private val random = SecureRandom()
    private val androidKeyStore = KeyStore.getInstance(ANDROID_KEY_STORE)


    fun generateKey(length: Int): Key {
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Key(bytes)
    }

    fun generateSecretKey(key: Key, salt: Key): SecretKey {
        return generateSecretKey(Password(key.toCharArray()), salt)
    }

    fun generateSecretKey(password: Password, salt: Key): SecretKey {
        return generateSecretKey(password, salt, 65536)
    }

    fun generateSecretKey(password: Password, salt: Key, iterations: Int): SecretKey {
        val keySpec = PBEKeySpec(password.toCharArray(), salt.data, iterations, 128)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        try {
            return factory.generateSecret(keySpec)
        }
        finally {
            keySpec.clearPassword()
        }
    }

    fun conjunctPasswords(password1: Password, password2: Password, salt: Key): Password {
        val message = MessageDigest.getInstance("SHA-256")
        message.update(salt.data)
        message.update(password1.toByteArray()) //TODO better p1 + p2 ?
        message.update(password2.toByteArray())
        val digest = message.digest()
        val result = digest.map { it.toChar() }.toCharArray()

        return Password(result)
    }

    fun encryptKey(secretKey: SecretKey, key: Key): Encrypted {
        return encryptData("", secretKey, key.data)
    }

    fun encryptKey(type: String, secretKey: SecretKey, key: Key): Encrypted {
        return encryptData(type, secretKey, key.data)
    }

    fun decryptKey(secretKey: SecretKey, encrypted: Encrypted): Key {
        return Key(decryptData(secretKey, encrypted))
    }

    fun encryptPassword(secretKey: SecretKey, password: Password): Encrypted {
        return encryptData("", secretKey, password.toByteArray())
    }

    fun encryptPassword(type: String, secretKey: SecretKey, password: Password): Encrypted {
        return encryptData(type, secretKey, password.toByteArray())
    }

    fun decryptPassword(secretKey: SecretKey, encrypted: Encrypted): Password {
        return Password(decryptData(secretKey, encrypted))
    }

    // TODO use CharSequence instead of String
    fun encryptCommonString(secretKey: SecretKey, string: String): Encrypted {
        return encryptData("", secretKey, string.toByteArray())
    }

    fun decryptCommonString(secretKey: SecretKey, encrypted: Encrypted): String {
        return String(decryptData(secretKey, encrypted))
    }

    fun encryptEncrypted(secretKey: SecretKey, encrypted: Encrypted): Encrypted {
        return encryptData(encrypted.type, secretKey, encrypted.toBase64())
    }

    fun decryptEncrypted(secretKey: SecretKey, encrypted: Encrypted): Encrypted {
        return Encrypted.fromBase64(decryptData(secretKey, encrypted))
    }

    private fun encryptData(type: String = "", secretKey: SecretKey, data: ByteArray): Encrypted {
        val cipher: Cipher = Cipher.getInstance(CIPHER_AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        return Encrypted(type, cipher.getIV(), cipher.doFinal(data))
    }

    private fun decryptData(secretKey: SecretKey, encrypted: Encrypted): ByteArray {
        val encryptionIv = encrypted.iv
        val encryptedData = encrypted.data
        val cipher = Cipher.getInstance(CIPHER_AES_GCM)
        val spec = GCMParameterSpec(128, encryptionIv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        try {
            return cipher.doFinal(encryptedData)
        } catch (e: GeneralSecurityException) {
            Log.e("SS", "unable to decrypt")
            return FAILED_BYTE_ARRAY;
        }
    }

    fun getAndroidSecretKey(alias: String): SecretKey {
        androidKeyStore.load(null)
        val entry: KeyStore.Entry? = androidKeyStore.getEntry(alias, null)

        return (entry as? KeyStore.SecretKeyEntry)?.getSecretKey() ?: initAndroidSecretKey(alias)
    }

    private fun initAndroidSecretKey(alias: String): SecretKey {
        val keyGenerator = KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

        val spec = KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()

        keyGenerator.init(spec)

        return keyGenerator.generateKey();
    }


}