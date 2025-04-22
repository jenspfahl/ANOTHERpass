package de.jepfa.yapm.ui

import android.app.Application
import android.content.Context
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
        Lingver.init(this, locale)
        Locale.getDefault()
    }

     fun getLocale(): Locale {
        val language = PreferenceService.getAsString(PreferenceService.PREF_LANGUAGE, this)

        return if (language == null || language == "default") Locale.getDefault()
        else Locale(language)
    }
}
