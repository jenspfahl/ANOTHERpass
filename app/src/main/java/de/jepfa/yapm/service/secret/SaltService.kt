package de.jepfa.yapm.service.secret

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.Validable.Companion.FAILED_BYTE_ARRAY
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.SecretService.ALIAS_KEY_SALT
import de.jepfa.yapm.service.secret.SecretService.decryptKey
import de.jepfa.yapm.service.secret.SecretService.encryptKey
import de.jepfa.yapm.service.secret.SecretService.generateKey
import de.jepfa.yapm.service.secret.SecretService.getAndroidSecretKey
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.util.PreferenceUtil
import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

object SaltService {

    @Synchronized
    fun getSalt(context: Context): Key {
        val encSalt = PreferenceUtil.getEncrypted(PreferenceUtil.DATA_SALT, context)
        if (encSalt != null) {
            val saltKey = getAndroidSecretKey(ALIAS_KEY_SALT)
            val salt = decryptKey(saltKey, encSalt)
            return salt
        }
        else {
            return createAndStoreSalt(context)
        }
    }

    fun getSaltAsBase64String(context: Context): String {
        val salt = getSalt(context)
        return Base64.encodeToString(salt.data, Base64.DEFAULT)
    }

    fun storeSaltFromBase64String(saltAsBase64String: String, context: Context) {
        val salt = Key(Base64.decode(saltAsBase64String, 0))
        storeSalt(salt, context)
    }

    private fun storeSalt(salt: Key, context: Context) {
        val saltKey = getAndroidSecretKey(ALIAS_KEY_SALT)
        val encSalt = encryptKey(Encrypted.TYPE_ENC_SALT, saltKey, salt)
        PreferenceUtil.putEncrypted(PreferenceUtil.DATA_SALT, encSalt, context)
    }

    private fun createAndStoreSalt(context: Context): Key {
        val salt = generateKey(128)
        storeSalt(salt, context)

        return salt
    }

}