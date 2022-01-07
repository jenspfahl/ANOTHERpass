package de.jepfa.yapm.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.util.Constants
import java.text.ParsePosition
import java.util.*

object PreferenceService {

    private const val PREF_PREFIX = "YAPM/pref:"
    private const val DATA_PREFIX = "YAPM/data:"
    private const val STATE_PREFIX = "YAPM/state:"
    private const val ACTION_PREFIX = "YAPM/action:"

    /**
     * TODO If you add new preference xml files inside #initDefaults, they should be recognised as well.
     * To achieve this, count the version value up here.
     */
    private const val STATE_DEFAULT_INIT_DONE_VERSION = "DONE_VERSION_13"

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

    const val DATA_VAULT_EXPORTED_AT = DATA_PREFIX + "vault_exported_at"
    const val DATA_VAULT_MODIFIED_AT = DATA_PREFIX + "vault_modified_at"
    const val DATA_VAULT_EXPORT_NOTIFICATION_SHOWED_AT = DATA_PREFIX + "vault_export_notification_showed_at"
    const val DATA_VAULT_EXPORT_NOTIFICATION_SHOWED_AS = DATA_PREFIX + "vault_export_notification_showed_as"
    const val PREF_SHOW_EXPORT_VAULT_REMINDER = PREF_PREFIX + "show_export_vault_reminder"

    const val DATA_MK_EXPORTED_AT = DATA_PREFIX + "mk_exported_at"
    const val DATA_MK_MODIFIED_AT = DATA_PREFIX + "mk_modified_at"
    const val DATA_MK_EXPORT_NOTIFICATION_SHOWED_AT = DATA_PREFIX + "mk_export_notification_showed_at"
    const val DATA_MK_EXPORT_NOTIFICATION_SHOWED_AS = DATA_PREFIX + "mk_export_notification_showed_as"
    const val PREF_SHOW_EXPORT_MK_REMINDER = PREF_PREFIX + "show_export_mk_reminder"

    const val DATA_MP_EXPORTED_AT = DATA_PREFIX + "mp_exported_at"
    const val DATA_MP_MODIFIED_AT = DATA_PREFIX + "mp_modified_at"
    const val DATA_MP_EXPORT_NOTIFICATION_SHOWED_AT = DATA_PREFIX + "mp_export_notification_showed_at"
    const val DATA_MP_EXPORT_NOTIFICATION_SHOWED_AS = DATA_PREFIX + "mp_export_notification_showed_as"
    const val PREF_SHOW_EXPORT_MP_REMINDER = PREF_PREFIX + "show_export_mp_reminder"

    const val DATA_BIOMETRIC_SMP_NOTIFICATION_SHOWED_AT = DATA_PREFIX + "biometric_smp_notification_showed_at"
    const val DATA_BIOMETRIC_SMP_NOTIFICATION_SHOWED_AS = DATA_PREFIX + "biometric_smp_notification_showed_as"
    const val PREF_SHOW_BIOMETRIC_SMP_REMINDER = PREF_PREFIX + "show_biometric_smp_reminder"

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
    const val PREF_OVERLAY_SHOW_USER = PREF_PREFIX + "overlay_show_user"
    const val PREF_OVERLAY_SIZE = PREF_PREFIX + "overlay_size"
    const val PREF_OVERLAY_CLOSE_ALL = PREF_PREFIX + "overlay_close_all"

    const val PREF_PASSWD_SHOW_FORMATTED = PREF_PREFIX + "password_show_formatted"
    const val PREF_PASSWD_WORDS_ON_NL = PREF_PREFIX + "password_words_on_nl"
    const val PREF_SHOW_LABELS_IN_LIST = PREF_PREFIX + "show_labels_in_list"
    const val PREF_COLORIZE_MP_QRCODES = PREF_PREFIX + "colorize_mp_qrcodes"
    const val PREF_QRCODES_WITH_HEADER = PREF_PREFIX + "qrcodes_with_header"

    const val PREF_AUTOFILL_EVERYWHERE = PREF_PREFIX + "autofill_suggest_everywhere"
    const val PREF_AUTOFILL_EXCLUSION_LIST = PREF_PREFIX + "autofill_exclusion_list"

    const val PREF_MASK_PASSWORD = PREF_PREFIX + "mask_password"
    const val PREF_ENABLE_COPY_PASSWORD = PREF_PREFIX + "enable_copy_password"
    const val ACTION_TEST_COPY_PASSWORD = ACTION_PREFIX + "test_copy_password"

    const val STATE_LOGIN_ATTEMPTS = STATE_PREFIX + "login_attempts"

    const val PREF_CREDENTIAL_SORT_ORDER = PREF_PREFIX + "credential_sort_order"
    const val PREF_SHOW_CREDENTIAL_IDS = PREF_PREFIX + "show_credential_ids"

    const val PREF_DARK_MODE = PREF_PREFIX + "dark_mode"

    const val PREF_INCLUDE_MASTER_KEY_IN_BACKUP_FILE = PREF_PREFIX + "include_master_key_in_backup_file"
    const val PREF_INCLUDE_SETTINGS_IN_BACKUP_FILE = PREF_PREFIX + "include_settings_in_backup_file"

    const val DATA_USED_LABEL_FILTER = DATA_PREFIX + "used_label_filter"
    const val STATE_INTRO_SHOWED = STATE_PREFIX + "intro_showed"

    const val PREF_REMINDER_PERIOD = PREF_PREFIX + "reminder_period"
    const val PREF_AUTH_SMP_WITH_BIOMETRIC = PREF_PREFIX + "auth_smp_with_biometric"

    const val STATE_REQUEST_CREDENTIAL_LIST_RELOAD = STATE_PREFIX + "request_credential_list_reload"
    const val STATE_REQUEST_CREDENTIAL_LIST_ACTIVITY_RELOAD = STATE_PREFIX + "request_credential_list_activity_reload"


    fun initDefaults(context: Context?) {
        if (context == null) return
        val defaultInitDone = getAsString(STATE_DEFAULT_INIT_DONE, context)
        if (defaultInitDone == null || defaultInitDone != STATE_DEFAULT_INIT_DONE_VERSION) {
            PreferenceManager.setDefaultValues(context, R.xml.autofill_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.clipboard_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.general_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.login_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.overlay_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.password_generator_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.security_preferences, true)
            PreferenceManager.setDefaultValues(context, R.xml.reminder_preferences, true)
            /*
            If you add new preference xml files here, don't forget to count up STATE_DEFAULT_INIT_DONE_VERSION.
            Also do so when adding new prefs in existing files
             */
            putString(STATE_DEFAULT_INIT_DONE, STATE_DEFAULT_INIT_DONE_VERSION, context)
            Log.i("PREFS", "default values set with version $STATE_DEFAULT_INIT_DONE_VERSION")
        }
    }

    fun getEncrypted(prefKey: String, context: Context?): Encrypted? {
        return get(prefKey, context)?.let { Encrypted.fromBase64String(it)}
    }

    fun getAsDate(prefKey: String, context: Context): Date? {
        val timestampAsString = get(prefKey, context) ?: return null
        val timestamp = timestampAsString.toLongOrNull()
        if (timestamp != null) {
            return Date(timestamp)
        }
        else {
            val date: Date? = Constants.SDF_DT_MEDIUM.parse(timestampAsString, ParsePosition(0))
            if (date != null) {
                // migrate to the new format
                Log.i("PS", "migrate date $timestamp for key $prefKey to Long.")
                putDate(prefKey, date, context)
                return date
            }
            else {
                // cannot parse it anymore, just delete it
                Log.w("PS", "cannot parse date $timestamp for key $prefKey. Deleting it.")
                delete(prefKey, context)
                return null
            }
        }
    }

    fun getAsInt(prefKey: String, context: Context?): Int {
        val value = get(prefKey, context) ?: return 0
        return value.toInt()
    }

    fun getAsBool(prefKey: String, context: Context?): Boolean {
        if (context == null) return false
        return getDefault(context).getBoolean(prefKey, false)
    }

    fun getAsBool(prefKey: String, defaultValue: Boolean, context: Context?): Boolean {
        if (context == null) return defaultValue
        return getDefault(context).getBoolean(prefKey, defaultValue)
    }

    fun getAsString(prefKey: String, context: Context?): String? {
        return get(prefKey, context)
    }

    fun getAsStringSet(prefKey: String, context: Context?): Set<String>? {
        if (context == null) return null
        return getDefault(context).getStringSet(prefKey, null)
    }

    private fun get(prefKey: String, context: Context?): String? {
        if (context == null) return null
        return getDefault(context).getString(prefKey, null)
    }

    fun isPresent(prefKey: String, context: Context?): Boolean {
        return get(prefKey, context) != null
    }

    fun putEncrypted(prefKey: String, encrypted: Encrypted, context: Context) {
        putString(prefKey, encrypted.toBase64String(), context)
    }

    fun putCurrentDate(prefKey: String, context: Context) {
        putDate(prefKey, Date(), context)
    }

    fun putDate(prefKey: String, date: Date, context: Context) {
        putString(
            prefKey,
            date.time.toString(),
            context
        )
    }

    fun putString(prefKey: String, value: String, context: Context) {
        val editor = getDefault(context).edit()
        editor.putString(prefKey, value)
        editor.apply()
    }

    fun putStringSet(prefKey: String, value: Set<String>, context: Context) {
        val editor = getDefault(context).edit()
        editor.putStringSet(prefKey, value)
        editor.apply()
    }

    fun putBoolean(prefKey: String, value: Boolean, context: Context) {
        val editor = getDefault(context).edit()
        editor.putBoolean(prefKey, value)
        editor.apply()
    }

    fun toggleBoolean(prefKey: String, context: Context) {
        val value = getAsBool(prefKey, context)
        putBoolean(prefKey, !value, context)
    }

    fun delete(prefKey: String, context: Context) {
        val editor = getDefault(context).edit()
        editor.putString(prefKey, null)
        editor.apply()
    }

    fun deleteAllData(context: Context) {
        getDefault(context).all
            .filter { (k, _) -> k.startsWith(DATA_PREFIX) }
            .forEach { (k, _) -> delete(k, context)}
    }

    fun getAllPrefs(context: Context): Map<String, Any?> {
        return getDefault(context).all.filter { (k, _) -> k.startsWith(PREF_PREFIX) }
    }

    private fun getDefault(context: Context?): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}