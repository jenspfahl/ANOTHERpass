package de.jepfa.yapm.ui.changelogin

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SwitchCompat
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.encrypted.DEFAULT_CIPHER_ALGORITHM
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.secret.ChangeMasterPasswordUseCase
import de.jepfa.yapm.usecase.secret.GenerateMasterPasswordUseCase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.PasswordColorizer
import de.jepfa.yapm.util.toastText

class ChangeEncryptionActivity : SecureActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var originCipherAlgorithm: CipherAlgorithm
    private lateinit var selectedCipherAlgorithm: CipherAlgorithm

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }

        setContentView(R.layout.activity_change_encryption)

        val currentPinTextView: EditText = findViewById(R.id.current_pin)
        val currentEncryptionTextView: TextView = findViewById(R.id.current_encryption_text)
        val newMasterKeySwitch: SwitchCompat = findViewById(R.id.switch_generate_new_masterkey)

        originCipherAlgorithm = SecretService.getCipherAlgorithm(this)
        currentEncryptionTextView.text = getString(R.string.update_vault_cipher_explanation,
            getString(originCipherAlgorithm.uiLabel))
        selectedCipherAlgorithm = originCipherAlgorithm

        val cipherSelectionInfo: ImageView = findViewById(R.id.imageview_cipher_selection)
        val cipherSelection: AppCompatSpinner = findViewById(R.id.cipher_selection)
        val cipherSelectionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            CipherAlgorithm.supportedValues()
                .map { getString(it.uiLabel)})
        cipherSelection.adapter = cipherSelectionAdapter
        val originSelection = CipherAlgorithm.supportedValues().indexOf(originCipherAlgorithm)
        cipherSelection.setSelection(originSelection)
        cipherSelection.onItemSelectedListener = this

        cipherSelectionInfo.setOnClickListener{
            AlertDialog.Builder(this)
                .setTitle(getString(selectedCipherAlgorithm.uiLabel))
                .setMessage(getString(selectedCipherAlgorithm.description))
                .show()

        }

        val changeButton = findViewById<Button>(R.id.button_change)
        changeButton.setOnClickListener {

            val currentPin = Password(currentPinTextView.text)

            if (currentPin.isEmpty()) {
                currentPinTextView.error = getString(R.string.pin_required)
                currentPinTextView.requestFocus()
            }
            else {


            }
        }

        hideKeyboard(currentPinTextView)
    }

    override fun lock() {
        recreate()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedCipherAlgorithm = CipherAlgorithm.supportedValues()[position]
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        selectedCipherAlgorithm = originCipherAlgorithm
    }

}