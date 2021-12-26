package de.jepfa.yapm.service.secret

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Log
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.Validable.Companion.FAILED_BYTE_ARRAY
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.encrypted.DEFAULT_CIPHER_ALGORITHM
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.encrypted.EncryptedType
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.biometrix.BiometricUtils
import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Service containing all base functionality for en-/decryption
 */
object SecretService {

    private val ANDROID_KEY_STORE = "AndroidKeyStore"

    private val random = SecureRandom()
    private val androidKeyStore = KeyStore.getInstance(ANDROID_KEY_STORE)

    fun getCipherAlgorithm(context: Context): CipherAlgorithm {
        val cipherAlgorithmName =
            PreferenceService.getAsString(PreferenceService.DATA_CIPHER_ALGORITHM, context)
                ?: return DEFAULT_CIPHER_ALGORITHM
        return CipherAlgorithm.valueOf(cipherAlgorithmName)
    }

    fun generateRandomKey(length: Int): Key {
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Key(bytes)
    }

    fun createSecretKey(data: Key, cipherAlgorithm: CipherAlgorithm): SecretKeyHolder {
        val sk = SecretKeySpec(data.data.copyOf(cipherAlgorithm.keyLength/8), cipherAlgorithm.secretKeyAlgorithm)
        return SecretKeyHolder(sk, cipherAlgorithm)
    }

    fun generateStrongSecretKey(data: Key, salt: Key, cipherAlgorithm: CipherAlgorithm): SecretKeyHolder {
        return generateStrongSecretKey(Password(data), salt, cipherAlgorithm)
    }

    fun generateStrongSecretKey(password: Password, salt: Key, cipherAlgorithm: CipherAlgorithm): SecretKeyHolder {
        return generatePBESecretKey(password, salt, 65536, cipherAlgorithm)
    }

    fun generateNormalSecretKey(password: Password, salt: Key, cipherAlgorithm: CipherAlgorithm): SecretKeyHolder {
        return generatePBESecretKey(password, salt, 1000, cipherAlgorithm)
    }

    private fun generatePBESecretKey(password: Password, salt: Key, iterations: Int, cipherAlgorithm: CipherAlgorithm): SecretKeyHolder {
        val keySpec = PBEKeySpec(password.toEncodedCharArray(), salt.data, iterations, cipherAlgorithm.keyLength)
        val factory = SecretKeyFactory.getInstance(cipherAlgorithm.secretKeyAlgorithm)
        try {
            return SecretKeyHolder(factory.generateSecret(keySpec), cipherAlgorithm)
        }
        finally {
            keySpec.clearPassword()
        }
    }

    fun conjunctPasswords(password1: Password, password2: Password, salt: Key): Password {
        val message = MessageDigest.getInstance("SHA-256")
        message.update(salt.data)
        message.update(password1.toByteArray())
        message.update(password2.toByteArray())
        val digest = message.digest()
        val result = digest.map { it.toChar() }.toCharArray()

        return Password(result)
    }

    fun secretKeyToKey(secretKeyHolder:  SecretKeyHolder, salt: Key) : Key {
        return fastHash(secretKeyHolder.secretKey.encoded, salt)
    }

    fun fastHash(data: ByteArray, salt: Key): Key {
        val message = MessageDigest.getInstance("SHA-256")
        message.update(salt.data)
        message.update(data)
        val digest = message.digest()

        return Key(digest)
    }

    fun encryptKey(secretKeyHolder:  SecretKeyHolder, key: Key): Encrypted {
        return encryptData(null, secretKeyHolder, key.data)
    }

    fun encryptKey(type: EncryptedType, secretKeyHolder:  SecretKeyHolder, key: Key): Encrypted {
        return encryptData(type, secretKeyHolder, key.data)
    }

    fun decryptKey(secretKeyHolder:  SecretKeyHolder, encrypted: Encrypted): Key {
        return Key(decryptData(secretKeyHolder, encrypted))
    }

    fun encryptPassword(secretKeyHolder:  SecretKeyHolder, password: Password): Encrypted {
        return encryptData(null, secretKeyHolder, password.toByteArray())
    }

    fun encryptPassword(type: EncryptedType, secretKeyHolder:  SecretKeyHolder, password: Password): Encrypted {
        return encryptData(type, secretKeyHolder, password.toByteArray())
    }

    fun decryptPassword(secretKeyHolder:  SecretKeyHolder, encrypted: Encrypted): Password {
        return Password(decryptData(secretKeyHolder, encrypted))
    }

    fun encryptCommonString(secretKeyHolder:  SecretKeyHolder, string: String): Encrypted {
        return encryptData(null, secretKeyHolder, string.toByteArray())
    }

    fun decryptCommonString(secretKeyHolder:  SecretKeyHolder, encrypted: Encrypted): String {
        return String(decryptData(secretKeyHolder, encrypted))
    }

    fun encryptEncrypted(secretKeyHolder:  SecretKeyHolder, encrypted: Encrypted): Encrypted {
        return encryptData(encrypted.type, secretKeyHolder, encrypted.toBase64())
    }

    fun decryptEncrypted(secretKeyHolder: SecretKeyHolder, encrypted: Encrypted): Encrypted {
        return Encrypted.fromBase64(decryptData(secretKeyHolder, encrypted))
    }

    fun hasStrongBoxSupport(context: Context): Boolean {
        return context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
    }

    private fun encryptData(type: EncryptedType?, secretKeyHolder: SecretKeyHolder, data: ByteArray): Encrypted {
        val cipher: Cipher = Cipher.getInstance(secretKeyHolder.cipherAlgorithm.cipherName)

        if (secretKeyHolder.cipherAlgorithm.integratedIvSupport) {
            cipher.init(Cipher.ENCRYPT_MODE, secretKeyHolder.secretKey)
        }
        else {
            val iv = ByteArray(cipher.blockSize)
            random.nextBytes(iv)
            val ivParams = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeyHolder.secretKey, ivParams)
        }

        return Encrypted(type, cipher.iv, cipher.doFinal(data), secretKeyHolder.cipherAlgorithm)
    }

    private fun decryptData(secretKeyHolder: SecretKeyHolder, encrypted: Encrypted): ByteArray {
        if (secretKeyHolder.cipherAlgorithm != encrypted.cipherAlgorithm) {
            Log.e("SS", "cipher algorithm missmatch")
            return FAILED_BYTE_ARRAY
        }

        try {
            val encryptionIv = encrypted.iv
            val encryptedData = encrypted.data
            val cipher = Cipher.getInstance(encrypted.cipherAlgorithm.cipherName)
            if (secretKeyHolder.cipherAlgorithm.gcmSupport) {
                val spec = GCMParameterSpec(128, encryptionIv)
                cipher.init(Cipher.DECRYPT_MODE, secretKeyHolder.secretKey, spec)
            }
            else {
                val ivParams = IvParameterSpec(encryptionIv)
                cipher.init(Cipher.DECRYPT_MODE, secretKeyHolder.secretKey, ivParams)
            }
            return cipher.doFinal(encryptedData)
        } catch (e: GeneralSecurityException) {
            Log.e("SS", "unable to decrypt")
            return FAILED_BYTE_ARRAY
        }
    }

    /*
    Android SK are always AES/GCM/NoPAdding with 128bit
     */
    fun getAndroidSecretKey(androidKey: AndroidKey, context: Context): SecretKeyHolder {
        androidKeyStore.load(null)
        val entry: KeyStore.Entry? = androidKeyStore.getEntry(androidKey.alias, null)

        val sk = (entry as? KeyStore.SecretKeyEntry)?.secretKey ?: initAndroidSecretKey(androidKey, context)
        return SecretKeyHolder(sk, DEFAULT_CIPHER_ALGORITHM)
    }

    fun removeAndroidSecretKey(androidKey: AndroidKey) {
        androidKeyStore.load(null)
        androidKeyStore.deleteEntry(androidKey.alias)
    }

    private fun initAndroidSecretKey(androidKey: AndroidKey, context: Context): SecretKey {
        val keyGenerator = KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)

        val spec = KeyGenParameterSpec.Builder(androidKey.alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            spec
                .setIsStrongBoxBacked(androidKey.boxed && hasStrongBoxSupport(context))
                .setUnlockedDeviceRequired(true)

            if (androidKey.requireUserAuth && BiometricUtils.isFingerprintAvailable(context)) {
                spec
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationValidityDurationSeconds(-1)
                    .setInvalidatedByBiometricEnrollment(true)

            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            keyGenerator.init(spec.build())
            return keyGenerator.generateKey()
        }
        else {
            try {
                keyGenerator.init(spec.build())
                return keyGenerator.generateKey()
            } catch (e: StrongBoxUnavailableException) {
                Log.w("SS", "Strong box not supported, falling back to without it", e)
                spec.setIsStrongBoxBacked(false)
                keyGenerator.init(spec.build())
                return keyGenerator.generateKey()
            }
        }
    }


}