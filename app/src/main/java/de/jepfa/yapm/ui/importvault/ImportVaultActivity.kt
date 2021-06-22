package de.jepfa.yapm.ui.importvault

import android.os.Bundle
import com.google.gson.JsonObject
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity

class ImportVaultActivity : BaseActivity() {

    var jsonContent: JsonObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_vault)
    }
}