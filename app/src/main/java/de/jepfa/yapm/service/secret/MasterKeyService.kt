package de.jepfa.yapm.service.secret

import android.content.Context
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.encrypted.EncryptedType
import de.jepfa.yapm.model.encrypted.EncryptedType.Types.ENC_MASTER_KEY
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.SecretService.generateRandomKey
import de.jepfa.yapm.util.Constants.MASTER_KEY_BYTE_SIZE

object MasterKeyService {

    /**
     * Returns the Master passphrase which is calculated of the users Master Pin and his Master password
     */
    fun getMasterPassPhraseSK(
        masterPin: Password,
        masterPassword: Password,
        salt: Key,
        cipherAlgorithm: CipherAlgorithm
    ): SecretKeyHolder {
        val masterPassPhrase = SecretService.conjunctPasswords(masterPin, masterPassword, salt)
        val masterPassPhraseSK = SecretService.generateStrongSecretKey(masterPassPhrase, salt, cipherAlgorithm)
        masterPassPhrase.clear()

        return masterPassPhraseSK
    }

    /**
     * Returns the Master Secret Key which is encrypted twice, first with the Android key
     * and second with the PassPhrase key.
     */
    fun getMasterSK(masterPassPhraseSK: SecretKeyHolder,
                    salt: Key,
                    storedEncMasterKey: Encrypted,
                    useLegacyGeneration: Boolean,
                    context: Context
    ): SecretKeyHolder? {
        val masterKey = getMasterKey(masterPassPhraseSK, storedEncMasterKey, context) ?: return null
        val masterSK =
            if (useLegacyGeneration) SecretService.generateDefaultSecretKey(masterKey, salt, masterPassPhraseSK.cipherAlgorithm)
            else SecretService.createSecretKey(masterKey, masterPassPhraseSK.cipherAlgorithm)
        masterKey.clear()

        return masterSK
    }

    fun generateMasterKey(context: Context?): Key {
        return generateRandomKey(MASTER_KEY_BYTE_SIZE, context)
    }

    fun getMasterKey(masterPassPhraseSK: SecretKeyHolder, storedEncMasterKey: Encrypted, context: Context): Key? {
        val androidSK = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MK, context)

        val encMasterKey = SecretService.decryptEncrypted(androidSK, storedEncMasterKey)
        val masterKey = SecretService.decryptKey(masterPassPhraseSK, encMasterKey)
        if (!masterKey.isValid()) {
            return null
        }
        return masterKey
    }

    /**
     * Returns the stored encrypted master key
     */
    fun encryptAndStoreMasterKey(
        masterKey: Key,
        pin: Password,
        masterPasswd: Password,
        salt: Key,
        pdkdfIterations: Int,
        cipherAlgorithm: CipherAlgorithm,
        context: Context)
    : Encrypted {

        val mkSK = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MK, context)
        val masterPassphrase = SecretService.conjunctPasswords(pin, masterPasswd, salt)
        val masterPassphraseSK = SecretService.generateStrongSecretKey(masterPassphrase, salt, cipherAlgorithm)
        masterPassphrase.clear()

        val pdkdfIterationsAsBase64String = PbkdfIterationService.toBase64String(pdkdfIterations)
        val encryptedMasterKey = SecretService.encryptKey(EncryptedType(ENC_MASTER_KEY, pdkdfIterationsAsBase64String), masterPassphraseSK, masterKey)

        val encEncryptedMasterKey = SecretService.encryptEncrypted(mkSK, encryptedMasterKey)

        PreferenceService.putEncrypted(
            PreferenceService.DATA_ENCRYPTED_MASTER_KEY,
            encEncryptedMasterKey,
            context
        )

        return encEncryptedMasterKey
    }

    fun isMasterKeyStored(context: Context): Boolean {
        return PreferenceService.isPresent(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, context)
    }
}