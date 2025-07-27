package de.jepfa.yapm.ui.createvault

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.errorhandling.ErrorActivity
import de.jepfa.yapm.usecase.app.ShowInfoUseCase
import de.jepfa.yapm.util.Constants

class CreateVaultActivity : BaseActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_vault)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        inflateActionsMenu(menu, R.menu.menu_create_or_import_vault)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_login_help) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Constants.ONLINE_HELP)
            startActivity(browserIntent)
            return true
        }
        if (id == R.id.menu_about) {
            ShowInfoUseCase.execute(this)
            return true
        }
        if (id == R.id.menu_report_bug) {
            val intent = Intent(this, ErrorActivity::class.java)
            intent.putExtra(ErrorActivity.EXTRA_FROM_ERROR_CATCHER, false)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val ARG_ENC_MASTER_PASSWD = "masterpasswd"
        const val ARG_ENC_PIN = "pin"
    }
}