package de.jepfa.yapm.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.util.PreferenceUtil


class LoginActivity : BaseActivity() {

    val createVaultActivityRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isMasterKeyStored(this)) {
            setContentView(R.layout.activity_login)
        }
        else {
            setContentView(R.layout.activity_create_or_import_vault)
            val buttonCreateVault: Button = findViewById(R.id.button_create_vault)
            buttonCreateVault.setOnClickListener {
                val intent = Intent(this@LoginActivity, CreateVaultActivity::class.java)
                startActivityForResult(intent, createVaultActivityRequestCode)
            }
            val buttonImportVault: Button = findViewById(R.id.button_import_vault)
            buttonImportVault.setOnClickListener {
                Toast.makeText(this@LoginActivity, "not supported yet", Toast.LENGTH_SHORT).show()
            }
        }
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_login_help -> true //TODO
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == createVaultActivityRequestCode && resultCode == Activity.RESULT_OK) {
            recreate()
        }

    }


    private fun isMasterKeyStored(activity: Activity): Boolean {
        return PreferenceUtil.get(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, activity) != null
    }
}