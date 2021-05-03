package de.jepfa.yapm.ui.changelogin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.ChangeMasterPasswordUseCase
import de.jepfa.yapm.usecase.GenerateMasterPasswordUseCase
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.util.AsyncWithProgressBar
import de.jepfa.yapm.util.PasswordColorizer
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.PreferenceUtil.DATA_ENCRYPTED_MASTER_PASSWORD

class ChangeMasterPasswordActivity : SecureActivity() {

    private var generatedPassword: Password = Password.empty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }

        setContentView(R.layout.activity_change_master_password)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val currentPinTextView: EditText = findViewById(R.id.current_pin)
        val pseudoPhraseSwitch: Switch = findViewById(R.id.switch_use_pseudo_phrase)
        val switchStorePasswd: Switch = findViewById(R.id.switch_store_master_password)
        val generatedPasswdView: TextView = findViewById(R.id.generated_passwd)

        val storedMasterPasswdPresent = PreferenceUtil.isPresent(DATA_ENCRYPTED_MASTER_PASSWORD, this)
        switchStorePasswd.isChecked = storedMasterPasswdPresent

        val buttonGeneratePasswd: Button = findViewById(R.id.button_generate_passwd)
        buttonGeneratePasswd.setOnClickListener {
            generatedPassword = GenerateMasterPasswordUseCase.execute(pseudoPhraseSwitch.isChecked)
            generatedPasswdView.text = PasswordColorizer.spannableString(generatedPassword, this)
        }

        val button = findViewById<Button>(R.id.button_change)
        button.setOnClickListener {

            val currentPin = Password.fromEditable(currentPinTextView.text)

            if (currentPin.isEmpty()) {
                currentPinTextView.setError(getString(R.string.pin_required))
                currentPinTextView.requestFocus()
            }
            else if (generatedPassword.data.isEmpty()) {
                Toast.makeText(it.context, getString(R.string.generate_passwd_first), Toast.LENGTH_LONG).show()
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val upIntent = Intent(intent)
            navigateUpTo(upIntent)
            return true
        }
        return super.onOptionsItemSelected(item)
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

                        Toast.makeText(baseContext, "Master password successfully changed", Toast.LENGTH_LONG).show()
                    }
                    else {
                        currentPinTextView.setError(getString(R.string.pin_wrong))
                        currentPinTextView.requestFocus()
                    }
                }
            )

        }
    }

}