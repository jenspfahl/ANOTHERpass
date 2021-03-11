package de.jepfa.yapm.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.ui.importvault.ImportVaultActivity
import de.jepfa.yapm.util.PreferenceUtil


class LoginActivity : BaseActivity() {

    val MAX_LOGIN_ATTEMPTS = 3
    var loginAttempts = 0

    val createVaultActivityRequestCode = 1
    val importVaultActivityRequestCode = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)

        if (isMasterKeyStored(this)) {
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

            //TODO open help in browser?
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

    private fun isMasterKeyStored(activity: Activity): Boolean {
        return PreferenceUtil.get(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, activity) != null
    }

    fun handleFailedLoginAttempt() {
        loginAttempts++
        if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
            Toast.makeText(baseContext, R.string.too_may_wrong_logins, Toast.LENGTH_LONG).show()
            PreferenceUtil.delete(PreferenceUtil.PREF_ENCRYPTED_MASTER_PASSWORD, baseContext)
            Secret.logout()
            finishAffinity()
            finishAndRemoveTask()
        }
    }

    fun loginSuccessful() {
        loginAttempts = 0
        finishAffinity()
    }
}