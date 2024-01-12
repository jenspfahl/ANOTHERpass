package de.jepfa.yapm.ui.importvault

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.errorhandling.ErrorActivity
import de.jepfa.yapm.usecase.app.ShowInfoUseCase
import de.jepfa.yapm.usecase.vault.ImportVaultUseCase
import de.jepfa.yapm.usecase.vault.ImportVaultUseCase.parseVaultFileContent
import de.jepfa.yapm.util.Constants

class ImportVaultActivity : SecureActivity() {

    lateinit var mode: String
    var parsedVault: ImportVaultUseCase.ParsedVault? = null

    init {
        checkSession = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mode = intent.getStringExtra(EXTRA_MODE) ?: EXTRA_MODE_INITIAL_IMPORT

        savedInstanceState?.getString("JSON")?.let {
            parsedVault = parseVaultFileContent(it, this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_vault)

        if (isOverrideMode()) {
            checkSession = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("JSON", parsedVault.toString())
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (mode != EXTRA_MODE_INITIAL_IMPORT) {
            return false
        }
        inflateActionsMenu(menu, R.menu.menu_create_or_import_vault)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mode != EXTRA_MODE_INITIAL_IMPORT) {
            return false
        }

        val id = item.itemId
        if (id == R.id.action_login_help) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Constants.HOMEPAGE)
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

    override fun lock() {
        recreate()
    }

    fun isOverrideMode() = mode == EXTRA_MODE_OVERRIDE_IMPORT

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_MODE_INITIAL_IMPORT = "initial_import"
        const val EXTRA_MODE_OVERRIDE_IMPORT = "override_import"
    }
}