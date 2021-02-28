package de.jepfa.yapm.util

import android.app.Activity
import android.preference.PreferenceManager

object PreferenceUtil {

    private const val PREF_PREFIX = "YAPM/pref:"

    const val PREF_SALT = PREF_PREFIX + "aslt"
    const val PREF_HASHED_MASTER_PIN = PREF_PREFIX + "hmpn"
    const val PREF_ENCRYPTED_MASTER_PASSWORD = PREF_PREFIX + "mpwd"
    const val PREF_ENCRYPTED_MASTER_KEY = PREF_PREFIX + "enmk"

    fun get(prefKey: String, activity: Activity): String? {
            val context = activity.applicationContext
        val defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)
        return defaultSharedPreferences
                .getString(prefKey, null)
    }

    fun put(prefKey: String, value: String, activity: Activity) {
        val context = activity.applicationContext
        val defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)

        val editor = defaultSharedPreferences.edit()
        editor.putString(prefKey, value)
        editor.commit()
    }

    fun delete(prefKey: String, activity: Activity) {
        val context = activity.applicationContext
        val defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)

        val editor = defaultSharedPreferences.edit()
        editor.putString(prefKey, null)
        editor.commit()
    }

}