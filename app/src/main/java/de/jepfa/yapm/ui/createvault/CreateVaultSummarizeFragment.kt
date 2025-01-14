package de.jepfa.yapm.ui.createvault

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.Slider
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.encrypted.CipherAlgorithm.Companion.getPreferredCipher
import de.jepfa.yapm.model.encrypted.KdfConfig
import de.jepfa.yapm.model.encrypted.KeyDerivationFunction
import de.jepfa.yapm.model.encrypted.PREFERRED_KDF
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.AndroidKey.ALIAS_KEY_TRANSPORT
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.KdfParameterService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.decryptPassword
import de.jepfa.yapm.service.secret.SecretService.encryptPassword
import de.jepfa.yapm.service.secret.SecretService.getAndroidSecretKey
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.createvault.CreateVaultActivity.Companion.ARG_ENC_MASTER_PASSWD
import de.jepfa.yapm.usecase.secret.ExportEncMasterPasswordUseCase
import de.jepfa.yapm.usecase.vault.BenchmarkLoginIterationsUseCase
import de.jepfa.yapm.usecase.vault.CreateVaultUseCase
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.PasswordColorizer
import de.jepfa.yapm.util.getEncrypted
import de.jepfa.yapm.util.toReadableFormat
import de.jepfa.yapm.util.toastText

class CreateVaultSummarizeFragment : BaseFragment() {

    private lateinit var pbkdfParamSection: LinearLayout
    private lateinit var argon2ParamSection: LinearLayout
    private var cipherAlgorithm = getPreferredCipher()
    private var kdfAlgorithm = PREFERRED_KDF
    private var askForBenchmarking = true

    init {
        enableBack = true
        backToPreviousFragment = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_vault_summarize, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(R.string.create_vault_summarize_fragment_label)

        val encMasterPasswd = arguments?.getEncrypted(ARG_ENC_MASTER_PASSWD)
        if (encMasterPasswd == null) {
            DebugInfo.logException("CV", "No master passwd in extra")
            toastText(context, R.string.something_went_wrong)
            return
        }
        val transSK = getAndroidSecretKey(ALIAS_KEY_TRANSPORT, view.context)
        val masterPasswd = decryptPassword(transSK, encMasterPasswd)

        val switchStorePasswd: SwitchCompat = view.findViewById(R.id.switch_store_master_password)
        val generatedPasswdView: TextView = view.findViewById(R.id.generated_passwd)
        generatedPasswdView.text = PasswordColorizer.spannableString(masterPasswd, getBaseActivity())

        val cipherSelectionInfo: ImageView = view.findViewById(R.id.imageview_cipher_selection)
        val cipherSelection: AppCompatSpinner = view.findViewById(R.id.cipher_selection)
        context?.let { _context ->
            val cipherSelectionAdapter = ArrayAdapter(
                _context,
                android.R.layout.simple_spinner_dropdown_item,
                CipherAlgorithm.supportedValues()
                    .map { getString(it.uiLabel)})
            cipherSelection.adapter = cipherSelectionAdapter
            cipherSelection.onItemSelectedListener = object: OnItemSelectedListener{
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    cipherAlgorithm = CipherAlgorithm.supportedValues()[position]
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    cipherAlgorithm = getPreferredCipher()
                }
            }
            cipherSelection.setSelection(cipherAlgorithm.ordinal)

            cipherSelectionInfo.setOnClickListener{
                AlertDialog.Builder(_context)
                    .setTitle(getString(cipherAlgorithm.uiLabel))
                    .setMessage(getString(cipherAlgorithm.description))
                    .show()
            }
        }

        val exportAsQrcImageView: ImageView = view.findViewById(R.id.imageview_qrcode)
        exportAsQrcImageView.setOnClickListener {
            val tempKey = getAndroidSecretKey(ALIAS_KEY_TRANSPORT, view.context)
            val encMasterPasswd = encryptPassword(tempKey, masterPasswd)
            getBaseActivity()?.let { baseActivity ->
                ExportEncMasterPasswordUseCase.startUiFlow(baseActivity, encMasterPasswd, noSessionCheck = true)
            }
        }
        val exportAsNfcImageView: ImageView = view.findViewById(R.id.imageview_nfc_tag)
        if (!NfcService.isNfcAvailable(getBaseActivity())) {
            exportAsNfcImageView.visibility = View.GONE
        }
        exportAsNfcImageView.setOnClickListener {
            val tempKey = getAndroidSecretKey(ALIAS_KEY_TRANSPORT, view.context)
            val encMasterPasswd = encryptPassword(tempKey, masterPasswd)
            getBaseActivity()?.let { baseActivity ->
                ExportEncMasterPasswordUseCase.startUiFlow(baseActivity, encMasterPasswd,
                    noSessionCheck = true, directlyToNfcActivity = true)
            }
        }
        
        // KDF

        val kdfSelectionInfo: ImageView = view.findViewById(R.id.imageview_kdf_selection)
        val kdfSelection: AppCompatSpinner = view.findViewById(R.id.kdf_selection)
        context?.let { _context ->
            val kdfSelectionAdapter = ArrayAdapter(
                _context,
                android.R.layout.simple_spinner_dropdown_item,
                KeyDerivationFunction.entries
                    .map { getString(it.uiLabel)})
            kdfSelection.adapter = kdfSelectionAdapter
            kdfSelection.onItemSelectedListener = object: OnItemSelectedListener{
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    kdfAlgorithm = KeyDerivationFunction.entries[position]
                    updateParamsVisibility()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    kdfAlgorithm = PREFERRED_KDF
                    updateParamsVisibility()
                }
            }
            kdfSelection.setSelection(kdfAlgorithm.ordinal)

            kdfSelectionInfo.setOnClickListener{
                AlertDialog.Builder(_context)
                    .setTitle(getString(kdfAlgorithm.uiLabel))
                    .setMessage(getString(kdfAlgorithm.description))
                    .show()
            }
        }


        // PBKDF iteration slider

        val pbkdfIterationsSlider = view.findViewById<Slider>(R.id.login_pbkdf_iterations_selection)
        val pbkdfIterationsSelectionView = view.findViewById<TextView>(R.id.current_pbkdf_iterations_selection)
        pbkdfIterationsSlider.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
            val iterations = KdfParameterService.mapPercentageToPbkdfIterations(value)
            pbkdfIterationsSelectionView.text = iterations.toReadableFormat() + " " + getString(R.string.login_iterations)
        })

        val currentPbkdfIterations = KdfParameterService.DEFAULT_PBKDF_ITERATIONS
        pbkdfIterationsSlider.value = KdfParameterService.mapPbkdfIterationsToPercentage(currentPbkdfIterations)
        pbkdfIterationsSelectionView.text = currentPbkdfIterations.toReadableFormat() + " " + getString(R.string.login_iterations)


        // Argon2 iteration slider

        val argon2IterationsSlider = view.findViewById<Slider>(R.id.login_argon2_iterations_selection)
        val argon2IterationsSelectionView = view.findViewById<TextView>(R.id.current_argon2_iterations_selection)
        argon2IterationsSlider.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
            argon2IterationsSelectionView.text = value.toInt().toReadableFormat() + " " + getString(R.string.login_iterations)
        })

        argon2IterationsSlider.value = KdfParameterService.DEFAULT_ARGON_ITERATIONS.toFloat()
        argon2IterationsSlider.valueFrom = KdfParameterService.MIN_ARGON_ITERATIONS.toFloat()
        argon2IterationsSlider.valueTo = KdfParameterService.MAX_ARGON_ITERATIONS.toFloat()
        argon2IterationsSelectionView.text = argon2IterationsSlider.value.toInt().toReadableFormat() + " " + getString(R.string.login_iterations)


        // Argon2 mem slider

        val argon2MemSlider = view.findViewById<Slider>(R.id.login_argon2_memory_usage_selection)
        val argon2MemSelectionView = view.findViewById<TextView>(R.id.current_argon2_memory_usage_selection)
        argon2MemSlider.addOnChangeListener(Slider.OnChangeListener { _, value, fromUser ->
            argon2MemSelectionView.text = value.toInt().toReadableFormat() + " " + "Mem cost"
        })

        argon2MemSlider.value = KdfParameterService.DEFAULT_ARGON_MIB.toFloat()
        argon2MemSlider.valueFrom = KdfParameterService.MIN_ARGON_MIB.toFloat()
        argon2MemSlider.valueTo = KdfParameterService.MAX_ARGON_MIB.toFloat()
        argon2MemSelectionView.text = argon2MemSlider.value.toInt().toReadableFormat() + " " + "Mem cost"




        view.findViewById<Button>(R.id.button_test_login_time).setOnClickListener {
            getBaseActivity()?.let { activity ->
                val iterations = KdfParameterService.mapPercentageToPbkdfIterations(pbkdfIterationsSlider.value)
                val input = if (isArgon2())
                    BenchmarkLoginIterationsUseCase.Input(
                        KdfConfig(
                            kdfAlgorithm,
                            argon2IterationsSlider.value.toInt(),
                            argon2MemSlider.value.toInt()),
                        cipherAlgorithm)
                else
                    BenchmarkLoginIterationsUseCase.Input(
                        KdfConfig(
                            kdfAlgorithm,
                            iterations,
                            null),
                        cipherAlgorithm)
                if (askForBenchmarking) {
                    BenchmarkLoginIterationsUseCase.openStartBenchmarkingDialog(input, activity)
                    { askForBenchmarking = false }
                }
                else {
                    UseCaseBackgroundLauncher(BenchmarkLoginIterationsUseCase)
                        .launch(activity, input)
                        { output ->
                            BenchmarkLoginIterationsUseCase.openResultDialog(input, output.data, activity)
                        }
                }
            }
        }

        pbkdfParamSection = view.findViewById(R.id.pbkdf_param_section)
        argon2ParamSection = view.findViewById(R.id.argon2_param_section)
        updateParamsVisibility()

        view.findViewById<Button>(R.id.button_create_vault).setOnClickListener {

            context?.let {
                if (MasterKeyService.isMasterKeyStored(it)) {
                    toastText(it, R.string.vault_already_created)
                    return@setOnClickListener
                }

                val encPin = arguments?.getEncrypted(CreateVaultActivity.ARG_ENC_PIN)
                if (encPin == null) {
                    DebugInfo.logException("CV", "No pin in extra")
                    toastText(it, R.string.something_went_wrong)
                    return@setOnClickListener
                }

                val pin = decryptPassword(transSK, encPin)
                val iterations = if (isArgon2())
                    argon2IterationsSlider.value.toInt()
                else
                    KdfParameterService.mapPercentageToPbkdfIterations(pbkdfIterationsSlider.value)

                val costInKiB = if (isArgon2())
                    argon2MemSlider.value.toInt()
                else
                    null

                Log.d(LOG_PREFIX + "ITERATIONS", "final iterations=$iterations")

                getBaseActivity()?.let { baseActivity ->

                    if (switchStorePasswd.isChecked) {
                        MasterPasswordService.storeMasterPassword(masterPasswd, baseActivity,
                            {
                                createVault(pin, masterPasswd, kdfAlgorithm, iterations, costInKiB, cipherAlgorithm, baseActivity)
                            },
                            {
                                toastText(activity, R.string.masterpassword_not_stored)
                            })
                    }
                    else {
                        createVault(pin, masterPasswd, kdfAlgorithm, iterations, costInKiB, cipherAlgorithm, baseActivity)
                    }

                }
            }

        }
    }

    private fun updateParamsVisibility() {
        pbkdfParamSection.isVisible = !isArgon2()
        argon2ParamSection.isVisible = isArgon2()
    }

    private fun isArgon2() = kdfAlgorithm != KeyDerivationFunction.BUILT_IN_PBKDF


    private fun createVault(
        pin: Password,
        masterPasswd: Password,
        kdf: KeyDerivationFunction,
        iterations: Int,
        costInMiB: Int?,
        cipherAlgorithm: CipherAlgorithm,
        activity: BaseActivity
    ) {

        val input = CreateVaultUseCase.Input(
            LoginData(pin, masterPasswd),
            KdfConfig(
                kdf,
                iterations,
                costInMiB),
            cipherAlgorithm
        )
        UseCaseBackgroundLauncher(CreateVaultUseCase)
            .launch(activity, input)
            { output ->
                if (!output.success) {
                    toastText(context, R.string.something_went_wrong)
                }
                else {
                    // here we are logged in so we can store the user seed encrypted
                    SecretService.persistUserSeed(activity)
                    pin.clear()
                    masterPasswd.clear()
                    findNavController().navigate(R.id.action_Create_Vault_to_ThirdFragment_to_Root)
                    getBaseActivity()?.finish()
                }
            }

    }
}


