package de.jepfa.yapm.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.yariksoffice.lingver.Lingver
import de.jepfa.yapm.database.YapmDatabase
import de.jepfa.yapm.database.repository.CredentialRepository
import de.jepfa.yapm.database.repository.LabelRepository
import de.jepfa.yapm.database.repository.UsernameTemplateRepository
import de.jepfa.yapm.database.repository.WebExtensionRepository
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.SecretService
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class YapmApp : Application() {

    val database by lazy { YapmDatabase.getDatabase(this) }
    val credentialRepository by lazy { CredentialRepository(database!!.credentialDao()) }
    val labelRepository by lazy { LabelRepository(database!!.labelDao()) }
    val usernameTemplateRepository by lazy { UsernameTemplateRepository(database!!.usernameTemplateDao()) }
    val webExtensionRepository by lazy { WebExtensionRepository(database!!.webExtensionDao()) }

    override fun onCreate() {
        super.onCreate()

        PreferenceService.initStorage(this.applicationContext)

        val darkMode = PreferenceService.getAsInt(PreferenceService.PREF_DARK_MODE, this.applicationContext)
        AppCompatDelegate.setDefaultNightMode(darkMode)

        // first thing after app start is to remove old transport key to get them exchanged / new generated
        SecretService.removeAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT)

        val locale = getLocale()
        Log.d("NLS", "use locale=$locale")

        Lingver.init(this, locale)
    }

     fun getLocale(): Locale {
        val language = PreferenceService.getAsString(PreferenceService.PREF_LANGUAGE, this)
         Log.d("NLS", "preferred language is $language")

        return if (language == null || language == "default") getDefaultLocale(this)
        else Locale(language)
    }

    companion object {

        val SUPPORTED_LANGUAGES = listOf("en", "de", "nl", "cn")

        fun getDefaultLocale(context: Context): Locale {
            // get default language if chosen
            var defaultValue: Locale
            val locales = context.resources.configuration.locales
            val systemLocales = ArrayList<Locale>()
            for (i in 0..locales.size()) {
                val locale = locales.get(i)
                if (locale != null && locale.country.isNotBlank()) { // to get the locales from the device settings
                    systemLocales.add(Locale(locale.language))
                }
            }


            defaultValue = systemLocales.firstOrNull { SUPPORTED_LANGUAGES.contains(it.language) }
                ?: Locale("en")

            Log.d("NLS", "defaultValue=$defaultValue, avail=$locales, system=$systemLocales")
            return defaultValue
        }
    }
}
