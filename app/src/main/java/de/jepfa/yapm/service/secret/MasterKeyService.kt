package de.jepfa.yapm.service.secret

import android.content.Context
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.encrypted.EncryptedType
import de.jepfa.yapm.model.encrypted.EncryptedType.Types.ENC_MASTER_KEY
import de.jepfa.yapm.model.encrypted.KdfConfig
import de.jepfa.yapm.model.encrypted.KeyDerivationFunction
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.SecretService.generateRandomKey
import de.jepfa.yapm.util.Constants.MASTER_KEY_BYTE_SIZE
import de.jepfa.yapm.util.sha256

object MasterKeyService {

    /**
     * Returns the Master passphrase as Secret Key which is calculated from the user's Master Pin and Master password
     * to decrypt or encrypt the actual master key.
     */
    fun getMasterPassPhraseSecretKey(
        masterPin: Password,
        masterPassword: Password,
        salt: Key,
        cipherAlgorithm: CipherAlgorithm,
        context: Context
    ): SecretKeyHolder {
        val masterPassPhrase = SecretService.conjunctPasswords(masterPin, masterPassword, salt)
        val masterPassPhraseSK = SecretService.generateSecretKeyForMasterKey(masterPassPhrase, salt, cipherAlgorithm, context)
        masterPassPhrase.clear()

        return masterPassPhraseSK
    }

    /**
     * Returns the Master Secret Key (first) which is directly used de- and encrypt vault data like credentials
     * and the hash of te master key (second) allowed to display to the user.
     */
    fun getMasterSecretKey(masterPassPhraseSK: SecretKeyHolder,
                           salt: Key,
                           storedEncMasterKey: Encrypted,
                           useLegacyGeneration: Boolean,
                           context: Context
    ): Pair<SecretKeyHolder, Key>? {
        val masterKey = decryptMasterKey(masterPassPhraseSK, storedEncMasterKey, context) ?: return null

        val masterKeyHash = Key(masterKey.data.sha256())

        val masterSK =
            if (useLegacyGeneration) {  // vaults with version 1 use this. KDF doesn't bring any security win but slowing the login process down
                SecretService.generateLegacySecretKey(masterKey, salt, masterPassPhraseSK.cipherAlgorithm, context)
            }
            else { // vaults >= version 2 use no KDF since this happened already on the master password + pin (masterPassPhraseSK)
                SecretService.generateSecretKey(masterKey, masterPassPhraseSK.cipherAlgorithm, context)
            }
        masterKey.clear()

        return Pair(masterSK, masterKeyHash)
    }

    fun generateMasterKey(context: Context?): Key {
        return generateRandomKey(MASTER_KEY_BYTE_SIZE, context)
    }

    /**
     * Returns the Master Key which is encrypted twice, first with the Android key
     * and second with the PassPhrase key.
     */
    fun decryptMasterKey(masterPassPhraseSK: SecretKeyHolder, storedEncMasterKey: Encrypted, context: Context): Key? {
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
        kdfConfig: KdfConfig,
        cipherAlgorithm: CipherAlgorithm,
        context: Context)
    : Encrypted {

        val mkSK = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MK, context)
        val masterPassphrase = SecretService.conjunctPasswords(pin, masterPasswd, salt)
        val masterPassphraseSK = SecretService.generateSecretKeyForMasterKey(masterPassphrase, salt, cipherAlgorithm, context)
        masterPassphrase.clear()

        val pdkdfIterationsAsBase64String = kdfConfig.toBase64String()
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