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
import de.jepfa.yapm.ui.ListCredentialsActivity

class CreateVaultActivity : BaseActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_vault)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val upIntent = Intent(this, ListCredentialsActivity::class.java)
            navigateUpTo(upIntent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val ARG_ENC_PASSWD = "passwd"
        const val ARG_ENC_PIN = "pin"
    }
}