package de.jepfa.yapm.ui.createvault

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.credential.ListCredentialsActivity

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