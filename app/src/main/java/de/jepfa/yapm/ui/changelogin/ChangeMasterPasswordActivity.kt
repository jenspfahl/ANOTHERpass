package de.jepfa.yapm.ui.changelogin

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.ui.ChangeKeyboardForPinManager
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.credential.ShowPasswordStrengthUseCase
import de.jepfa.yapm.usecase.secret.ChangeMasterPasswordUseCase
import de.jepfa.yapm.usecase.secret.GenerateMasterPasswordUseCase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.PasswordColorizer
import de.jepfa.yapm.util.toastText

class ChangeMasterPasswordActivity : SecureActivity() {

    private var generatedPassword: Password = Password.empty()
    private var combinations = 0.0
    private var passwordChanged = false

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }

        setContentView(R.layout.activity_change_master_password)

        val currentPinTextView: EditText = findViewById(R.id.current_pin)
        val pseudoPhraseSwitch: SwitchCompat = findViewById(R.id.switch_use_pseudo_phrase)
        val switchStorePasswd: SwitchCompat = findViewById(R.id.switch_store_master_password)
        val generatedPasswdView: TextView = findViewById(R.id.generated_passwd)

        val pinImeiManager = ChangeKeyboardForPinManager(this, listOf(currentPinTextView))
        pinImeiManager.create(findViewById(R.id.imageview_change_imei))

        switchStorePasswd.isChecked = MasterPasswordService.isMasterPasswordStored(this)

        val masterPasswd = MasterPasswordService.getMasterPasswordFromSession(this)
        if (!Session.isDenied() && masterPasswd != null) {
            generatedPassword = masterPasswd
            generatedPasswdView.text = PasswordColorizer.spannableString(generatedPassword, this)
        }

        generatedPasswdView.setOnLongClickListener {
            if (combinations == 0.0) {
                combinations = ShowPasswordStrengthUseCase.guessPasswordCombinations(generatedPassword)
            }
            ShowPasswordStrengthUseCase.showPasswordStrength(combinations, R.string.password_strength, this )
            true
        }

        val buttonGeneratePasswd: Button = findViewById(R.id.button_generate_passwd)

        if (DebugInfo.isDebug) {
            buttonGeneratePasswd.setOnLongClickListener {

                val input = EditText(it.context)
                input.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                input.setText(generatedPassword.toRawFormattedPassword(), TextView.BufferType.EDITABLE)

                val filters = arrayOf<InputFilter>(InputFilter.LengthFilter(Constants.MAX_CREDENTIAL_PASSWD_LENGTH))
                input.setFilters(filters)

                AlertDialog.Builder(it.context)
                    .setTitle(R.string.edit_password)
                    .setMessage(R.string.edit_password_message)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        generatedPassword = Password(input.text)
                        var spannedString = PasswordColorizer.spannableString(generatedPassword, it.context)
                        generatedPasswdView.text = spannedString
                        passwordChanged = true
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, which ->
                        dialog.cancel()
                    }
                    .show()

                true
            }

            hideKeyboard(currentPinTextView)
        }

        buttonGeneratePasswd.setOnClickListener {
            UseCaseBackgroundLauncher(GenerateMasterPasswordUseCase)
                .launch(this, pseudoPhraseSwitch.isChecked)
                { output ->
                    generatedPassword = output.data.first
                    combinations = output.data.second
                    generatedPasswdView.text = PasswordColorizer.spannableString(generatedPassword, this)
                    passwordChanged = true
                }
        }

        val changeButton = findViewById<Button>(R.id.button_change)
        changeButton.setOnClickListener {

            val currentPin = Password(currentPinTextView.text)

            if (currentPin.isEmpty()) {
                currentPinTextView.error = getString(R.string.pin_required)
                currentPinTextView.requestFocus()
            }
            else if (generatedPassword.data.isEmpty()) {
                toastText(it.context, R.string.generate_password_first)
            }
            else if (!passwordChanged) {
                toastText(it.context, R.string.master_password_not_changed)
            }
            else {

                if (switchStorePasswd.isChecked) {
                    MasterPasswordService.storeMasterPassword(generatedPassword, this,
                        {
                            changeMasterPin(currentPinTextView, currentPin)
                        },
                        {
                            toastText(this, R.string.masterpassword_not_stored)
                        })
                }
                else {
                    MasterPasswordService.deleteStoredMasterPassword(this)
                    changeMasterPin(currentPinTextView, currentPin)
                }

            }
        }
    }

    override fun lock() {
        recreate()
    }

    private fun changeMasterPin(
        currentPinTextView: TextView,
        currentPin: Password
    ) {

        hideKeyboard(currentPinTextView)

        getProgressBar()?.let {

            UseCaseBackgroundLauncher(ChangeMasterPasswordUseCase)
                .launch(this,
                    LoginData(currentPin, generatedPassword))
                    { output ->
                        if (output.success) {
                            val upIntent = Intent(intent)
                            navigateUpTo(upIntent)

                            generatedPassword.clear()

                            toastText(baseContext, R.string.masterpassword_changed)
                        }
                        else {
                            currentPinTextView.error = getString(R.string.pin_wrong)
                            currentPinTextView.requestFocus()
                        }
                    }


        }
    }

}