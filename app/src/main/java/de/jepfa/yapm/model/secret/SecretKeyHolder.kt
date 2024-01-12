package de.jepfa.yapm.model.secret

import android.content.Context
import android.os.Build
import android.util.Log
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import javax.crypto.SecretKey

data class SecretKeyHolder(
    val secretKey: SecretKey,
    val cipherAlgorithm: CipherAlgorithm,
    val androidKey: AndroidKey?,
    val context: Context,
) {
    fun destroy() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                secretKey.destroy()
            }
        } catch (e: Exception) {
            Log.w(LOG_PREFIX + "SESSION", "cannot destroy mSK")
        }
    }
}