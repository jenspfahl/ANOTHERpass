package de.jepfa.yapm.ui.exportvault

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.SwitchCompat
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_INCLUDE_MASTER_KEY_IN_BACKUP_FILE
import de.jepfa.yapm.service.PreferenceService.PREF_INCLUDE_SETTINGS_IN_BACKUP_FILE
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.usecase.secret.ChangeMasterPasswordUseCase
import de.jepfa.yapm.usecase.secret.ChangeVaultEncryptionUseCase
import de.jepfa.yapm.usecase.vault.ExportPlainCredentialsUseCase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.usecase.vault.ShareVaultUseCase
import de.jepfa.yapm.util.PermissionChecker
import de.jepfa.yapm.util.toastText

class ExportPlainCredentialsActivity : SecureActivity() {

    private val saveAsFile = 1

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }

        setContentView(R.layout.activity_export_plain_credentials)

        val currentPinTextView: EditText = findViewById(R.id.current_pin)

        findViewById<Button>(R.id.button_export_plain_credentials).setOnClickListener {

            if (checkPin(currentPinTextView)) return@setOnClickListener

            masterSecretKey?.let{ key ->
                PermissionChecker.verifyRWStoragePermissions(this)
                if (PermissionChecker.hasRWStoragePermissions(this)) {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "text/json"
                    intent.putExtra(Intent.EXTRA_TITLE, ExportPlainCredentialsUseCase.getCsvFileName(this))
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.save_as)), saveAsFile)
                }
            }
        }
        findViewById<Button>(R.id.button_share_plain_credentials).setOnClickListener {

            if (checkPin(currentPinTextView)) return@setOnClickListener

            masterSecretKey?.let{ key ->
                UseCaseBackgroundLauncher(ExportPlainCredentialsUseCase).launch(this, Unit)
                { output ->
                    ExportPlainCredentialsUseCase.startShareActivity(output.data, this)
                }
            }
        }
    }

    private fun checkPin(currentPinTextView: EditText): Boolean {
        val currentPin = Password(currentPinTextView.text)

        if (currentPin.isEmpty()) {
            currentPinTextView.error = getString(R.string.pin_required)
            currentPinTextView.requestFocus()
            return true
        }

        val encEncryptedMasterKey =
            PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, this)
        if (encEncryptedMasterKey == null) {
            currentPinTextView.error = getString(R.string.something_went_wrong)
            currentPinTextView.requestFocus()
            return true
        }

        val currentMasterPassword = MasterPasswordService.getMasterPasswordFromSession(this)
        if (currentMasterPassword == null) {
            currentPinTextView.error = getString(R.string.something_went_wrong)
            currentPinTextView.requestFocus()
            return true
        }

        val cipherAlgorithm = SecretService.getCipherAlgorithm(this)
        val salt = SaltService.getSalt(this)
        val masterPassphraseSK = MasterKeyService.getMasterPassPhraseSK(
            currentPin,
            currentMasterPassword,
            salt,
            cipherAlgorithm
        )

        val masterKey =
            MasterKeyService.getMasterKey(masterPassphraseSK, encEncryptedMasterKey, this)
        if (masterKey == null) {
            currentPinTextView.error = getString(R.string.pin_wrong)
            currentPinTextView.requestFocus()
            return true
        }
        return false
    }

    override fun lock() {
        recreate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == saveAsFile) {

           data?.data?.let {
                val intent = Intent(this, FileIOService::class.java)
                intent.action = FileIOService.ACTION_EXPORT_PLAIN_CREDENTIALS
                intent.putExtra(FileIOService.PARAM_FILE_URI, it)
                startService(intent)

                finish()

                val upIntent = Intent(this, ListCredentialsActivity::class.java)
                navigateUpTo(upIntent)

            }

        }
    }

}