package de.jepfa.yapm.ui.createvault

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SwitchCompat
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.Slider
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.encrypted.DEFAULT_CIPHER_ALGORITHM
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.AndroidKey.ALIAS_KEY_TRANSPORT
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.PbkdfIterationService
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
import de.jepfa.yapm.util.*
import de.jepfa.yapm.util.Constants.LOG_PREFIX

class CreateVaultSummarizeFragment : BaseFragment(), AdapterView.OnItemSelectedListener {

    private var cipherAlgorithm = DEFAULT_CIPHER_ALGORITHM
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(R.string.create_vault_summarize_fragment_label)

        val encMasterPasswd = arguments?.getEncrypted(ARG_ENC_MASTER_PASSWD)
        if (encMasterPasswd == null) {
            Log.e(LOG_PREFIX + "CV", "No master passwd in extra")
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
            cipherSelection.onItemSelectedListener = this

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

        val iterationsSlider = view.findViewById<Slider>(R.id.login_iterations_selection)
        val iterationsSelectionView = view.findViewById<TextView>(R.id.current_iterations_selection)
        iterationsSlider.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
            val iterations = PbkdfIterationService.mapPercentageToIterations(value)
            iterationsSelectionView.text = iterations.toReadableFormat() + " " + getString(R.string.login_iterations)
        })

        val currentIterations = PbkdfIterationService.DEFAULT_PBKDF_ITERATIONS
        iterationsSlider.value = PbkdfIterationService.mapIterationsToPercentage(currentIterations)
        iterationsSelectionView.text = currentIterations.toReadableFormat() + " " + getString(R.string.login_iterations)

        view.findViewById<Button>(R.id.button_test_login_time).setOnClickListener {
            getBaseActivity()?.let { activity ->
                val iterations = PbkdfIterationService.mapPercentageToIterations(iterationsSlider.value)
                val input = BenchmarkLoginIterationsUseCase.Input(iterations, cipherAlgorithm)
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

        view.findViewById<Button>(R.id.button_create_vault).setOnClickListener {

            context?.let {
                if (MasterKeyService.isMasterKeyStored(it)) {
                    toastText(it, R.string.vault_already_created)
                    return@setOnClickListener
                }

                val encPin = arguments?.getEncrypted(CreateVaultActivity.ARG_ENC_PIN)
                if (encPin == null) {
                    Log.e(LOG_PREFIX + "CV", "No pin in extra")
                    toastText(it, R.string.something_went_wrong)
                    return@setOnClickListener
                }

                val pin = decryptPassword(transSK, encPin)
                val iterations = PbkdfIterationService.mapPercentageToIterations(iterationsSlider.value)
                Log.d(LOG_PREFIX + "ITERATIONS", "final iterations=$iterations")

                getBaseActivity()?.let { baseActivity ->

                    if (switchStorePasswd.isChecked) {
                        MasterPasswordService.storeMasterPassword(masterPasswd, baseActivity,
                            {
                                createVault(pin, masterPasswd, iterations, cipherAlgorithm, baseActivity)
                            },
                            {
                                toastText(activity, R.string.masterpassword_not_stored)
                            })
                    }
                    else {
                        createVault(pin, masterPasswd, iterations, cipherAlgorithm, baseActivity)
                    }

                }
            }

        }
    }


    private fun createVault(
        pin: Password,
        masterPasswd: Password,
        pbkdfIterations: Int,
        cipherAlgorithm: CipherAlgorithm,
        activity: BaseActivity
    ) {

        val input = CreateVaultUseCase.Input(
            LoginData(pin, masterPasswd),
            pbkdfIterations,
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

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        cipherAlgorithm = CipherAlgorithm.supportedValues()[position]
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        cipherAlgorithm = DEFAULT_CIPHER_ALGORITHM
    }
}


