package de.jepfa.yapm.ui.createvault

import android.os.Bundle
import android.view.MenuItem
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity

class CreateVaultActivity : BaseActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_vault)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            navigateUpTo(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val ARG_ENC_MASTER_PASSWD = "masterpasswd"
        const val ARG_ENC_PIN = "pin"
    }
}