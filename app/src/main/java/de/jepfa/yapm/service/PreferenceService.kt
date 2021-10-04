package de.jepfa.yapm.service

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted

object PreferenceService {

    private const val PREF_PREFIX = "YAPM/pref:"
    private const val DATA_PREFIX = "YAPM/data:"
    private const val STATE_PREFIX = "YAPM/state:"
    private const val ACTION_PREFIX = "YAPM/action:"

    /**
     * If you add new preference xml files inside #initDefaults, they should be recognised as well.
     * To achieve this, count the version value up here.
     */
    private const val STATE_DEFAULT_INIT_DONE_VERSION = "DONE_VERSION_1"

    const val STATE_DEFAULT_INIT_DONE = STATE_PREFIX + "default_init_done"

    const val DATA_CIPHER_ALGORITHM = DATA_PREFIX + "cipher_algorithm"
    const val DATA_SALT = DATA_PREFIX + "aslt"
    const val DATA_ENCRYPTED_MASTER_PASSWORD = DATA_PREFIX + "mpwd"
    const val DATA_ENCRYPTED_MASTER_KEY = DATA_PREFIX + "enmk"
    const val DATA_MASTER_PASSWORD_TOKEN_KEY = DATA_PREFIX + "mpt"
    const val STATE_MASTER_PASSWD_TOKEN_COUNTER = STATE_PREFIX + "mpt_counter"

    const val DATA_VAULT_VERSION = DATA_PREFIX + "vault_version"
    const val DATA_VAULT_CREATED_AT = DATA_PREFIX + "vault_created_at"
    const val DATA_VAULT_IMPORTED_AT = DATA_PREFIX + "vault_imported_at"

    const val PREF_MAX_LOGIN_ATTEMPTS = PREF_PREFIX + "max_login_attempts"
    const val PREF_SELF_DESTRUCTION = PREF_PREFIX + "drop_vault_if_login_declined"

    const val PREF_LOCK_TIMEOUT = PREF_PREFIX + "lock_timeout"
    const val PREF_LOGOUT_TIMEOUT = PREF_PREFIX + "logout_timeout"
    const val PREF_WARN_BEFORE_COPY_TO_CB = PREF_PREFIX + "warn_copy_password"

    const val PREF_USE_PREUDO_PHRASE = PREF_PREFIX + "use_pseudo_prase_all_time"
    const val PREF_PASSWD_STRENGTH = PREF_PREFIX + "default_passwd_strength"
    const val PREF_WITH_UPPER_CASE = PREF_PREFIX + "with_upper_case"
    const val PREF_WITH_DIGITS = PREF_PREFIX + "with_digits"
    const val PREF_WITH_SPECIAL_CHARS = PREF_PREFIX + "with_special_chars"

    const val PREF_FAST_MASTERPASSWD_LOGIN_WITH_QRC = PREF_PREFIX + "fast_mp_login_with_qrc"
    const val PREF_FAST_MASTERPASSWD_LOGIN_WITH_NFC = PREF_PREFIX + "fast_mp_login_with_nfc"
    const val PREF_COLORED_PASSWORD = PREF_PREFIX + "colored_passwords"
    const val PREF_ENABLE_OVERLAY_FEATURE = PREF_PREFIX + "enable_overlay_feature"
    const val PREF_TRANSPARENT_OVERLAY = PREF_PREFIX + "transparent_overlay"
    const val PREF_PASSWD_SHOW_FORMATTED = PREF_PREFIX + "password_show_formatted"
    const val PREF_PASSWD_WORDS_ON_NL = PREF_PREFIX + "password_words_on_nl"
    const val PREF_SHOW_LABELS_IN_LIST = PREF_PREFIX + "show_labels_in_list"
    const val PREF_COLORIZE_MP_QRCODES = PREF_PREFIX + "colorize_mp_qrcodes"

    const val PREF_AUTOFILL_EVERYWHERE = PREF_PREFIX + "autofill_suggest_everywhere"
    const val PREF_AUTOFILL_EXCLUSION_LIST = PREF_PREFIX + "autofill_exclusion_list"

    const val PREF_MASK_PASSWORD = PREF_PREFIX + "mask_password"
    const val PREF_ENABLE_COPY_PASSWORD = PREF_PREFIX + "enable_copy_password"
    const val ACTION_TEST_COPY_PASSWORD = ACTION_PREFIX + "test_copy_password"

    const val STATE_LOGIN_ATTEMPTS = STATE_PREFIX + "login_attempts"

    const val PREF_SORT_BY_RECENT = PREF_PREFIX + "sort_by_recent"


    fun initDefaults(context: Context?) {
        if (context == null) return
        val defaultInitDone = getAsString(STATE_DEFAULT_INIT_DONE, context)
        if (defaultInitDone == null || !defaultInitDone.equals(STATE_DEFAULT_INIT_DONE_VERSION)) {
            PreferenceManager.setDefaultValues(context, R.xml.autofill_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.clipboard_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.general_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.login_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.overlay_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.password_generator_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.security_preferences, true)
            /*
            If you add new preference xml files here, don't forget to count up STATE_DEFAULT_INIT_DONE_VERSION
             */
            putString(STATE_DEFAULT_INIT_DONE, STATE_DEFAULT_INIT_DONE_VERSION, context)
            Log.i("PREFS", "default values set with version $STATE_DEFAULT_INIT_DONE_VERSION")
        }
    }

    fun getEncrypted(prefKey: String, context: Context?): Encrypted? {
        return get(prefKey, context)?.let { Encrypted.fromBase64String(it)}
    }

    fun getAsInt(prefKey: String, context: Context?): Int {
        val value = get(prefKey, context) ?: return 0
        return value.toInt()
    }

    fun getAsBool(prefKey: String, context: Context?): Boolean {
        if (context == null) return false
        val defaultSharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context)
        return defaultSharedPreferences
            .getBoolean(prefKey, false)
    }

    fun getAsString(prefKey: String, context: Context?): String? {
        return get(prefKey, context)
    }

    fun getAsStringSet(prefKey: String, context: Context?): Set<String>? {
        if (context == null) return null
        val defaultSharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context)
        return defaultSharedPreferences
            .getStringSet(prefKey, null)
    }

    private fun get(prefKey: String, context: Context?): String? {
        if (context == null) return null
        val defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)
        return defaultSharedPreferences
                .getString(prefKey, null)
    }

    fun isPresent(prefKey: String, context: Context?): Boolean {
        return get(prefKey, context) != null
    }

    fun putEncrypted(prefKey: String, encrypted: Encrypted, context: Context) {
        putString(prefKey, encrypted.toBase64String(), context)
    }

    fun putString(prefKey: String, value: String, context: Context) {
        val defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)

        val editor = defaultSharedPreferences.edit()
        editor.putString(prefKey, value)
        editor.apply()
    }

    fun putBoolean(prefKey: String, value: Boolean, context: Context) {
        val defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)

        val editor = defaultSharedPreferences.edit()
        editor.putBoolean(prefKey, value)
        editor.apply()
    }

    fun toggleBoolean(prefKey: String, context: Context) {
        val value = getAsBool(prefKey, context)
        putBoolean(prefKey, !value, context)
    }

    fun delete(prefKey: String, context: Context) {
        val defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)

        val editor = defaultSharedPreferences.edit()
        editor.putString(prefKey, null)
        editor.apply()
    }

}