package de.jepfa.yapm.ui.exportvault

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.widget.SwitchCompat
import de.jepfa.yapm.R
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_INCLUDE_MASTER_KEY_IN_BACKUP_FILE
import de.jepfa.yapm.service.PreferenceService.PREF_INCLUDE_SETTINGS_IN_BACKUP_FILE
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.usecase.vault.ShareVaultUseCase
import de.jepfa.yapm.util.PermissionChecker

class ExportVaultActivity : SecureActivity() {

    private val saveAsFile = 1

    private lateinit var includeMasterKeySwitch: SwitchCompat
    private lateinit var includePrefsSwitch: SwitchCompat

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }

        setContentView(R.layout.activity_export_vault)

        includeMasterKeySwitch = findViewById(R.id.switch_include_enc_masterkey)
        includeMasterKeySwitch.isChecked = PreferenceService.getAsBool(
            PREF_INCLUDE_MASTER_KEY_IN_BACKUP_FILE, true, this)
        includeMasterKeySwitch.setOnClickListener {
            PreferenceService.putBoolean(
                PREF_INCLUDE_MASTER_KEY_IN_BACKUP_FILE, includeMasterKeySwitch.isChecked, this)
        }

        includePrefsSwitch = findViewById(R.id.switch_include_prefs)
        includePrefsSwitch.isChecked = PreferenceService.getAsBool(
            PREF_INCLUDE_SETTINGS_IN_BACKUP_FILE, true, this)
        includePrefsSwitch.setOnClickListener {
            PreferenceService.putBoolean(
                PREF_INCLUDE_SETTINGS_IN_BACKUP_FILE, includePrefsSwitch.isChecked, this)
        }

        findViewById<Button>(R.id.button_export_vault).setOnClickListener {
            masterSecretKey?.let{ key ->
                PermissionChecker.verifyRWStoragePermissions(this)
                if (PermissionChecker.hasRWStoragePermissions(this)) {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "text/json"
                    intent.putExtra(Intent.EXTRA_TITLE, ShareVaultUseCase.getBackupFileName(this))
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.save_as)), saveAsFile)
                }
            }
        }
        findViewById<Button>(R.id.button_share_vault).setOnClickListener {
            masterSecretKey?.let{ key ->
                val input = ShareVaultUseCase.Input(
                    includeMasterKeySwitch.isChecked,
                    includePrefsSwitch.isChecked
                )
                UseCaseBackgroundLauncher(ShareVaultUseCase).launch(this, input)
                { output ->
                    ShareVaultUseCase.startShareActivity(output.data, this)
                }
            }
        }
    }

    override fun lock() {
        recreate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == saveAsFile) {

            data?.data?.let {
                val intent = Intent(this, FileIOService::class.java)
                intent.action = FileIOService.ACTION_EXPORT_VAULT
                intent.putExtra(FileIOService.PARAM_INCLUDE_MK, includeMasterKeySwitch.isChecked)
                intent.putExtra(FileIOService.PARAM_INCLUDE_PREFS, includePrefsSwitch.isChecked)
                intent.putExtra(FileIOService.PARAM_FILE_URI, it)
                startService(intent)

                finish()

            }

        }
    }

}