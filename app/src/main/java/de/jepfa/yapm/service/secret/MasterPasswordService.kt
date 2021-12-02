package de.jepfa.yapm.service.secret

import android.content.Context
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.DEFAULT_CIPHER_ALGORITHM
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.encrypted.EncryptedType
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_ENCRYPTED_MASTER_PASSWORD
import de.jepfa.yapm.service.biometrix.BiometricCallback
import de.jepfa.yapm.service.biometrix.BiometricManager
import de.jepfa.yapm.service.biometrix.BiometricUtils
import de.jepfa.yapm.util.toastText
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

object MasterPasswordService {

    private const val MODE_WITH_AUTH = "wa"
    private val EMP_SALT = Key(byteArrayOf(12, 57, 33, 75, 22, -33, 1, 123, -72, -82, 42, 100, -18, 54, 92, 23, -89, -21, -1, 95, -51, 11, 4, -99))

    fun getMasterPasswordFromSession(context: Context) : Password? {
        val transSK = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, context)
        val encMasterPasswd = Session.getEncMasterPasswd() ?: return null
        return SecretService.decryptPassword(transSK, encMasterPasswd)
    }

    fun isMasterPasswordStored(context: Context) =
        PreferenceService.isPresent(DATA_ENCRYPTED_MASTER_PASSWORD, context)


    fun getMasterPasswordFromStore(context: Context,
                                   handlePasswordReceived: (masterPassword: Password) -> Unit,
                                   handleNothingReceived: () -> Unit) {
        val encStoredMasterPasswd = PreferenceService.getEncrypted(DATA_ENCRYPTED_MASTER_PASSWORD, context)

        if (encStoredMasterPasswd == null) {
            handleNothingReceived()
            return
        }

        if (encStoredMasterPasswd.type?.payload == MODE_WITH_AUTH && BiometricUtils.isBiometricsAvailable(context)) {
            decryptWithBiometrics(
                context,
                encStoredMasterPasswd,
                handlePasswordReceived,
                handleNothingReceived
            )
        }
        else {
            val key = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MP, context)
            val masterPassword = SecretService.decryptPassword(key, encStoredMasterPasswd)
            if (masterPassword.isValid()) {
                handlePasswordReceived(masterPassword)
            }
            else {
                handleNothingReceived()
            }
        }
    }


    fun storeMasterPassword(masterPassword: Password,
                            context: Context,
                            handlePasswordStored: () -> Unit,
                            handleNothingStored: () -> Unit) {
        if (BiometricUtils.isBiometricsAvailable(context)) {
            encryptWithBiometrics(
                context,
                masterPassword,
                handlePasswordStored,
                handleNothingStored
            )
        }
        else {
            val key = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MP, context)
            val encMasterPasswd = SecretService.encryptPassword(key, masterPassword)
            PreferenceService.putEncrypted(
                DATA_ENCRYPTED_MASTER_PASSWORD,
                encMasterPasswd,
                context
            )
            handlePasswordStored()
        }
    }

    fun deleteStoredMasterPassword(context: Context) {
        PreferenceService.delete(DATA_ENCRYPTED_MASTER_PASSWORD, context)
        SecretService.removeAndroidSecretKey(AndroidKey.ALIAS_KEY_MP)
        SecretService.removeAndroidSecretKey(AndroidKey.ALIAS_KEY_MP_WITH_AUTH)
    }

    /**
     * Generates a SK to encrypt the Master Password. It uses always DEFAULT_CIPHER_ALGORITHM,
     * not the selected one from the user.
     * This is a pseudo encryption for exporting EMPs, to make them unreadable without the corresponding vault
     */
    fun generateEncMasterPasswdSKForExport(context: Context): SecretKeyHolder {
        val saltAsPassword = Password(SaltService.getSalt(context))
        val empSK = SecretService.generateNormalSecretKey(saltAsPassword, EMP_SALT, DEFAULT_CIPHER_ALGORITHM)
        saltAsPassword.clear()
        return empSK
    }

    private fun encryptWithBiometrics(
        context: Context,
        masterPassword: Password,
        handlePasswordStored: () -> Unit,
        handleNothingStored: () -> Unit
    ) {
        val key = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MP_WITH_AUTH, context)
        val cipher = createEncryptCipher(key)
        BiometricManager(cipher, context).authenticate(
            context.getString(R.string.auth_to_encrypt_emp),
            object : BiometricCallback {

            override fun onAuthenticationFailed() {
                toastText(context, R.string.biometric_failed)
                handleNothingStored()
            }

            override fun onAuthenticationCancelled() {
                toastText(context, context.getString(R.string.cancelled_by_user))
                handleNothingStored()
            }

            override fun onAuthenticationSuccessful(result: Cipher?) {
                if (result != null) {

                    val encMasterPasswd = encryptMasterPassword(result, masterPassword, MODE_WITH_AUTH)

                    if (encMasterPasswd != null) {
                        PreferenceService.putEncrypted(
                            DATA_ENCRYPTED_MASTER_PASSWORD,
                            encMasterPasswd,
                            context
                        )
                        handlePasswordStored()
                    }
                    else {
                        handleNothingStored()
                    }
                } else {
                    toastText(context, R.string.biometric_failed)
                    handleNothingStored()
                }
            }

            override fun onAuthenticationError(errString: CharSequence) {
                toastText(context, errString.toString())
            }

        })
    }

    private fun decryptWithBiometrics(
        context: Context,
        encStoredMasterPasswd: Encrypted,
        handlePasswordReceived: (masterPassword: Password) -> Unit,
        handleNothingReceived: () -> Unit
    ) {
        val key = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MP_WITH_AUTH, context)
        val cipher = createDecryptCipher(key, encStoredMasterPasswd)
        BiometricManager(cipher, context).authenticate(
            context.getString(R.string.auth_to_decrypt_emp),
            object : BiometricCallback {

            override fun onAuthenticationFailed() {
                toastText(context, R.string.biometric_failed)
                handleNothingReceived()
            }

            override fun onAuthenticationCancelled() {
                toastText(context, context.getString(R.string.cancelled_by_user))
                handleNothingReceived()
            }

            override fun onAuthenticationSuccessful(result: Cipher?) {
                if (result != null) {
                    val masterPasswd = decryptMasterPassword(result, encStoredMasterPasswd)

                    masterPasswd?.let(handlePasswordReceived)
                        ?: run(handleNothingReceived)
                } else {
                    toastText(context, R.string.biometric_failed)
                    handleNothingReceived()
                }
            }

            override fun onAuthenticationError(errString: CharSequence) {
                toastText(context, errString.toString())
            }

        })
    }

    private fun createEncryptCipher(secretKeyHolder: SecretKeyHolder) : Cipher {
        val cipher = Cipher.getInstance(secretKeyHolder.cipherAlgorithm.cipherName)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeyHolder.secretKey)
        return cipher
    }

    private fun createDecryptCipher(secretKeyHolder: SecretKeyHolder, encrypted: Encrypted) : Cipher {
        val cipher = Cipher.getInstance(secretKeyHolder.cipherAlgorithm.cipherName)
        val spec = GCMParameterSpec(128, encrypted.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKeyHolder.secretKey, spec)
        return cipher
    }

    private fun encryptMasterPassword(cipher: Cipher, masterPassword: Password, mode:String): Encrypted? {
        try {
            val encryptedData = cipher.doFinal(masterPassword.data)
            return Encrypted(
                EncryptedType(EncryptedType.Types.ENC_MASTER_PASSWD, mode),
                cipher.iv,
                encryptedData,
                DEFAULT_CIPHER_ALGORITHM)
        } catch (e: Exception) {
            Log.e("EMPS", "unable to encrypt stored EMP")
            return null
        }
    }

    private fun decryptMasterPassword(cipher: Cipher, encrypted: Encrypted): Password? {
        try {
            val decrypted = cipher.doFinal(encrypted.data)
            return Password(decrypted)
        } catch (e: Exception) {
            Log.e("EMPS", "unable to decrypt stored EMP")
            return null
        }
    }

}