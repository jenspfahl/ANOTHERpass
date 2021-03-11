package de.jepfa.yapm.ui.exportvault

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Switch
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.util.ExtPermissionChecker
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ExportVaultActivity : SecureActivity() {

    private val saveAsFile = 1
    private var SDF_D_INTERNATIONAL: DateFormat = SimpleDateFormat("yyyy-MM-dd")


    private lateinit var includeMasterKeySwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Secret.isDenied()) {
            return
        }

        setContentView(R.layout.activity_export_vault)

        includeMasterKeySwitch = findViewById(R.id.switch_include_enc_masterkey)

        findViewById<Button>(R.id.button_export_vault).setOnClickListener {
            val key = masterSecretKey
            if (key != null) {

                ExtPermissionChecker.verifyRWStoragePermissions(this) // TODO this is not enough

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "text/json"
                intent.putExtra(Intent.EXTRA_TITLE, getBackupFileName())
                startActivityForResult(Intent.createChooser(intent, "Save as"), saveAsFile)

            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun lock() {
        recreate()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val upIntent = Intent(intent)
            navigateUpTo(upIntent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == saveAsFile) {

            data?.data?.let {
                val intent = Intent(this, FileIOService::class.java)
                intent.action = FileIOService.ACTION_EXPORT_VAULT
                intent.putExtra(FileIOService.PARAM_INCLUDE_MK, includeMasterKeySwitch.isChecked)
                intent.putExtra(FileIOService.PARAM_FILE_URI, it)
                startService(intent)

                finishAffinity()

                val upIntent = Intent(this, ListCredentialsActivity::class.java)
                navigateUpTo(upIntent)

            }

        }
    }

    fun getBackupFileName(): String {
        return "anotherpassvault-${SDF_D_INTERNATIONAL.format(Date())}.json"
    }

}