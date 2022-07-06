package de.jepfa.yapm.usecase.secret

import android.util.Log
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.usecase.UseCaseOutput
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ChangeVaultEncryptionUseCase: InputUseCase<ChangeVaultEncryptionUseCase.Input, SecureActivity>() {

    data class Input(val loginData: LoginData,
                     val newCipherAlgorithm: CipherAlgorithm,
                     val generateNewMasterKey: Boolean)

    fun openDialog(input: Input, activity: SecureActivity, postHandler: (backgroundResult: UseCaseOutput<Unit>) -> Unit) {

        AlertDialog.Builder(activity)
            .setTitle(R.string.title_change_encryption)
            .setMessage(R.string.message_change_encryption)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                UseCaseBackgroundLauncher(this)
                    .launch(activity, input, postHandler)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

    }

     override suspend fun doExecute(input: Input, activity: SecureActivity): Boolean {
        val salt = SaltService.getSalt(activity)
        val currentCipherAlgorithm = SecretService.getCipherAlgorithm(activity)

        val masterPassphraseSK =
            checkAndGetMasterPassphraseSK(input.loginData, salt, currentCipherAlgorithm, activity)
                ?: return false

        if (currentCipherAlgorithm != input.newCipherAlgorithm || input.generateNewMasterKey) {
            val success = renewVaultEncryption(activity, masterPassphraseSK, input, salt)

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
        val masterPassphraseSK = MasterKeyService.getMasterPassPhraseSK(
            loginData.pin,
            loginData.masterPassword,
            salt,
            cipherAlgorithm
        )
        val encEncryptedMasterKey =
            PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, activity)
        if (encEncryptedMasterKey == null) {
            Log.e("VaultEnc", "master key not on device")
            return null
        }

        val masterKey = MasterKeyService.getMasterKey(masterPassphraseSK, encEncryptedMasterKey, activity)
        if (masterKey == null) {
            Log.e("VaultEnc", "cannot decrypt master key, pin wrong?")
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
                    reencryptString(credential.additionalInfo, oldMasterSK, newMasterSK),
                    reencryptString(credential.user, oldMasterSK, newMasterSK),
                    reencryptPassword(credential.password, oldMasterSK, newMasterSK),
                    if (credential.lastPassword != null) reencryptPassword(credential.lastPassword!!, oldMasterSK, newMasterSK) else null,
                    reencryptString(credential.website, oldMasterSK, newMasterSK),
                    reencryptString(credential.labels, oldMasterSK, newMasterSK),
                    credential.isObfuscated,
                    credential.isLastPasswordObfuscated,
                    credential.modifyTimestamp
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

            PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, activity)
            PreferenceService.putCurrentDate(PreferenceService.DATA_MK_MODIFIED_AT, activity)
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
            Log.e("VaultEnc", "master key not on device")
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
            MasterKeyService.getMasterKey(masterPassphraseSK, encEncryptedMasterKey, activity)
        }
        if (masterKey == null) {
            Log.e("VaultEnc", "stored master key not valid")
            return null
        }
        val newEncryptedMasterKey = MasterKeyService.encryptAndStoreMasterKey(
            masterKey,
            input.loginData.pin,
            input.loginData.masterPassword,
            salt,
            input.newCipherAlgorithm,
            activity
        )
        masterKey.clear()

        val newMasterPassphraseSK =
            MasterKeyService.getMasterPassPhraseSK(
                input.loginData.pin, input.loginData.masterPassword, salt, input.newCipherAlgorithm)

        val vaultVersion = PreferenceService.getAsInt(PreferenceService.DATA_VAULT_VERSION, activity)
        val useLegacyGeneration = vaultVersion < Constants.FAST_KEYGEN_VAULT_VERSION
        return MasterKeyService.getMasterSK(newMasterPassphraseSK, salt, newEncryptedMasterKey, useLegacyGeneration, activity)
    }

}