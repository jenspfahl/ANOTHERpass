package de.jepfa.yapm.ui.changelogin

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.ChangeMasterPasswordUseCase
import de.jepfa.yapm.usecase.GenerateMasterPasswordUseCase
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.ui.AsyncWithProgressBar
import de.jepfa.yapm.util.PasswordColorizer
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_ENCRYPTED_MASTER_PASSWORD

class ChangeMasterPasswordActivity : SecureActivity() {

    private var generatedPassword: Password = Password.empty()
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

        val storedMasterPasswdPresent = PreferenceService.isPresent(DATA_ENCRYPTED_MASTER_PASSWORD, this)
        switchStorePasswd.isChecked = storedMasterPasswdPresent

        val masterPasswd = MasterPasswordService.getMasterPasswordFromSession()
        if (!Session.isDenied() && masterPasswd != null) {
            generatedPassword = masterPasswd
            generatedPasswdView.text = PasswordColorizer.spannableString(generatedPassword, this)
        }

        val buttonGeneratePasswd: Button = findViewById(R.id.button_generate_passwd)
        buttonGeneratePasswd.setOnClickListener {
            generatedPassword = GenerateMasterPasswordUseCase.execute(pseudoPhraseSwitch.isChecked)
            generatedPasswdView.text = PasswordColorizer.spannableString(generatedPassword, this)
            passwordChanged = true
        }

        val changeButton = findViewById<Button>(R.id.button_change)
        changeButton.setOnClickListener {

            val currentPin = Password.fromEditable(currentPinTextView.text)

            if (currentPin.isEmpty()) {
                currentPinTextView.error = getString(R.string.pin_required)
                currentPinTextView.requestFocus()
            }
            else if (generatedPassword.data.isEmpty()) {
                Toast.makeText(it.context, getString(R.string.generate_passwd_first), Toast.LENGTH_LONG).show()
            }
            else if (!passwordChanged) {
                Toast.makeText(it.context, getString(R.string.master_password_not_changed), Toast.LENGTH_LONG).show()
            }
            else {
                changeMasterPin(
                    currentPinTextView,
                    currentPin,
                    storeMasterPassword = switchStorePasswd.isChecked)
            }
        }
    }

    override fun lock() {
        recreate()
    }

    private fun changeMasterPin(
        currentPinTextView: TextView,
        currentPin: Password,
        storeMasterPassword: Boolean
    ) {

        hideKeyboard(currentPinTextView)

        getProgressBar()?.let {

            AsyncWithProgressBar(
                this,
                {
                    ChangeMasterPasswordUseCase.execute(currentPin, generatedPassword, storeMasterPassword, this)
                },
                { success ->
                    if (success) {
                        val upIntent = Intent(intent)
                        navigateUpTo(upIntent)

                        generatedPassword.clear()

                        Toast.makeText(baseContext, getString(R.string.masterpassword_changed), Toast.LENGTH_LONG).show()
                    }
                    else {
                        currentPinTextView.error = getString(R.string.pin_wrong)
                        currentPinTextView.requestFocus()
                    }
                }
            )

        }
    }

}