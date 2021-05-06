package de.jepfa.yapm.ui.importvault

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.google.gson.JsonObject
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.credential.ListCredentialsActivity

class ImportVaultActivity : BaseActivity() {

    var jsonContent: JsonObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_vault)
    }
}