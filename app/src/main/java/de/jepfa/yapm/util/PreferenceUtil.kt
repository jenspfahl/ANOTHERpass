package de.jepfa.yapm.util

import android.app.Activity
import android.content.Context
import android.preference.PreferenceManager
import de.jepfa.yapm.model.Encrypted

object PreferenceUtil {

    private const val PREF_PREFIX = "YAPM/pref:"

    const val PREF_SALT = PREF_PREFIX + "aslt"
    const val PREF_MASTER_PASSWORD_TOKEN_KEY = PREF_PREFIX + "mpt"
    const val PREF_ENCRYPTED_MASTER_PASSWORD = PREF_PREFIX + "mpwd"
    const val PREF_ENCRYPTED_MASTER_KEY = PREF_PREFIX + "enmk"

    fun getEncrypted(prefKey: String, context: Context): Encrypted? {
        return get(prefKey, context)?.let {Encrypted.fromBase64String(it)}
    }

    fun get(prefKey: String, context: Context): String? {
        val defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)
        return defaultSharedPreferences
                .getString(prefKey, null)
    }

    fun isPresent(prefKey: String, context: Context): Boolean {
        return get(prefKey, context) != null
    }

    fun put(prefKey: String, value: String, context: Context) {
        val defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)

        val editor = defaultSharedPreferences.edit()
        editor.putString(prefKey, value)
        editor.commit()
    }

    fun delete(prefKey: String, context: Context) {
        val defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)

        val editor = defaultSharedPreferences.edit()
        editor.putString(prefKey, null)
        editor.commit()
    }

}