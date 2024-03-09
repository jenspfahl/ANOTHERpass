package de.jepfa.yapm.service.secret

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.security.keystore.*
import android.util.Log
import de.jepfa.yapm.model.Validable.Companion.FAILED_BYTE_ARRAY
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.encrypted.DEFAULT_CIPHER_ALGORITHM
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.encrypted.EncryptedType
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_ENCRYPTED_SEED
import de.jepfa.yapm.service.biometrix.BiometricUtils
import de.jepfa.yapm.service.secret.PbkdfIterationService.getStoredPbkdfIterations
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import java.security.*
import java.security.spec.InvalidKeySpecException
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

    /**
     * See https://github.com/jenspfahl/ANOTHERpass/issues/52
     */
    class KeyStoreNotReadyException : Exception()

    private val ANDROID_KEY_STORE = "AndroidKeyStore"

    private var userSeed: Key? = null
    private var userSeedUsed: Boolean = false
    private var random: SecureRandom? = null
    private val androidKeyStore = KeyStore.getInstance(ANDROID_KEY_STORE)

    @Synchronized
    fun getSecureRandom(context: Context?): SecureRandom {
        if (random == null || random!!.nextInt(100) <= 0) {
            Log.d(LOG_PREFIX + "SEED", "init PRNG")
            random = SecureRandom()
        }
        else {
            Log.d(LOG_PREFIX + "SEED", "return current PRNG")
        }
        loadUserSeed(context)
        if (!userSeedUsed) {
            userSeed?.let { seed ->
                Log.d(LOG_PREFIX + "SEED", "add user seed to PRNG")
                random?.setSeed(seed.data)
                userSeedUsed = true
            }
        }
        return random!!
    }

    fun clear() {
        userSeed?.clear()
        userSeed = null
        userSeedUsed = false
        random = null
    }

    private fun loadUserSeed(context: Context?) {
        if (context != null && userSeed == null) {
            Session.getMasterKeySK()?.let { key ->
                PreferenceService.getEncrypted(DATA_ENCRYPTED_SEED, context)?.let { encSeed ->
                    userSeed = decryptKey(key, encSeed)
                    userSeedUsed = false
                }
            }
        }
    }

    fun getCipherAlgorithm(context: Context): CipherAlgorithm {
        val cipherAlgorithmName =
            PreferenceService.getAsString(PreferenceService.DATA_CIPHER_ALGORITHM, context)
                ?: return DEFAULT_CIPHER_ALGORITHM
        return CipherAlgorithm.valueOf(cipherAlgorithmName)
    }

    fun generateRandomKey(length: Int, context: Context?): Key {
        val bytes = ByteArray(length)
        getSecureRandom(context).nextBytes(bytes)
        return Key(bytes)
    }

    fun createSecretKey(data: Key, cipherAlgorithm: CipherAlgorithm, context: Context): SecretKeyHolder {
        val sk = SecretKeySpec(data.data.copyOf(cipherAlgorithm.keyLength/8), cipherAlgorithm.secretKeyAlgorithm)
        return SecretKeyHolder(sk, cipherAlgorithm, null, context)
    }

    fun generateDefaultSecretKey(data: Key, salt: Key, cipherAlgorithm: CipherAlgorithm, context: Context): SecretKeyHolder {
        return generatePBESecretKey(Password(data), salt, PbkdfIterationService.LEGACY_PBKDF_ITERATIONS, cipherAlgorithm, context)
    }

    fun generateStrongSecretKey(password: Password, salt: Key, cipherAlgorithm: CipherAlgorithm, context: Context): SecretKeyHolder {
        return generatePBESecretKey(password, salt, getStoredPbkdfIterations(), cipherAlgorithm, context)
    }

    fun generateNormalSecretKey(password: Password, salt: Key, cipherAlgorithm: CipherAlgorithm, context: Context): SecretKeyHolder {
        return generatePBESecretKey(password, salt, 1000, cipherAlgorithm, context)
    }

    fun generatePBESecretKey(password: Password, salt: Key, iterations: Int, cipherAlgorithm: CipherAlgorithm, context: Context): SecretKeyHolder {
        val keySpec = PBEKeySpec(password.toEncodedCharArray(), salt.data, iterations, cipherAlgorithm.keyLength)
        val factory = SecretKeyFactory.getInstance(cipherAlgorithm.secretKeyAlgorithm)
        try {
            return SecretKeyHolder(factory.generateSecret(keySpec), cipherAlgorithm, null, context)
        }
        finally {
            keySpec.clearPassword()
        }
    }

    fun generateAesKey(key: Key, context: Context): SecretKeyHolder {
        val keySpec = SecretKeySpec(key.data, "AES")
        val factory = SecretKeyFactory.getInstance(CipherAlgorithm.AES_128.secretKeyAlgorithm) //TODO doesn't work
        try {
            return SecretKeyHolder(
                factory.generateSecret(keySpec),
                CipherAlgorithm.AES_128,
                null,
                context
            )
        } finally {
            try {
                keySpec.destroy()
            } catch (e: Exception) {
                // ignore
            }
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

    fun encryptLong(secretKeyHolder:  SecretKeyHolder, long: Long): Encrypted {
        return encryptData(null, secretKeyHolder, long.toString().toByteArray())
    }

    fun decryptLong(secretKeyHolder:  SecretKeyHolder, encrypted: Encrypted): Long? {
        if (encrypted.isEmpty()) {
            return null
        }
        val longAsString = String(decryptData(secretKeyHolder, encrypted))
        return longAsString.toLongOrNull()
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
        return try {
            encrypt(secretKeyHolder, type, data)
        } catch (e: KeyStoreNotReadyException) {
            Log.e(LOG_PREFIX + "SS", "KeyStore not ready, trying again", e)
            SystemClock.sleep(3000) // artificial wait before retry
            return try {
                encrypt(secretKeyHolder, type, data)
            } catch (e: KeyStoreNotReadyException) {
                Log.e(LOG_PREFIX + "SS", "KeyStore still not ready, trying again", e)
                SystemClock.sleep(5000) // artificial wait before last retry
                encrypt(secretKeyHolder, type, data)
            }
        }
    }

    private fun encrypt(
        secretKeyHolder: SecretKeyHolder,
        type: EncryptedType?,
        data: ByteArray,
    ): Encrypted {
        val cipher: Cipher = Cipher.getInstance(secretKeyHolder.cipherAlgorithm.cipherName)
        try {
            if (secretKeyHolder.cipherAlgorithm.integratedIvSupport) {
                cipher.init(Cipher.ENCRYPT_MODE, secretKeyHolder.secretKey)
            } else {
                val iv = ByteArray(cipher.blockSize)
                getSecureRandom(null).nextBytes(iv)
                val ivParams = IvParameterSpec(iv)
                cipher.init(Cipher.ENCRYPT_MODE, secretKeyHolder.secretKey, ivParams)
            }
        } catch (e: UserNotAuthenticatedException) {
            if (secretKeyHolder.androidKey?.requireUserAuth == false || !checkKeyRequiresUserAuthOnInsecureDevice(secretKeyHolder, secretKeyHolder.context)) {
                /*
                Seems to be a bug in KeyStoreCryptoOperationUtils.getInvalidKeyException.
                See https://github.com/jenspfahl/ANOTHERpass/issues/61
                This exception might be a wrong exception indicating something else. Here the code of getInvalidKeyException:

                case ResponseCode.LOCKED:
                case ResponseCode.UNINITIALIZED:
                case KeymasterDefs.KM_ERROR_KEY_USER_NOT_AUTHENTICATED:
                    // TODO b/173111727 remove response codes LOCKED and UNINITIALIZED
                    return new UserNotAuthenticatedException();

                The comment indicates that the reason could also be a LOCKED or UNINITIALIZED
                 */
                Log.w(LOG_PREFIX + "SS", "UserNotAuthenticatedException caught but not handled", e)
                throw KeyStoreNotReadyException()
            }
            else {
                // it seems correct to forward UserNotAuthenticatedException
                Log.w(LOG_PREFIX + "SS", "UserNotAuthenticatedException caught and forwarded", e)
                throw e
            }
        }

        return Encrypted(type, cipher.iv, cipher.doFinal(data), secretKeyHolder.cipherAlgorithm)
    }

    private fun decryptData(secretKeyHolder: SecretKeyHolder, encrypted: Encrypted): ByteArray {
        if (encrypted.isEmpty()) {
            Log.e(LOG_PREFIX + "SS", "empty encrypted")
            return FAILED_BYTE_ARRAY
        }
        if (secretKeyHolder.cipherAlgorithm != encrypted.cipherAlgorithm) {
            Log.e(LOG_PREFIX + "SS", "cipher algorithm mismatch")
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
            Log.e(LOG_PREFIX + "SS", "unable to decrypt")
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

        return SecretKeyHolder(sk, DEFAULT_CIPHER_ALGORITHM, androidKey, context)
    }

    fun checkKeyRequiresUserAuthOnInsecureDevice(secretKeyHolder: SecretKeyHolder, context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val deviceRequiresUserAuth = keyguardManager.isDeviceSecure
        val keyInfo = getKeyInfo(secretKeyHolder)

        return keyInfo?.isUserAuthenticationRequired?:false && !deviceRequiresUserAuth
    }

    private fun getKeyInfo(secretKeyHolder: SecretKeyHolder): KeyInfo? {
        val factory = SecretKeyFactory.getInstance(secretKeyHolder.cipherAlgorithm.secretKeyAlgorithm)
        try {
            return factory.getKeySpec(secretKeyHolder.secretKey, KeyInfo::class.java) as KeyInfo
        } catch (e: InvalidKeySpecException) {
            Log.e(LOG_PREFIX + "SS", "Asking for invalid key spec: ${secretKeyHolder.cipherAlgorithm}", e)
        }
        return null
    }

    fun removeAndroidSecretKey(androidKey: AndroidKey) {
        androidKeyStore.load(null)
        try {
            androidKeyStore.deleteEntry(androidKey.alias)
        } catch (e: Exception) {
            // do nothing
        }
    }

    private fun initAndroidSecretKey(androidKey: AndroidKey, context: Context): SecretKey {

        val spec = KeyGenParameterSpec.Builder(androidKey.alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            spec
                .setIsStrongBoxBacked(androidKey.boxed && hasStrongBoxSupport(context))
                .setUnlockedDeviceRequired(true)
                .setUserAuthenticationRequired(false)

            if (androidKey.requireUserAuth && BiometricUtils.isBiometricsAvailable(context)) {
                val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                val deviceRequiresUserAuth = keyguardManager.isDeviceSecure
                spec
                    .setUserAuthenticationRequired(deviceRequiresUserAuth)
                    .setInvalidatedByBiometricEnrollment(true)

            }
        }

        val keyGenerator = KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return initSecretKey(keyGenerator, spec.build())
        }
        else {
            try {
                keyGenerator.init(spec.build())
                return keyGenerator.generateKey()
            } catch (e: StrongBoxUnavailableException) {
                Log.w(LOG_PREFIX + "SS", "Strong box not supported, falling back to without it", e)
                spec.setIsStrongBoxBacked(false)
                return initSecretKey(keyGenerator, spec.build())
            }
            catch (e: Exception) {
                Log.w(LOG_PREFIX + "SS", "Unknown exception, just retry", e)
                return initSecretKey(keyGenerator, spec.build())
            }
        }
    }

    private fun initSecretKey(keyGenerator: KeyGenerator, spec: KeyGenParameterSpec): SecretKey {

        return try {
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            Log.w(LOG_PREFIX + "SS", "Unknown exception, just retry", e)
            SystemClock.sleep(100) // artificial wait before retry
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    fun setUserSeed(seed: Key?, context: Context) {
        userSeed = seed
        userSeedUsed = false
        Log.d(LOG_PREFIX + "SEED", "update user seed")
        persistUserSeed(context)
    }

    fun persistUserSeed(context: Context) {
        userSeed?.let { seed ->
            Session.getMasterKeySK()?.let { key ->
                val encSeed = encryptKey(key, seed)
                PreferenceService.putEncrypted(DATA_ENCRYPTED_SEED, encSeed, context)
                Log.d(LOG_PREFIX + "SEED", "persist user seed")
            }
        }
    }


}