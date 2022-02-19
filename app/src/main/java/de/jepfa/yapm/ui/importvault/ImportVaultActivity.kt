package de.jepfa.yapm.ui.importvault

import android.os.Bundle
import com.google.gson.JsonObject
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.vault.ImportVaultUseCase.parseVaultFileContent

class ImportVaultActivity : SecureActivity() {

    lateinit var mode: String
    var jsonContent: JsonObject? = null

    init {
        checkSession = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.getString("JSON")?.let {
            jsonContent = parseVaultFileContent(it, this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_vault)

        mode = intent.getStringExtra(EXTRA_MODE) ?: EXTRA_MODE_INITIAL_IMPORT
        if (isOverrideMode()) {
            checkSession = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("JSON", jsonContent.toString())
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