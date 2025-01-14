package de.jepfa.yapm.ui.changelogin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.google.android.material.slider.Slider
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.encrypted.KdfConfig
import de.jepfa.yapm.model.encrypted.KeyDerivationFunction
import de.jepfa.yapm.model.encrypted.PREFERRED_KDF
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.KdfParameterService
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

    private lateinit var originKdfConfig: KdfConfig
    private lateinit var originCipherAlgorithm: CipherAlgorithm
    private lateinit var selectedCipherAlgorithm: CipherAlgorithm
    private lateinit var pbkdfParamSection: LinearLayout
    private lateinit var argon2ParamSection: LinearLayout
    private var kdfAlgorithm = PREFERRED_KDF
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


        // KDF

        val kdfSelectionInfo: ImageView = findViewById(R.id.imageview_kdf_selection)
        val kdfSelection: AppCompatSpinner = findViewById(R.id.kdf_selection)
        val kdfSelectionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            KeyDerivationFunction.entries
                .map { getString(it.uiLabel)})
        kdfSelection.adapter = kdfSelectionAdapter
        kdfSelection.onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                kdfAlgorithm = KeyDerivationFunction.entries[position]
                updateParamsVisibility()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                kdfAlgorithm = PREFERRED_KDF
                updateParamsVisibility()
            }
        }
        originKdfConfig = SecretService.getStoredKdfConfig(this)
        kdfAlgorithm = originKdfConfig.kdf
        kdfSelection.setSelection(kdfAlgorithm.ordinal)

        kdfSelectionInfo.setOnClickListener{
            AlertDialog.Builder(this)
                .setTitle(getString(kdfAlgorithm.uiLabel))
                .setMessage(getString(kdfAlgorithm.description))
                .show()
        }

        // PBKDF iteration slider

        val pbkdfIterationsSlider = findViewById<Slider>(R.id.login_pbkdf_iterations_selection)
        val pbkdfIterationsSelectionView = findViewById<TextView>(R.id.current_pbkdf_iterations_selection)
        pbkdfIterationsSlider.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
            val iterations = KdfParameterService.mapPercentageToPbkdfIterations(value)
            pbkdfIterationsSelectionView.text = iterations.toReadableFormat() + " " + getString(R.string.login_iterations)
        })

        val currentPbkdfIterations = KdfParameterService.getStoredPbkdfIterations()
        pbkdfIterationsSlider.value = KdfParameterService.mapPbkdfIterationsToPercentage(currentPbkdfIterations)
        pbkdfIterationsSelectionView.text = currentPbkdfIterations.toReadableFormat() + " " + getString(R.string.login_iterations)


        // Argon2 iteration slider

        val argon2IterationsSlider = findViewById<Slider>(R.id.login_argon2_iterations_selection)
        val argon2IterationsSelectionView = findViewById<TextView>(R.id.current_argon2_iterations_selection)
        argon2IterationsSlider.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
            argon2IterationsSelectionView.text = value.toInt().toReadableFormat() + " " + getString(R.string.login_iterations)
        })

        argon2IterationsSlider.value = PreferenceService.getAsInt(PreferenceService.DATA_ARGON2_ITERATIONS, this).toFloat()
        argon2IterationsSlider.valueFrom = KdfParameterService.MIN_ARGON_ITERATIONS.toFloat()
        argon2IterationsSlider.valueTo = KdfParameterService.MAX_ARGON_ITERATIONS.toFloat()
        argon2IterationsSelectionView.text = argon2IterationsSlider.value.toInt().toReadableFormat() + " " + getString(R.string.login_iterations)


        // Argon2 mem slider

        val argon2MemSlider = findViewById<Slider>(R.id.login_argon2_memory_usage_selection)
        val argon2MemSelectionView = findViewById<TextView>(R.id.current_argon2_memory_usage_selection)
        argon2MemSlider.addOnChangeListener(Slider.OnChangeListener { _, value, fromUser ->
            argon2MemSelectionView.text = value.toInt().toReadableFormat() + " " + "Mem cost"
        })

        argon2MemSlider.value = PreferenceService.getAsInt(PreferenceService.DATA_ARGON2_MIB, this).toFloat()
        argon2MemSlider.valueFrom = KdfParameterService.MIN_ARGON_MIB.toFloat()
        argon2MemSlider.valueTo = KdfParameterService.MAX_ARGON_MIB.toFloat()
        argon2MemSelectionView.text = argon2MemSlider.value.toInt().toReadableFormat() + " " + "Mem cost"




        findViewById<Button>(R.id.button_test_login_time).setOnClickListener {
            val iterations = KdfParameterService.mapPercentageToPbkdfIterations(pbkdfIterationsSlider.value)
            val input = if (isArgon2())
                BenchmarkLoginIterationsUseCase.Input(
                    KdfConfig(
                        kdfAlgorithm,
                        argon2IterationsSlider.value.toInt(),
                        argon2MemSlider.value.toInt()),
                    selectedCipherAlgorithm)
            else
                BenchmarkLoginIterationsUseCase.Input(
                    KdfConfig(
                        kdfAlgorithm,
                        iterations,
                        null),
                    selectedCipherAlgorithm)
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

        pbkdfParamSection = findViewById(R.id.pbkdf_param_section)
        argon2ParamSection = findViewById(R.id.argon2_param_section)
        updateParamsVisibility()




        val changeButton = findViewById<Button>(R.id.button_change)
        changeButton.setOnClickListener {

            val newIterations = KdfParameterService.mapPercentageToPbkdfIterations(pbkdfIterationsSlider.value)

            val newKdfConfig = KdfConfig(
                kdfAlgorithm,
                if (isArgon2())
                    argon2IterationsSlider.value.toInt()
                else
                    KdfParameterService.mapPercentageToPbkdfIterations(pbkdfIterationsSlider.value),
                argon2MemSlider.value.toInt()
            )

            Log.d(LOG_PREFIX + "ITERATIONS", "final iterations=$newIterations")
            val currentPin = Password(currentPinTextView.text)

            if (currentPin.isEmpty()) {
                currentPinTextView.error = getString(R.string.pin_required)
                currentPinTextView.requestFocus()
            }
            else if (selectedCipherAlgorithm == originCipherAlgorithm
                && !newMasterKeySwitch.isChecked
                && originKdfConfig == newKdfConfig) {
                toastText(this, R.string.nothing_has_been_changed)
                currentPin.clear()
            }
            else {
                val masterPassword = MasterPasswordService.getMasterPasswordFromSession(this)
                    ?: return@setOnClickListener
                ChangeVaultEncryptionUseCase.openDialog(
                    ChangeVaultEncryptionUseCase.Input(
                        LoginData(currentPin, masterPassword),
                        newKdfConfig,
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

    private fun updateParamsVisibility() {
        pbkdfParamSection.isVisible = !isArgon2()
        argon2ParamSection.isVisible = isArgon2()
    }

    private fun isArgon2() = kdfAlgorithm != KeyDerivationFunction.BUILT_IN_PBKDF



}