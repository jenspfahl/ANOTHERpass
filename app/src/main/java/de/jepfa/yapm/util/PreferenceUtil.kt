package de.jepfa.yapm.util

import android.content.Context
import androidx.preference.PreferenceManager
import de.jepfa.yapm.model.encrypted.Encrypted

object PreferenceUtil {

    private const val PREF_PREFIX = "YAPM/pref:"
    private const val DATA_PREFIX = "YAPM/data:"
    private const val STATE_PREFIX = "YAPM/state:"
    private const val ACTION_PREFIX = "YAPM/action:"

    const val DATA_SALT = DATA_PREFIX + "aslt"
    const val DATA_ENCRYPTED_MASTER_PASSWORD = DATA_PREFIX + "mpwd"
    const val DATA_ENCRYPTED_MASTER_KEY = DATA_PREFIX + "enmk"
    const val DATA_MASTER_PASSWORD_TOKEN_KEY = DATA_PREFIX + "mpt"
    const val STATE_MASTER_PASSWD_TOKEN_COUNTER = STATE_PREFIX + "mpt_counter"

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
    const val PREF_TRANSPARENT_OVERLAY = PREF_PREFIX + "transparent_overlay"
    const val PREF_PASSWD_WORDS_ON_NL = PREF_PREFIX + "password_words_on_nl"
    const val PREF_SHOW_LABELS_IN_LIST = PREF_PREFIX + "show_labels_in_list"
    const val PREF_COLORIZE_MP_QRCODES = PREF_PREFIX + "colorize_mp_qrcodes"

    const val PREF_AUTOFILL_EVERYWHERE = PREF_PREFIX + "autofill_suggest_everywhere"

    const val PREF_MASK_PASSWORD = PREF_PREFIX + "mask_password"
    const val PREF_ENABLE_COPY_PASSWORD = PREF_PREFIX + "enable_copy_password"
    const val ACTION_TEST_COPY_PASSWORD = ACTION_PREFIX + "test_copy_password"

    const val STATE_LOGIN_ATTEMPTS = STATE_PREFIX + "login_attempts"


    fun getEncrypted(prefKey: String, context: Context): Encrypted? {
        return get(prefKey, context)?.let { Encrypted.fromBase64String(it)}
    }

    fun getAsInt(prefKey: String, default: Int, context: Context): Int {
        val value = get(prefKey, context) ?: return default
        return value.toInt()
    }

    fun getAsInt(prefKey: String, context: Context): Int? {
        val value = get(prefKey, context) ?: return null
        return value.toInt()
    }

    fun getAsBool(prefKey: String, default: Boolean, context: Context): Boolean {
        val defaultSharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context)
        return defaultSharedPreferences
            .getBoolean(prefKey, default)
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

    fun putEncrypted(prefKey: String, encrypted: Encrypted, context: Context) {
        put(prefKey, encrypted.toBase64String(), context)
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