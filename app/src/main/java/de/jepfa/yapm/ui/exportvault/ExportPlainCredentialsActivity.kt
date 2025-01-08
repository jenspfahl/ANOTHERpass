package de.jepfa.yapm.ui.exportvault

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.AsyncWithProgressBar
import de.jepfa.yapm.ui.ChangeKeyboardForPinManager
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.ui.credential.KeepassPasswordDialog
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


            if (Session.isDenied()) {
                return@setOnClickListener
            }

            PermissionChecker.verifyRWStoragePermissions(this)
            if (PermissionChecker.hasRWStoragePermissions(this)) {

                val currentPin = Password(currentPinTextView.text)
                var errorMessage: String? = null
                AsyncWithProgressBar(this,
                    {
                        errorMessage = ExportPlainCredentialsUseCase.checkPin(currentPin, this)
                        return@AsyncWithProgressBar errorMessage == null
                    },
                    { success ->
                        currentPin.clear()
                        if (success) {
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
                                KeepassPasswordDialog.openAskForSettingAPasswordDialog(
                                    this
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
                        else {
                            currentPinTextView.error = errorMessage
                            currentPinTextView.requestFocus()
                        }
                    })

            }
        }

        findViewById<Button>(R.id.button_share_plain_credentials).setOnClickListener {

            if (Session.isDenied()) {
                return@setOnClickListener
            }

            val currentPin = Password(currentPinTextView.text)

            if (radioFileFormat.checkedRadioButtonId == R.id.radio_format_csv) {

                val input = ExportPlainCredentialsUseCase.Input(currentPin, null)
                UseCaseBackgroundLauncher(ExportPlainCredentialsUseCase).launch(this, input)
                { output ->
                    currentPin.clear()
                    if (output.success) {
                        ExportPlainCredentialsUseCase.startShareActivity(output.data, this)
                    }
                    else {
                        currentPinTextView.error = output.errorMessage
                        currentPinTextView.requestFocus()
                    }
                }
            } else if (radioFileFormat.checkedRadioButtonId == R.id.radio_format_kdbx) {
                // ask for password
                KeepassPasswordDialog.openAskForSettingAPasswordDialog(
                    this,
                )
                { keepassMasterPassword ->
                    if (keepassMasterPassword != null) {
                        val input = ExportPlainCredentialsUseCase.Input(currentPin, keepassMasterPassword)

                        UseCaseBackgroundLauncher(ExportPlainCredentialsUseCase).launch(this, input)
                        { output ->
                            currentPin.clear()
                            keepassMasterPassword.clear()
                            if (output.success) {
                                ExportPlainCredentialsUseCase.startShareActivity(output.data, this)
                            }
                            else {
                                currentPinTextView.error = output.errorMessage
                                currentPinTextView.requestFocus()
                            }

                        }
                    }
                }
            }


        }
    }


    override fun lock() {
        recreate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        getProgressBar()?.let { progressBar -> hideProgressBar(progressBar) }

        if (resultCode == RESULT_OK && requestCode == saveAsFile) {


            data?.data?.let { uri ->
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

                intent.putExtra(FileIOService.PARAM_FILE_URI, uri)

                getProgressBar()?.let { progressBar -> showProgressBar(progressBar) }
                startService(intent)

                finish()

                val upIntent = Intent(this, ListCredentialsActivity::class.java)
                navigateUpTo(upIntent)

            }



        }
    }



}