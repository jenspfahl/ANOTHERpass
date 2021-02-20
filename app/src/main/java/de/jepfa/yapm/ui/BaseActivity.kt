package de.jepfa.yapm.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.service.secretgenerator.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.PassphraseGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
import de.jepfa.yapm.ui.MainActivity as MainActivity1

open class BaseActivity : AppCompatActivity() {

    val PASSWD_STRENGTH_DEFAULT = PasswordStrength.STRONG

    fun getApp(): YapmApp {
        return application as YapmApp
    }
}