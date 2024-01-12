package de.jepfa.yapm.ui.changelogin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.secret.ChangePinUseCase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.toastText

class ChangePinActivity : SecureActivity() {

    init {
        enableBack = true
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }

        setContentView(R.layout.activity_change_pin)

        val currentPinTextView: EditText = findViewById(R.id.current_pin)
        val newPin1TextView: EditText = findViewById(R.id.first_new_pin)
        val newPin2TextView: EditText = findViewById(R.id.second_new_pin)

        val explanationView: TextView = findViewById(R.id.change_pin_explanation)
        explanationView.setOnLongClickListener {
            if (DebugInfo.isDebug) {
                Log.w(LOG_PREFIX + "TEST", "trigger report")
                throw RuntimeException("test bug report")
            }
            true
        }

        findViewById<Button>(R.id.button_change).setOnClickListener {

            val currentPin = Password(currentPinTextView.text)
            val newPin1 = Password(newPin1TextView.text)
            val newPin2 = Password(newPin2TextView.text)

            if (currentPin.isEmpty()) {
                currentPinTextView.error = getString(R.string.pin_required)
                currentPinTextView.requestFocus()
            }
            else if (newPin1.isEmpty()) {
                newPin1TextView.error = getString(R.string.pin_required)
                newPin1TextView.requestFocus()
            }
            else if (newPin1.length < Constants.MIN_PIN_LENGTH) {
                newPin1TextView.error = getString(R.string.pin_too_short)
                newPin1TextView.requestFocus()
            }
            else if (! newPin1.isEqual(newPin2)) {
                newPin2TextView.error = getString(R.string.pin_not_equal)
                newPin2TextView.requestFocus()
            }
            else {
                changePin(currentPinTextView, currentPin, newPin1, newPin2)
            }
        }

    }

    override fun lock() {
        recreate()
    }

    private fun changePin(
        currentPinTextView: TextView,
        currentPin: Password,
        newPin1: Password,
        newPin2: Password
    ) {

        hideKeyboard(currentPinTextView)

        getProgressBar()?.let {

            UseCaseBackgroundLauncher(ChangePinUseCase)
                .launch(this, ChangePinUseCase.Input(currentPin, newPin1))
                { output ->
                    if (output.success) {
                        val upIntent = Intent(intent)
                        navigateUpTo(upIntent)

                        newPin1.clear()
                        newPin2.clear()

                        toastText(baseContext, R.string.pin_changed)
                    }
                    else {
                        currentPinTextView.error = getString(R.string.pin_wrong)
                        currentPinTextView.requestFocus()
                    }
                }
        }
    }

}