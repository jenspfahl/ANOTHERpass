package de.jepfa.yapm.usecase.secret

import android.util.Log
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.*
import de.jepfa.yapm.model.kdf.KdfConfig
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.usecase.UseCaseOutput
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import de.jepfa.yapm.util.DebugInfo

object ChangeVaultEncryptionUseCase: InputUseCase<ChangeVaultEncryptionUseCase.Input, SecureActivity>() {

    data class Input(val loginData: LoginData,
                     val kdfConfig: KdfConfig,
                     val newCipherAlgorithm: CipherAlgorithm,
                     val generateNewMasterKey: Boolean)

    fun openDialog(input: Input, activity: SecureActivity, postHandler: (backgroundResult: UseCaseOutput<Unit>) -> Unit) {

        val currentCipherAlgorithm = SecretService.getCipherAlgorithm(activity)
        val currentKdfConfig = SecretService.getStoredKdfConfig(activity)

        val messageId = if (onlyNeedsToRecryptMasterSK(
                input,
                currentCipherAlgorithm,
                currentKdfConfig
            )
        ) {
            //only KDF config has been changed, no need to renew the whole vault but only the master key
            R.string.message_change_iterations
        }
        else {
            R.string.message_change_encryption
        }

        AlertDialog.Builder(activity)
            .setTitle(R.string.title_change_encryption)
            .setMessage(messageId)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                UseCaseBackgroundLauncher(this)
                    .launch(activity, input, postHandler)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

    }

    private fun onlyNeedsToRecryptMasterSK(
        input: Input,
        currentCipherAlgorithm: CipherAlgorithm,
        currentKdfConfig: KdfConfig
    ) = (currentCipherAlgorithm == input.newCipherAlgorithm
                && !input.generateNewMasterKey
                && (currentKdfConfig != input.kdfConfig))

    override suspend fun doExecute(input: Input, activity: SecureActivity): Boolean {
        val salt = SaltService.getSalt(activity)
        val currentCipherAlgorithm = SecretService.getCipherAlgorithm(activity)


        val masterPassphraseSK =
            checkAndGetMasterPassphraseSK(input.loginData, salt, currentCipherAlgorithm, activity)
                ?: return false

         val currentKdfConfig = SecretService.getStoredKdfConfig(activity)

         if (currentCipherAlgorithm != input.newCipherAlgorithm
             || input.generateNewMasterKey
             || currentKdfConfig != input.kdfConfig) {

            val success = if (onlyNeedsToRecryptMasterSK(
                    input,
                    currentCipherAlgorithm,
                    currentKdfConfig
                )) {
                //only iterations has been changed, no need to renew the whole vault but only the master key
                renewMasterSK(masterPassphraseSK, input, salt, activity) != null
            }
            else {
                renewVaultEncryption(activity, masterPassphraseSK, input, salt)
            }

            if (success) {
                return LoginUseCase.execute(input.loginData, activity).success
            }
        }

        return true
    }

    private fun checkAndGetMasterPassphraseSK(
        loginData: LoginData,
        salt: Key,
        cipherAlgorithm: CipherAlgorithm,
        activity: SecureActivity
    ): SecretKeyHolder? {
        val masterPassphraseSK = MasterKeyService.getMasterPassPhraseSecretKey(
            loginData.pin,
            loginData.masterPassword,
            salt,
            cipherAlgorithm,
            activity,
        )
        val encEncryptedMasterKey =
            PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, activity)
        if (encEncryptedMasterKey == null) {
            DebugInfo.logException("VaultEnc", "master key not on device")
            return null
        }

        val masterKey = MasterKeyService.decryptMasterKey(masterPassphraseSK, encEncryptedMasterKey, activity)
        if (masterKey == null) {
            DebugInfo.logException("VaultEnc", "cannot decrypt master key, pin wrong?")
            return null
        }
        masterKey.clear()
        return masterPassphraseSK

    }

    private suspend fun renewVaultEncryption(
        activity: SecureActivity,
        masterPassphraseSK: SecretKeyHolder,
        input: Input,
        salt: Key
    ): Boolean {

        activity.masterSecretKey?.let { oldMasterSK ->

            val app = activity.getApp()

            val newMasterSK = renewMasterSK(masterPassphraseSK, input, salt, activity) ?: return false

            app.credentialRepository.getAllSync().forEach { credential ->
                val updated = EncCredential(
                    credential.id,
                    credential.uid,
                    reencryptString(credential.name, oldMasterSK, newMasterSK),
                    reencryptString(credential.website, oldMasterSK, newMasterSK),
                    reencryptString(credential.user, oldMasterSK, newMasterSK),
                    reencryptString(credential.additionalInfo, oldMasterSK, newMasterSK),
                    reencryptString(credential.labels, oldMasterSK, newMasterSK),
                    PasswordData(
                        reencryptPassword(credential.passwordData.password, oldMasterSK, newMasterSK),
                        credential.passwordData.isObfuscated,
                        if (credential.passwordData.lastPassword != null) reencryptPassword(credential.passwordData.lastPassword!!, oldMasterSK, newMasterSK) else null,
                        credential.passwordData.isLastPasswordObfuscated,
                    ),
                    TimeData(
                        credential.timeData.modifyTimestamp,
                        reencryptString(credential.timeData.expiresAt, oldMasterSK, newMasterSK),
                    ),
                    null,
                )
                app.credentialRepository.update(updated)
            }

            app.labelRepository.getAllSync().forEach { label ->
                val updated = EncLabel(
                    label.id,
                    label.uid,
                    reencryptString(label.name, oldMasterSK, newMasterSK),
                    reencryptString(label.description, oldMasterSK, newMasterSK),
                    label.color
                )
                app.labelRepository.update(updated)
            }

            app.usernameTemplateRepository.getAllSync().forEach { usernameTemplate ->
                val updated = EncUsernameTemplate(
                    usernameTemplate.id,
                    reencryptString(usernameTemplate.username, oldMasterSK, newMasterSK),
                    reencryptString(usernameTemplate.description, oldMasterSK, newMasterSK),
                    reencryptString(usernameTemplate.generatorType, oldMasterSK, newMasterSK),
                )
                app.usernameTemplateRepository.update(updated)
            }

            app.webExtensionRepository.getAllSync().forEach { webExtension ->
                val updated = EncWebExtension(
                    webExtension.id,
                    reencryptString(webExtension.webClientId, oldMasterSK, newMasterSK),
                    reencryptString(webExtension.title, oldMasterSK, newMasterSK),
                    reencryptString(webExtension.extensionPublicKey, oldMasterSK, newMasterSK),
                    reencryptString(webExtension.sharedBaseKey, oldMasterSK, newMasterSK),
                    linked = webExtension.linked,
                    enabled = webExtension.enabled,
                    bypassIncomingRequests = webExtension.bypassIncomingRequests,
                    webExtension.lastUsedTimestamp,
                )
                app.webExtensionRepository.update(updated)
            }

            PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, activity)
            PreferenceService.putString(
                PreferenceService.DATA_CIPHER_ALGORITHM,
                input.newCipherAlgorithm.name,
                activity
            )

            return true

        }

       return false
    }

    private fun reencryptPassword(encrypted: Encrypted, oldKey: SecretKeyHolder, newKey: SecretKeyHolder): Encrypted {
        return SecretService.encryptPassword(newKey, SecretService.decryptPassword(oldKey, encrypted))
    }

    private fun reencryptString(encrypted: Encrypted, oldKey: SecretKeyHolder, newKey: SecretKeyHolder): Encrypted {
        return SecretService.encryptCommonString(newKey, SecretService.decryptCommonString(oldKey, encrypted))

    }

    private fun renewMasterSK(
        masterPassphraseSK: SecretKeyHolder,
        input: Input,
        salt: Key,
        activity: SecureActivity
    ): SecretKeyHolder? {
        val encEncryptedMasterKey =
            PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, activity)
        if (encEncryptedMasterKey == null) {
            DebugInfo.logException("VaultEnc", "master key not on device")
            return null
        }

        val masterKey = if (input.generateNewMasterKey) {
            //by generating a new master key we upgrade vault version because the master key has been changed anyway
            PreferenceService.putString(
                PreferenceService.DATA_VAULT_VERSION,
                Constants.CURRENT_VERSION.toString(),
                activity
            )
            MasterKeyService.generateMasterKey(activity)
        }
        else {
            MasterKeyService.decryptMasterKey(masterPassphraseSK, encEncryptedMasterKey, activity)
        }
        if (masterKey == null) {
            DebugInfo.logException("VaultEnc", "stored master key not valid")
            return null
        }

        input.kdfConfig.persist(activity)
        Log.d(LOG_PREFIX + "ITERATIONS", "store changed iterations=${input.kdfConfig}")

        val newEncryptedMasterKey = MasterKeyService.encryptAndStoreMasterKey(
            masterKey,
            input.loginData.pin,
            input.loginData.masterPassword,
            salt,
            input.kdfConfig,
            input.newCipherAlgorithm,
            activity
        )
        masterKey.clear()

        PreferenceService.putCurrentDate(PreferenceService.DATA_MK_MODIFIED_AT, activity)

        val newMasterPassphraseSK =
            MasterKeyService.getMasterPassPhraseSecretKey(
                input.loginData.pin, input.loginData.masterPassword, salt, input.newCipherAlgorithm, activity)

        val vaultVersion = PreferenceService.getAsInt(PreferenceService.DATA_VAULT_VERSION, activity)
        val useLegacyGeneration = vaultVersion < Constants.FAST_KEYGEN_VAULT_VERSION
        return MasterKeyService.getMasterSecretKey(newMasterPassphraseSK, salt, newEncryptedMasterKey, useLegacyGeneration, activity)?.first
    }

}