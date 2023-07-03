package de.jepfa.yapm.service

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.util.Constants
import java.text.ParsePosition
import java.util.*

object PreferenceService {

    private const val PREF_PREFIX = "YAPM/pref:"
    private const val DATA_PREFIX = "YAPM/data:"
    private const val TEMP_PREFIX = "YAPM/temp:"
    private const val STATE_PREFIX = "YAPM/state:"
    private const val ACTION_PREFIX = "YAPM/action:"

    /**
     * TODO If you add new preference xml files inside #initDefaults, they should be recognised as well.
     * To achieve this, count the version value up here.
     */
    private const val STATE_DEFAULT_INIT_DONE_VERSION = "DONE_VERSION_27"

    private const val ENC_SHARED_PREFERENCES_NAME = "de.jepfa.yapm.enc-preferences"
    private const val SYSTEM_SHARED_PREFERENCES_NAME = "de.jepfa.yapm.sys-preferences"


    const val STATE_DEFAULT_INIT_DONE = STATE_PREFIX + "default_init_done"

    const val DATA_CIPHER_ALGORITHM = DATA_PREFIX + "cipher_algorithm"
    const val DATA_SALT = DATA_PREFIX + "aslt"
    const val DATA_ENCRYPTED_SEED = DATA_PREFIX + "seed"
    const val DATA_PBKDF_ITERATIONS = DATA_PREFIX + "pbkdf_iterations"
    const val DATA_ENCRYPTED_MASTER_PASSWORD = DATA_PREFIX + "mpwd"
    const val DATA_ENCRYPTED_MASTER_KEY = DATA_PREFIX + "enmk"
    const val DATA_MASTER_PASSWORD_TOKEN_KEY = DATA_PREFIX + "mpt"
    const val DATA_MASTER_PASSWORD_TOKEN_NFC_TAG_ID = DATA_PREFIX + "mpt_nfc_tag_id"

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

    const val DATA_MPT_CREATED_AT = DATA_PREFIX + "mpt_created_at"
    const val DATA_EXPIRY_DATES = DATA_PREFIX + "expiry_dates"

    const val DATA_NAV_MENU_QUICK_ACCESS_EXPANDED = DATA_PREFIX + "nav_menu_quick_access_expanded"
    const val DATA_NAV_MENU_EXPORT_EXPANDED = DATA_PREFIX + "nav_menu_export_expanded"
    const val DATA_NAV_MENU_IMPORT_EXPANDED = DATA_PREFIX + "nav_menu_import_expanded"
    const val DATA_NAV_MENU_VAULT_EXPANDED = DATA_PREFIX + "nav_menu_vault_expanded"

    const val DATA_MP_EXPORTED_AT = DATA_PREFIX + "mp_exported_at"
    const val DATA_MP_MODIFIED_AT = DATA_PREFIX + "mp_modified_at"
    const val DATA_MP_EXPORT_NOTIFICATION_SHOWED_AT = DATA_PREFIX + "mp_export_notification_showed_at"
    const val DATA_MP_EXPORT_NOTIFICATION_SHOWED_AS = DATA_PREFIX + "mp_export_notification_showed_as"
    const val PREF_SHOW_EXPORT_MP_REMINDER = PREF_PREFIX + "show_export_mp_reminder"

    const val DATA_BIOMETRIC_SMP_NOTIFICATION_SHOWED_AT = DATA_PREFIX + "biometric_smp_notification_showed_at"
    const val DATA_BIOMETRIC_SMP_NOTIFICATION_SHOWED_AS = DATA_PREFIX + "biometric_smp_notification_showed_as"
    const val PREF_SHOW_BIOMETRIC_SMP_REMINDER = PREF_PREFIX + "show_biometric_smp_reminder"

    const val DATA_REFRESH_MPT_NOTIFICATION_SHOWED_AT = DATA_PREFIX + "refresh_mpt_notification_showed_at"
    const val DATA_REFRESH_MPT_NOTIFICATION_SHOWED_AS = DATA_PREFIX + "refresh_mpt_notification_showed_as"
    const val PREF_SHOW_REFRESH_MPT_REMINDER = PREF_PREFIX + "show_refresh_mpt_reminder"

    const val DATA_EXPIRED_PASSWORDS_NOTIFICATION_SHOWED_AT = DATA_PREFIX + "expired_passwords_notification_showed_at"
    const val DATA_EXPIRED_PASSWORDS_NOTIFICATION_SHOWED_AS = DATA_PREFIX + "expired_passwords_notification_showed_as"
    const val PREF_SHOW_EXPIRED_PASSWORDS_REMINDER = PREF_PREFIX + "show_expired_passwords_reminder"
    const val PREF_EXPIRED_CREDENTIALS_NOTIFICATION_ENABLED = PREF_PREFIX + "expired_credentials_notification_enabled"

    const val PREF_SHOW_LAST_LOGIN_STATE = PREF_PREFIX + "show_last_login_state"
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
    const val PREF_USE_EXTENDED_SPECIAL_CHARS = PREF_PREFIX + "use_extended_special_chars"

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
    const val PREF_SHOW_DIVIDERS_IN_LIST = PREF_PREFIX + "show_dividers_in_list"
    const val PREF_NAV_MENU_ALWAYS_COLLAPSED = PREF_PREFIX + "nav_menu_always_collapsed"
    const val PREF_COLORIZE_MP_QRCODES = PREF_PREFIX + "colorize_mp_qrcodes"
    const val PREF_QRCODES_WITH_HEADER = PREF_PREFIX + "qrcodes_with_header"
    const val PREF_LABEL_FILTER_SINGLE_CHOICE = PREF_PREFIX + "label_filter_single_choice"

    const val PREF_AUTOFILL_EVERYWHERE = PREF_PREFIX + "autofill_suggest_everywhere"
    const val PREF_AUTOFILL_INLINE_PRESENTATIONS = PREF_PREFIX + "autofill_inline_presentations"
    const val PREF_AUTOFILL_SUGGEST_CREDENTIALS = PREF_PREFIX + "autofill_suggest_credentials"
    const val PREF_AUTOFILL_EXCLUSION_LIST = PREF_PREFIX + "autofill_exclusion_list"
    const val PREF_AUTOFILL_DEACTIVATION_DURATION = PREF_PREFIX + "autofill_deactivation_duration"

    const val PREF_MASK_PASSWORD = PREF_PREFIX + "mask_password"
    const val PREF_ENABLE_COPY_PASSWORD = PREF_PREFIX + "enable_copy_password"
    const val ACTION_TEST_COPY_PASSWORD = ACTION_PREFIX + "test_copy_password"

    const val STATE_LOGIN_ATTEMPTS = STATE_PREFIX + "login_attempts"
    const val STATE_LOGIN_DENIED_AT = STATE_PREFIX + "login_denied_at"
    const val STATE_LOGIN_SUCCEEDED_AT = STATE_PREFIX + "login_succeeded_at"

    const val STATE_PREVIOUS_LOGIN_ATTEMPTS = STATE_PREFIX + "previous_login_attempts"
    const val STATE_PREVIOUS_LOGIN_SUCCEEDED_AT = STATE_PREFIX + "previous_login_succeeded_at"

    const val PREF_CREDENTIAL_SORT_ORDER = PREF_PREFIX + "credential_sort_order"
    const val PREF_SHOW_CREDENTIAL_IDS = PREF_PREFIX + "show_credential_ids"
    const val PREF_EXPIRED_CREDENTIALS_ON_TOP = PREF_PREFIX + "expired_credentials_on_top"

    const val PREF_DARK_MODE = PREF_PREFIX + "dark_mode"
    const val PREF_LANGUAGE = PREF_PREFIX + "language"

    const val PREF_INCLUDE_MASTER_KEY_IN_BACKUP_FILE = PREF_PREFIX + "include_master_key_in_backup_file"
    const val PREF_INCLUDE_SETTINGS_IN_BACKUP_FILE = PREF_PREFIX + "include_settings_in_backup_file"

    const val DATA_USED_LABEL_FILTER = DATA_PREFIX + "used_label_filter"
    const val STATE_INTRO_SHOWED = STATE_PREFIX + "intro_showed"
    const val STATE_DISCLAIMER_SHOWED = STATE_PREFIX + "disclaimer_showed"
    const val STATE_WHATS_NEW_SHOWED_FOR_VERSION = STATE_PREFIX + "whats_new_showed_for_version"

    const val PREF_REMINDER_PERIOD = PREF_PREFIX + "reminder_period"
    const val PREF_REMINDER_DURATION = PREF_PREFIX + "reminder_duration"
    const val PREF_AUTH_SMP_WITH_BIOMETRIC = PREF_PREFIX + "auth_smp_with_biometric"

    const val STATE_REQUEST_CREDENTIAL_LIST_RELOAD = STATE_PREFIX + "request_credential_list_reload"
    const val STATE_REQUEST_CREDENTIAL_LIST_ACTIVITY_RELOAD = STATE_PREFIX + "request_credential_list_activity_reload"

    const val STATE_PAUSE_AUTOFILL = STATE_PREFIX + "pause_autofill"

    const val TEMP_BLOB_CREDENTIALS = TEMP_PREFIX + "blob_credentials"
    const val TEMP_BLOB_LABELS = TEMP_PREFIX + "blob_labels"
    const val TEMP_BLOB_SETTINGS = TEMP_PREFIX + "blob_settings"

    private lateinit var prefs: SharedPreferences

    /**
     * Must be called before this object can be used!!!
     */
    fun initStorage(context: Context) {

        initDefaultValues(context)

        prefs = getDefaultPrefs(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val encPrefs = getEncPrefs(context)
            if (encPrefs != null) {
                if (prefs.all.isNotEmpty()) {
                    try {
                        prefs.copyTo(encPrefs)
                        prefs.clear()
                    } catch (e: Exception) {
                        Log.e("PREFS", "could not migrate to enc prefs", e)
                        return
                    }
                }
                prefs = encPrefs
            }
        }
    }

    /**
     * Initializes the default values from the preferences if needed. Initializes them to the
     * default shared preferences, not the encrypted.
     */
    private fun initDefaultValues(context: Context) {

        val systemPreferences = getSystemPrefs(context) // system preferences are never migrated to enc prefs

        val defaultInitDone = systemPreferences.getString(STATE_DEFAULT_INIT_DONE, null)

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
            systemPreferences.set(STATE_DEFAULT_INIT_DONE, STATE_DEFAULT_INIT_DONE_VERSION)
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
        return prefs.getBoolean(prefKey, false)
    }

    fun getAsBool(prefKey: String, defaultValue: Boolean, context: Context?): Boolean {
        return prefs.getBoolean(prefKey, defaultValue)
    }

    fun getAsString(prefKey: String, context: Context?): String? {
        return get(prefKey, context)
    }

    fun getAsStringSet(prefKey: String, context: Context?): Set<String>? {
        return prefs.getStringSet(prefKey, null)
    }

    private fun get(prefKey: String, context: Context?): String? {
        return prefs.getString(prefKey, null)
    }

    fun isPresent(prefKey: String, context: Context?): Boolean {
        return get(prefKey, context) != null
    }

    fun putEncrypted(prefKey: String, encrypted: Encrypted, context: Context?) {
        putString(prefKey, encrypted.toBase64String(), context)
    }

    fun putCurrentDate(prefKey: String, context: Context?) {
        putDate(prefKey, Date(), context)
    }

    fun putDate(prefKey: String, date: Date, context: Context?) {
        putString(
            prefKey,
            date.time.toString(),
            context
        )
    }

    fun putInt(prefKey: String, value: Int, context: Context?) {
        prefs.edit { it.putString(prefKey, value.toString()) }
    }

    fun putString(prefKey: String, value: String, context: Context?) {
        prefs.edit { it.putString(prefKey, value) }
    }

    fun putStringSet(prefKey: String, value: Set<String>, context: Context?) {
        prefs.edit { it.putStringSet(prefKey, value) }
    }

    fun putBoolean(prefKey: String, value: Boolean, context: Context?) {
        prefs.edit { it.putBoolean(prefKey, value) }
    }

    fun toggleBoolean(prefKey: String, context: Context?) {
        val value = getAsBool(prefKey, context)
        putBoolean(prefKey, !value, context)
    }

    fun delete(prefKey: String, context: Context?) {
        prefs.remove(prefKey)
    }

    fun deleteAllData(context: Context?) {
        deleteAllStartingWith(DATA_PREFIX, context)
    }


    fun getAllStartingWith(prefix:String, context: Context?): Map<String, Any?> {
        return prefs.all.filter { (k, _) -> k.startsWith(prefix) }
    }

    fun deleteAllStartingWith(prefix: String, context: Context?) {
        getAllStartingWith(prefix, context)
            .forEach { (k, _) -> delete(k, context)}
    }

    fun deleteAllTempData(context: Context) {
        getAllStartingWith(TEMP_PREFIX, context)
            .forEach { (k, _) -> delete(k, context)}
    }

    fun getAllPrefs(context: Context): Map<String, Any?> {
        return getAllStartingWith(PREF_PREFIX, context)
    }

    private fun getDefaultPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun getSystemPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(SYSTEM_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private fun getEncPrefs(context: Context): SharedPreferences? {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                ENC_SHARED_PREFERENCES_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("PREFS", "cannot create encrypted shared preferences", e)
            null
        }
    }
}

fun SharedPreferences.copyTo(dest: SharedPreferences) {
    all.entries.forEach { entry ->
        val key = entry.key
        if (!dest.contains(key)) {
            val value = entry.value
            dest.set(key, value)
        }
    }
}

inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
    val editor = this.edit()
    operation(editor)
    editor.apply()
}

fun SharedPreferences.set(key: String, value: Any?) {
    when (value) {
        is String? -> edit { it.putString(key, value) }
        is Set<*>? -> edit { it.putStringSet(key, value?.map { it.toString() }?.toSet()) }
        is Boolean -> edit { it.putBoolean(key, value) }
        else -> {
            Log.e("PREFS", "Unsupported Type: $value")
        }
    }
}

fun SharedPreferences.clear() {
    edit() { it.clear() }
}

fun SharedPreferences.remove(key: String) {
    edit { it.remove(key) }
}