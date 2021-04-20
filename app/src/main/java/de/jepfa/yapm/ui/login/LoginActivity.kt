package de.jepfa.yapm.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.ui.importvault.ImportVaultActivity
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.PreferenceUtil.DATA_ENCRYPTED_MASTER_KEY
import de.jepfa.yapm.util.PreferenceUtil.PREF_MAX_LOGIN_ATTEMPTS
import de.jepfa.yapm.util.PreferenceUtil.STATE_LOGIN_ATTEMPTS


class LoginActivity : BaseActivity() {

    val DEFAULT_MAX_LOGIN_ATTEMPTS = 3
    var loginAttempts = 0

    val createVaultActivityRequestCode = 1
    val importVaultActivityRequestCode = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)

        loginAttempts = PreferenceUtil.getAsInt(STATE_LOGIN_ATTEMPTS, 0, this)

        if (PreferenceUtil.isPresent(DATA_ENCRYPTED_MASTER_KEY, this)) {
            setContentView(R.layout.activity_login)
        }
        else {
            setContentView(R.layout.activity_create_or_import_vault)
            val buttonCreateVault: Button = findViewById(R.id.button_create_vault)
            buttonCreateVault.setOnClickListener {
                val intent = Intent(this@LoginActivity, CreateVaultActivity::class.java)
                startActivityForResult(intent, importVaultActivityRequestCode)
            }
            val buttonImportVault: Button = findViewById(R.id.button_import_vault)
            buttonImportVault.setOnClickListener {
                val intent = Intent(this@LoginActivity, ImportVaultActivity::class.java)
                startActivityForResult(intent, createVaultActivityRequestCode)
            }
        }
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_login_help) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Constants.HOMEPAGE)
            startActivity(browserIntent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == createVaultActivityRequestCode && resultCode == Activity.RESULT_OK) {
            recreate()
        }
        if (requestCode == importVaultActivityRequestCode && resultCode == Activity.RESULT_OK) {
            recreate()
        }
    }

    fun handleFailedLoginAttempt() {
        loginAttempts++
        PreferenceUtil.put(STATE_LOGIN_ATTEMPTS, loginAttempts.toString(), this)
        if (loginAttempts >= getMaxLoginAttempts()) {
            Toast.makeText(baseContext, R.string.too_may_wrong_logins, Toast.LENGTH_LONG).show()
            PreferenceUtil.delete(PreferenceUtil.DATA_ENCRYPTED_MASTER_PASSWORD, baseContext)
            PreferenceUtil.delete(PreferenceUtil.DATA_MASTER_PASSWORD_TOKEN_KEY, baseContext)
            Session.logout()
            finishAffinity()
            finishAndRemoveTask()
        }
    }

    fun getLoginAttemptMessage(): String {
        return "(attempt $loginAttempts of ${getMaxLoginAttempts()})"
    }

    fun loginSuccessful() {
        loginAttempts = 0
        PreferenceUtil.delete(STATE_LOGIN_ATTEMPTS, this)
        finishAffinity()
    }

    private fun getMaxLoginAttempts(): Int {
        return PreferenceUtil.getAsInt(PREF_MAX_LOGIN_ATTEMPTS, DEFAULT_MAX_LOGIN_ATTEMPTS, this)
    }
}