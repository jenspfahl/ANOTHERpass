package de.jepfa.yapm.ui

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import de.jepfa.yapm.R
import de.jepfa.yapm.util.PreferenceUtil


class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isMasterKeyStored(this)) {
            setContentView(R.layout.activity_login)
        }
        else {
            setContentView(R.layout.activity_create_or_import_vault)
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


    private fun isMasterKeyStored(activity: Activity): Boolean {
        return PreferenceUtil.get(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, activity) != null
    }
}