package de.jepfa.yapm.ui.settings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import de.jepfa.yapm.R
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.ClipboardUtil

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
        if (savedInstanceState == null) {
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
            pref.fragment
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
            setPreferencesFromResource(R.xml.header_preferences, rootKey)
        }
    }

    class GeneralSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
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
        }
    }

    class LoginSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
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
        }
    }

    class PasswordGeneratorSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.password_generator_preferences, rootKey)
        }
    }

    class OverlaySettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.overlay_preferences, rootKey)
        }
    }

    class ClipboardSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.clipboard_preferences, rootKey)

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
            setPreferencesFromResource(R.xml.autofill_preferences, rootKey)

            val exclusionAppPref = findPreference<MultiSelectListPreference>(
                PreferenceService.PREF_AUTOFILL_EXCLUSION_LIST)
            exclusionAppPref?.let { pref ->
                val pm = requireContext().packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                packages?.let {
                    val filteredPackages = it.toList()
                        .filterNotNull()
                        .filter { it.enabled }
                        .filter { isUserApp(it) }
                        .sortedBy { it.loadLabel(pm).toString() }

                    pref.entries = filteredPackages.map { it.loadLabel(pm) }.toTypedArray()
                    pref.entryValues = filteredPackages.map { it.packageName }.toTypedArray()

                }
            }
        }

        fun isUserApp(ai: ApplicationInfo): Boolean {
            val mask = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
            return ai.flags and mask == 0
        }
    }
}