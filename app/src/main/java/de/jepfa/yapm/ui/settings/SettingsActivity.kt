package de.jepfa.yapm.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import com.yariksoffice.lingver.Lingver
import de.jepfa.yapm.R
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.autofill.ResponseFiller
import de.jepfa.yapm.service.biometrix.BiometricUtils
import de.jepfa.yapm.service.net.HttpServer
import de.jepfa.yapm.service.net.HttpServer.DEFAULT_HTTP_SERVER_PORT
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.login.LoginActivity
import de.jepfa.yapm.usecase.session.LogoutUseCase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.ClipboardUtil
import java.util.*


private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity : SecureActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }

        setContentView(R.layout.settings_activity)

        val openServerSettings = intent.getBooleanExtra("OpenServerSettings", false)

        if (openServerSettings) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, ServerSettingsFragment())
                .commit()
        }
        else if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, HeaderFragment())
                .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.title_activity_settings)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun lock() {
        recreate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        navigateUpTo(intent)
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment!!
        ).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment)
            .addToBackStack(null)
            .commit()
        title = pref.title
        return true
    }

    class HeaderFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = EncryptedPreferenceDataStore.getInstance(requireContext())
            setPreferencesFromResource(R.xml.header_preferences, rootKey)
        }
    }

    class GeneralSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = EncryptedPreferenceDataStore.getInstance(requireContext())
            setPreferencesFromResource(R.xml.general_preferences, rootKey)

            val darkModePref = findPreference<ListPreference>(
                PreferenceService.PREF_DARK_MODE)

            darkModePref?.let {
                it.setOnPreferenceChangeListener { preference, newValue ->
                    val currentDarkMode = AppCompatDelegate.getDefaultNightMode()
                    val newDarkMode = newValue.toString().toInt()
                    if (currentDarkMode != newDarkMode) {
                        AppCompatDelegate.setDefaultNightMode(newDarkMode)
                    }
                    true
                }
            }

            val languagePref = findPreference<ListPreference>(
                PreferenceService.PREF_LANGUAGE)

            languagePref?.let {
                it.setOnPreferenceChangeListener { preference, newValue ->

                    val oldValue = languagePref.value

                    if (oldValue != newValue) {
                        (activity as? SettingsActivity)?.let { activity ->
                            Lingver.getInstance().setLocale(activity, Locale(newValue.toString()))

                            AlertDialog.Builder(activity)
                                .setTitle(R.string.title_change_language)
                                .setMessage(R.string.message_change_language)
                                .setNeutralButton(R.string.button_restart) { dialog, _ ->
                                    LogoutUseCase.execute(activity)
                                    val intent = Intent(activity, LoginActivity::class.java)
                                    activity.startActivity(intent)
                                }
                                .show()
                        }
                    }
                    true
                }
            }

            val showLabelsInListPref = findPreference<SwitchPreferenceCompat>(
                PreferenceService.PREF_SHOW_LABELS_IN_LIST)

            showLabelsInListPref?.let {
                it.setOnPreferenceChangeListener { preference, newValue ->
                    PreferenceService.putBoolean(PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_RELOAD, true, preference.context)
                    true
                }
            }

            val showDividersInListPref = findPreference<SwitchPreferenceCompat>(
                PreferenceService.PREF_SHOW_DIVIDERS_IN_LIST)

            showDividersInListPref?.let {
                it.setOnPreferenceChangeListener { preference, newValue ->
                    PreferenceService.putBoolean(PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_ACTIVITY_RELOAD, true, preference.context)
                    true
                }
            }
        }
    }

    class LoginSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = EncryptedPreferenceDataStore.getInstance(requireContext())
            setPreferencesFromResource(R.xml.login_preferences, rootKey)

            val qrcPref = findPreference<SwitchPreferenceCompat>(
                PreferenceService.PREF_FAST_MASTERPASSWD_LOGIN_WITH_QRC)
            val nfcPref = findPreference<SwitchPreferenceCompat>(
                PreferenceService.PREF_FAST_MASTERPASSWD_LOGIN_WITH_NFC)

            qrcPref?.let {
                it.setOnPreferenceChangeListener { preference, newValue ->
                    if (newValue == true) {
                        nfcPref?.isChecked = false
                    }
                    true
                }
            }

            nfcPref?.isEnabled = activity?.let { NfcService.isNfcAvailable(it) } == true

            nfcPref?.let {
                it.setOnPreferenceChangeListener { preference, newValue ->
                    if (newValue == true) {
                        qrcPref?.isChecked = false
                    }
                    true
                }
            }
        }
    }

    class SecuritySettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = EncryptedPreferenceDataStore.getInstance(requireContext())
            setPreferencesFromResource(R.xml.security_preferences, rootKey)

            findPreference<ListPreference>(PreferenceService.PREF_LOCK_TIMEOUT)?.let {
                it.setOnPreferenceChangeListener { preference, newValue ->
                    Session.setLockTimeout(newValue.toString().toInt())
                    true
                }
            }

            findPreference<ListPreference>(PreferenceService.PREF_LOGOUT_TIMEOUT)?.let {
                it.setOnPreferenceChangeListener { preference, newValue ->
                    Session.setLogoutTimeout(newValue.toString().toInt())
                    true
                }
            }

            findPreference<SwitchPreferenceCompat>(PreferenceService.PREF_AUTH_SMP_WITH_BIOMETRIC)?.let { pref ->
                activity?.let { pref.isEnabled = BiometricUtils.isBiometricsSupported(it) }
            }
        }
    }

    class PasswordGeneratorSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = EncryptedPreferenceDataStore.getInstance(requireContext())
            setPreferencesFromResource(R.xml.password_generator_preferences, rootKey)
        }
    }

    class OverlaySettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = EncryptedPreferenceDataStore.getInstance(requireContext())
            setPreferencesFromResource(R.xml.overlay_preferences, rootKey)

            val enableOverlayPref = findPreference<SwitchPreferenceCompat>(
                PreferenceService.PREF_ENABLE_OVERLAY_FEATURE)

            enableOverlayPref?.let {
                it.setOnPreferenceChangeListener { preference, newValue ->
                    PreferenceService.putBoolean(PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_ACTIVITY_RELOAD, true, preference.context)
                    true
                }
            }
        }
    }

    class ClipboardSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = EncryptedPreferenceDataStore.getInstance(requireContext())
            setPreferencesFromResource(R.xml.clipboard_preferences, rootKey)

            val enableCopyPasswdPref = findPreference<SwitchPreferenceCompat>(
                PreferenceService.PREF_ENABLE_COPY_PASSWORD)

            enableCopyPasswdPref?.let {
                it.setOnPreferenceChangeListener { preference, newValue ->
                    PreferenceService.putBoolean(PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_ACTIVITY_RELOAD, true, preference.context)
                    true
                }
            }

            findPreference<Preference>(PreferenceService.ACTION_TEST_COPY_PASSWORD)?.let {
                it.setOnPreferenceClickListener { preference ->
                    activity?.let { activity -> ClipboardUtil.copyTestPasteConsumer(activity.applicationContext) }
                    true
                }
            }
        }
    }

    class AutofillSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = EncryptedPreferenceDataStore.getInstance(requireContext())
            setPreferencesFromResource(R.xml.autofill_preferences, rootKey)

            findPreference<SwitchPreferenceCompat>(PreferenceService.PREF_AUTOFILL_EVERYWHERE)?.let { pref ->
                activity?.let { pref.isEnabled = ResponseFiller.isAutofillSupported() }
            }

            findPreference<SwitchPreferenceCompat>(PreferenceService.PREF_AUTOFILL_SUGGEST_CREDENTIALS)?.let { pref ->
                activity?.let { pref.isEnabled = ResponseFiller.isAutofillSupported() }
            }

            findPreference<MultiSelectListPreference>(PreferenceService.PREF_AUTOFILL_EXCLUSION_LIST)?.let { pref ->
                activity?.let { pref.isEnabled = ResponseFiller.isAutofillSupported() }
            }

            findPreference<ListPreference>(PreferenceService.PREF_AUTOFILL_DEACTIVATION_DURATION)?.let { pref ->
                activity?.let { pref.isEnabled = ResponseFiller.isAutofillSupported() }
            }

            findPreference<SwitchPreferenceCompat>(PreferenceService.PREF_AUTOFILL_INLINE_PRESENTATIONS)?.let { pref ->
                activity?.let { pref.isEnabled = ResponseFiller.isInlinePresentationSupported() }
            }

            val excludedApps = PreferenceService.getAsStringSet(
                PreferenceService.PREF_AUTOFILL_EXCLUSION_LIST, context)?: emptySet()

            val exclusionAppPref = findPreference<MultiSelectListPreference>(
                PreferenceService.PREF_AUTOFILL_EXCLUSION_LIST)
            exclusionAppPref?.let { pref ->
                val pm = requireContext().packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                packages.let {
                    val filteredPackages = it.toList()
                        .filterNotNull()
                        .filterNot { it.packageName == requireContext().packageName}
                        .filter { it.enabled }
                        .filter { isUserOrExcludedApp(it, excludedApps) }
                        .sortedBy { it.loadLabel(pm).toString().uppercase(Locale.ROOT) }

                    pref.entries = filteredPackages.map { it.loadLabel(pm) }.toTypedArray()
                    pref.entryValues = filteredPackages.map { it.packageName }.toTypedArray()

                }
            }
        }

        fun isUserOrExcludedApp(ai: ApplicationInfo, excludedApps: Set<String>): Boolean {
            if (excludedApps.contains(ai.packageName)) {
                return true // always show excluded apps, so filter them
            }
            val mask = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
            return ai.flags and mask == 0
        }
    }

    class ReminderSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = EncryptedPreferenceDataStore.getInstance(requireContext())
            setPreferencesFromResource(R.xml.reminder_preferences, rootKey)

            findPreference<SwitchPreferenceCompat>(PreferenceService.PREF_SHOW_BIOMETRIC_SMP_REMINDER)?.let { pref ->
                activity?.let { pref.isEnabled = BiometricUtils.isBiometricsSupported(it) }
            }
        }
    }

    class ServerSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = EncryptedPreferenceDataStore.getInstance(requireContext())
            setPreferencesFromResource(R.xml.server_preferences, rootKey)

            val prefServerCapabilityEnabled = findPreference<SwitchPreferenceCompat>(
                PreferenceService.PREF_SERVER_CAPABILITIES_ENABLED)

            prefServerCapabilityEnabled?.let {
                it.setOnPreferenceChangeListener { preference, newValue ->
                    if (newValue == false) {
                        HttpServer.shutdownAllAsync()
                    }
                    PreferenceService.putBoolean(PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_ACTIVITY_RELOAD, true, preference.context)
                    true
                }
            }

            findPreference<EditTextPreference>(PreferenceService.PREF_SERVER_PORT)?.let { pref ->
                pref.setOnBindEditTextListener { editText ->
                    editText.inputType = InputType.TYPE_CLASS_NUMBER
                    editText.filters = arrayOf(InputFilter.LengthFilter(5))
                }

                val port =
                    PreferenceService.getAsString(PreferenceService.PREF_SERVER_PORT, context)
                pref.text = if (port == null || port == "" || port == "0") DEFAULT_HTTP_SERVER_PORT.toString() else port.toString()

            }
        }
    }

}


class EncryptedPreferenceDataStore private constructor(val context: Context) : PreferenceDataStore() {
    companion object {

        @Volatile private var INSTANCE: EncryptedPreferenceDataStore? = null

        fun getInstance(context: Context): EncryptedPreferenceDataStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: EncryptedPreferenceDataStore(context).also { INSTANCE = it }
            }
    }


    override fun putString(key: String, value: String?) {
        if (value != null) {
            PreferenceService.putString(key, value, context)
        }
        else {
            PreferenceService.delete(key, context)
        }
    }

    override fun putStringSet(key: String, value: Set<String>?) {
        if (value != null) {
            PreferenceService.putStringSet(key, value, context)
        }
        else {
            PreferenceService.delete(key, context)
        }
    }

    override fun putInt(key: String, value: Int) {
        PreferenceService.putInt(key, value, context)
    }

    override fun putLong(key: String, value: Long) {
        PreferenceService.putString(key, value.toString(), context)
    }

    override fun putFloat(key: String, value: Float) {
        PreferenceService.putString(key, value.toString(), context)
    }

    override fun putBoolean(key: String, value: Boolean) {
        PreferenceService.putBoolean(key, value, context)
    }

    override fun getString(key: String, defValue: String?): String? {
        return PreferenceService.getAsString(key, context) ?: defValue
    }

    override fun getStringSet(key: String, defValue: Set<String>?): Set<String>? {
        return PreferenceService.getAsStringSet(key, context) ?: defValue
    }

    override fun getInt(key: String, defValue: Int): Int {
        return PreferenceService.getAsInt(key, context) // no default support!!
    }

    override fun getLong(key: String, defValue: Long): Long {
        return PreferenceService.getAsString(key, context)?.toLongOrNull() ?: defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return PreferenceService.getAsString(key, context)?.toFloatOrNull() ?: defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return PreferenceService.getAsBool(key, context)
    }
}