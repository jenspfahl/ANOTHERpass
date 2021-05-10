package de.jepfa.yapm.ui.changelogin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.ChangePinUseCase
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.util.AsyncWithProgressBar
import de.jepfa.yapm.util.DebugInfo
import java.lang.RuntimeException

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
            throw RuntimeException("test bug report")
            true
        }

        findViewById<Button>(R.id.button_change).setOnClickListener {

            val currentPin = Password.fromEditable(currentPinTextView.text)
            val newPin1 = Password.fromEditable(newPin1TextView.text)
            val newPin2 = Password.fromEditable(newPin2TextView.text)

            if (currentPin.isEmpty()) {
                currentPinTextView.setError(getString(R.string.pin_required))
                currentPinTextView.requestFocus()
            }
            else if (newPin1.isEmpty()) {
                newPin1TextView.setError(getString(R.string.pin_required))
                newPin1TextView.requestFocus()
            }
            else if (newPin1.length < 4) {
                newPin1TextView.setError(getString(R.string.pin_too_short))
                newPin1TextView.requestFocus()
            }
            else if (! newPin1.isEqual(newPin2)) {
                newPin2TextView.setError(getString(R.string.pin_not_equal))
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

            AsyncWithProgressBar(
                this,
                {
                    ChangePinUseCase.execute(currentPin, newPin1, this)
                },
                { success ->
                    if (success) {
                        val upIntent = Intent(intent)
                        navigateUpTo(upIntent)

                        newPin1.clear()
                        newPin2.clear()

                        Toast.makeText(baseContext, "Pin successfully changed", Toast.LENGTH_LONG).show()
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