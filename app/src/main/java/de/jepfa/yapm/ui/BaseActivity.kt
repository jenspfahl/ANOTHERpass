package de.jepfa.yapm.ui

import androidx.appcompat.app.AppCompatActivity
import de.jepfa.yapm.service.secretgenerator.PasswordStrength

open class BaseActivity : AppCompatActivity() {

    val PASSWD_STRENGTH_DEFAULT = PasswordStrength.STRONG

    fun getApp(): YapmApp {
        return application as YapmApp
    }
}