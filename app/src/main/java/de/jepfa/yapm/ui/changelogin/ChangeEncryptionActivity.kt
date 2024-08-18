package de.jepfa.yapm.ui.changelogin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.slider.Slider
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.PbkdfIterationService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.ChangeKeyboardForPinManager
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.secret.ChangeVaultEncryptionUseCase
import de.jepfa.yapm.usecase.vault.BenchmarkLoginIterationsUseCase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import de.jepfa.yapm.util.toReadableFormat
import de.jepfa.yapm.util.toastText

class ChangeEncryptionActivity : SecureActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var originCipherAlgorithm: CipherAlgorithm
    private lateinit var selectedCipherAlgorithm: CipherAlgorithm
    private var askForBenchmarking = true

    init {
        enableBack = true
    }

    @SuppressLint("SetTextI18n")
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

        val pinImeiManager = ChangeKeyboardForPinManager(this, listOf(currentPinTextView))
        pinImeiManager.create(findViewById(R.id.imageview_change_imei))


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

        val iterationsSlider = findViewById<Slider>(R.id.login_iterations_selection)
        val iterationsSelectionView = findViewById<TextView>(R.id.current_iterations_selection)
        iterationsSlider.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
            val iterations = PbkdfIterationService.mapPercentageToIterations(value)
            iterationsSelectionView.text = iterations.toReadableFormat() + " " + getString(R.string.login_iterations)
        })

        val currentIterations = PbkdfIterationService.getStoredPbkdfIterations()
        iterationsSlider.value = PbkdfIterationService.mapIterationsToPercentage(currentIterations)
        iterationsSelectionView.text = currentIterations.toReadableFormat() + " " + getString(R.string.login_iterations)

        findViewById<Button>(R.id.button_test_login_time).setOnClickListener {
            val iterations = PbkdfIterationService.mapPercentageToIterations(iterationsSlider.value)
            val input = BenchmarkLoginIterationsUseCase.Input(iterations, selectedCipherAlgorithm)

            if (askForBenchmarking) {
                BenchmarkLoginIterationsUseCase.openStartBenchmarkingDialog(input, this)
                { askForBenchmarking = false }
            }
            else {
                UseCaseBackgroundLauncher(BenchmarkLoginIterationsUseCase)
                    .launch(this, input)
                    { output ->
                        BenchmarkLoginIterationsUseCase.openResultDialog(input, output.data, this)
                    }
            }
        }

        val changeButton = findViewById<Button>(R.id.button_change)
        changeButton.setOnClickListener {

            val newIterations = PbkdfIterationService.mapPercentageToIterations(iterationsSlider.value)
            Log.d(LOG_PREFIX + "ITERATIONS", "final iterations=$newIterations")
            val currentPin = Password(currentPinTextView.text)

            if (currentPin.isEmpty()) {
                currentPinTextView.error = getString(R.string.pin_required)
                currentPinTextView.requestFocus()
            }
            else if (selectedCipherAlgorithm == originCipherAlgorithm
                && !newMasterKeySwitch.isChecked
                && newIterations == currentIterations) {
                toastText(this, R.string.nothing_has_been_changed)
                currentPin.clear()
            }
            else {
                val masterPassword = MasterPasswordService.getMasterPasswordFromSession(this)
                    ?: return@setOnClickListener
                ChangeVaultEncryptionUseCase.openDialog(
                    ChangeVaultEncryptionUseCase.Input(
                        LoginData(currentPin, masterPassword),
                        newIterations,
                        selectedCipherAlgorithm,
                        newMasterKeySwitch.isChecked
                ), this)
                { output ->
                    currentPin.clear()
                    masterPassword.clear()

                    if (output.success) {
                        val upIntent = Intent(intent)
                        navigateUpTo(upIntent)

                        masterPassword.clear()
                        toastText(baseContext, R.string.encryption_changed)
                    }
                    else {
                        currentPinTextView.error = getString(R.string.pin_wrong)
                        currentPinTextView.requestFocus()
                    }
                }

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