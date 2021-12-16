package de.jepfa.yapm.ui.importvault

import android.os.Bundle
import com.google.gson.JsonObject
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity

class ImportVaultActivity : BaseActivity() {

    lateinit var mode: String
    var jsonContent: JsonObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_vault)
        mode = intent.getStringExtra(EXTRA_MODE) ?: EXTRA_MODE_INITIAL_IMPORT
    }

    fun isOverrideMode() = mode == EXTRA_MODE_OVERRIDE_IMPORT

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_MODE_INITIAL_IMPORT = "initial_import"
        const val EXTRA_MODE_OVERRIDE_IMPORT = "override_import"
    }
}