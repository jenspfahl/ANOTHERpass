package de.jepfa.yapm.model.secret

import android.os.Build
import android.util.Log
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import javax.crypto.SecretKey

data class SecretKeyHolder(val secretKey: SecretKey, val cipherAlgorithm: CipherAlgorithm) {
    fun destroy() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                secretKey?.destroy()
            }
        } catch (e: Exception) {
            Log.w("SESSION", "cannot destroy mSK", e)
        }
    }
}