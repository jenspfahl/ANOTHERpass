package de.jepfa.yapm.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
import de.jepfa.yapm.ui.createvault.CreateVaultActivity

open class BaseActivity : AppCompatActivity() {

    val PASSWD_STRENGTH_DEFAULT = PasswordStrength.STRONG

    fun getApp(): YapmApp {
        return application as YapmApp
    }

}