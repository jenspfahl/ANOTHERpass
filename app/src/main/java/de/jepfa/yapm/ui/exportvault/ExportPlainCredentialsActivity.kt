package de.jepfa.yapm.ui.exportvault

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.DEFAULT_OBFUSCATIONABLE_SPECIAL_CHARS
import de.jepfa.yapm.ui.ChangeKeyboardForPinManager
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.credential.DeobfuscationDialog
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.ui.credential.PasswordDialog
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.usecase.vault.ExportPlainCredentialsUseCase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.PermissionChecker
import de.jepfa.yapm.util.putEncryptedExtra

class ExportPlainCredentialsActivity : SecureActivity() {

    private var keepassMasterPassword: Password? = null
    private val saveAsFile = 1

    private lateinit var radioFileFormat: RadioGroup

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

        val pinImeiManager = ChangeKeyboardForPinManager(this, listOf(currentPinTextView))
        pinImeiManager.create(findViewById(R.id.imageview_change_imei))


        radioFileFormat = findViewById(R.id.radio_format)

        radioFileFormat.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.radio_format_csv -> {
                    findViewById<Button>(R.id.button_export_plain_credentials)?.text = getString(R.string.button_export_plain_credentials)
                    findViewById<Button>(R.id.button_share_plain_credentials)?.text = getString(R.string.button_share_plain_credentials)
                }
                R.id.radio_format_kdbx -> {
                    findViewById<Button>(R.id.button_export_plain_credentials)?.text = getString(R.string.button_export_credentials)
                    findViewById<Button>(R.id.button_share_plain_credentials)?.text = getString(R.string.button_share_credentials)
                }
            }
        }

        findViewById<Button>(R.id.button_export_plain_credentials).setOnClickListener {

            Log.d("XXX", "AAAA")
            if (checkPin(currentPinTextView)) { //TODO do checkPin in Bg and show prBar
                return@setOnClickListener
            }
            Log.d("XXX", "BBB checkPin is soo slow and not in bg")

            PermissionChecker.verifyRWStoragePermissions(this)
            if (PermissionChecker.hasRWStoragePermissions(this)) {

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)

                if (radioFileFormat.checkedRadioButtonId == R.id.radio_format_csv) {
                    intent.type = "text/csv"
                    intent.putExtra(
                        Intent.EXTRA_TITLE,
                        ExportPlainCredentialsUseCase.getTakeoutFileName(this, "csv")
                    )
                    getProgressBar()?.let { progressBar -> showProgressBar(progressBar) }

                    startActivityForResult(Intent.createChooser(intent, getString(R.string.save_as)), saveAsFile)

                } else if (radioFileFormat.checkedRadioButtonId == R.id.radio_format_kdbx) {
                    // ask for password
                    PasswordDialog.openAskForPasswordDialog(
                        this,
                        "Set a master password",
                        "A Keepass vault requires a master password.",
                        getString(android.R.string.ok),
                        getString(android.R.string.cancel)
                    )
                    { keepassMasterPassword ->
                        if (keepassMasterPassword != null) {
                            this.keepassMasterPassword = keepassMasterPassword
                            intent.type = "application/x-keepass"
                            intent.putExtra(
                                Intent.EXTRA_TITLE,
                                ExportPlainCredentialsUseCase.getTakeoutFileName(this, "kdbx")
                            )
                            getProgressBar()?.let { progressBar -> showProgressBar(progressBar) }

                            startActivityForResult(Intent.createChooser(intent, getString(R.string.save_as)), saveAsFile)
                        }
                    }
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
            cipherAlgorithm,
            this,
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
        getProgressBar()?.let { progressBar -> hideProgressBar(progressBar) }

        if (resultCode == RESULT_OK && requestCode == saveAsFile) {


            data?.data?.let {
                val intent = Intent(this, FileIOService::class.java)
                intent.action = FileIOService.ACTION_EXPORT_PLAIN_CREDENTIALS

                if (radioFileFormat.checkedRadioButtonId == R.id.radio_format_csv) {
                    intent.action = FileIOService.ACTION_EXPORT_PLAIN_CREDENTIALS
                }
                else if (radioFileFormat.checkedRadioButtonId == R.id.radio_format_kdbx) {
                    intent.action = FileIOService.ACTION_EXPORT_AS_KDBX

                    val tempKey = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, this)

                    keepassMasterPassword?.let { keepassMasterPassword ->
                        val encKeepassPassword =
                            SecretService.encryptPassword(tempKey, keepassMasterPassword)
                        keepassMasterPassword.clear()
                        intent.putEncryptedExtra(
                            FileIOService.PARAM_KEEPASS_PASSWORD,
                            encKeepassPassword
                        )
                    }
                }

                intent.putExtra(FileIOService.PARAM_FILE_URI, it)

                getProgressBar()?.let { progressBar -> showProgressBar(progressBar) }
                startService(intent)

                finish()

                val upIntent = Intent(this, ListCredentialsActivity::class.java)
                navigateUpTo(upIntent)

            }



        }
    }



}