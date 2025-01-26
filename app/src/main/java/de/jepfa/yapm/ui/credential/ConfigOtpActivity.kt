package de.jepfa.yapm.ui.credential

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.widget.addTextChangedListener
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncCredential.Companion.EXTRA_CREDENTIAL_OTP_DATA
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.otp.OTPAlgorithm
import de.jepfa.yapm.model.otp.OTPConfig
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_ALGORITHM
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_COUNTER
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_DIGITS
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_MODE
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_PERIOD
import de.jepfa.yapm.model.otp.OTPConfig.Companion.stringToBase32Key
import de.jepfa.yapm.model.otp.OTPMode
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.importread.ReadQrCodeOrNfcActivityBase
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.ClipboardUtil
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.putEncryptedExtra
import de.jepfa.yapm.util.toastText


class ConfigOtpActivity : ReadQrCodeOrNfcActivityBase() {


    private lateinit var otpViewer: OtpViewer

    private lateinit var qrCodeScannerImageView: ImageView
    private lateinit var otpModeSelection: AppCompatSpinner
    private lateinit var otpAlgorithmSelection: AppCompatSpinner
    private lateinit var sharedSecretEditText: EditText
    private lateinit var counterOrPeriodEditText: EditText
    private lateinit var digitsEditText: EditText
    private lateinit var counterOrPeriodTextView: TextView
    private lateinit var otpAuthTextView: TextView

    private var lastCounter = DEFAULT_OTP_COUNTER
    private var lastPeriod = DEFAULT_OTP_PERIOD


    init {
        enableBack = true
        onlyQrCodeScan = true

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        otpViewer = OtpViewer(createEmptyOtpConfig(), this) {
            counterOrPeriodEditText.setText(otpViewer.otpConfig?.periodOrCounter.toString())
            updateOtpAuthTextView()
        }

        qrCodeScannerImageView = findViewById(R.id.imageview_scan_qrcode)
        otpAuthTextView = findViewById(R.id.otpauth_text)
        if (!DebugInfo.isDebug) {
            otpAuthTextView.visibility = View.GONE
        }


        otpModeSelection = findViewById(R.id.otp_mode_selection)
        val otpModeSelectionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            OTPMode.entries
                .map { getString(it.uiLabel)})
        otpModeSelection.adapter = otpModeSelectionAdapter
        otpModeSelection.onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newOtpMode = OTPMode.entries[position]
                if (otpViewer.otpConfig?.mode != newOtpMode) {
                    otpViewer.otpConfig?.mode = newOtpMode

                    updateCounterOrPeriodLabels()
                    stashLastCounterOrPeriodContent()
                    updateOtpAuthTextView()
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                otpViewer.otpConfig?.mode = DEFAULT_OTP_MODE
                updateOtpAuthTextView()
            }
        }
        otpViewer.otpConfig?.mode?.ordinal?.let { otpModeSelection.setSelection(it) }


        otpAlgorithmSelection = findViewById(R.id.otp_algo_selection)
        val otpAlgorithmSelectionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            OTPAlgorithm.entries
                .map { it.uiName})
        otpAlgorithmSelection.adapter = otpAlgorithmSelectionAdapter
        otpAlgorithmSelection.onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                otpViewer.otpConfig?.algorithm = OTPAlgorithm.entries[position]
                updateOtpAuthTextView()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                otpViewer.otpConfig?.algorithm = DEFAULT_OTP_ALGORITHM
                updateOtpAuthTextView()
            }
        }
        otpViewer.otpConfig?.algorithm?.ordinal?.let { otpAlgorithmSelection.setSelection(it) }


        counterOrPeriodTextView = findViewById(R.id.text_otp_counter_or_period)
        sharedSecretEditText = findViewById(R.id.edit_otp_shared_secret)
        sharedSecretEditText.addTextChangedListener { text ->
            otpViewer.otpConfig?.secret = stringToBase32Key(text.toString())
            updateOtpAuthTextView()
        }

        counterOrPeriodEditText = findViewById(R.id.edit_otp_counter_or_period)
        counterOrPeriodEditText.addTextChangedListener { text ->
            val value = text.toString().toIntOrNull()
            if (value != null) {
                otpViewer.otpConfig?.periodOrCounter = value
                updateOtpAuthTextView()
            }
        }
        otpViewer.otpConfig?.periodOrCounter?.let { counterOrPeriodEditText.setText(it.toString()) }

        digitsEditText = findViewById(R.id.edit_otp_digits)
        digitsEditText.addTextChangedListener { text ->
            val value = text.toString().toIntOrNull()
            if (value != null) {
                otpViewer.otpConfig?.digits = value
                updateOtpAuthTextView()
            }
        }
        otpViewer.otpConfig?.digits?.let { digitsEditText.setText(it.toString()) }

        val saveButton: Button = findViewById(R.id.button_save)
        saveButton.setOnClickListener {

            if (sharedSecretEditText.text.isBlank()) {
                sharedSecretEditText.requestFocus()
                toastText(this, R.string.otp_error_secret_not_in_base32)
                return@setOnClickListener
            }

            if (counterOrPeriodEditText.text.isBlank()) {
                counterOrPeriodEditText.requestFocus()
                toastText(this, R.string.error_field_required)
                return@setOnClickListener
            }

            if (counterOrPeriodEditText.text.toString().toIntOrNull() ?: 0 <= 0) {
                counterOrPeriodEditText.requestFocus()
                toastText(this, R.string.error_value_greater_zero_required)
                return@setOnClickListener
            }

            if (digitsEditText.text.isBlank()) {
                digitsEditText.requestFocus()
                toastText(this, R.string.error_field_required)
                return@setOnClickListener
            }

            if (digitsEditText.text.toString().toIntOrNull() ?: 0 <= 0) {
                digitsEditText.requestFocus()
                toastText(this, R.string.error_value_greater_zero_required)
                return@setOnClickListener
            }

            masterSecretKey?.let { key ->
                val otpToSave = otpViewer.otpConfig
                if (otpToSave != null) {
                    val encOtpData =
                        SecretService.encryptCommonString(key, otpToSave.toUri().toString())

                    val data = Intent()
                    data.putEncryptedExtra(EXTRA_CREDENTIAL_OTP_DATA, encOtpData)

                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
                else {
                    toastText(this, getString(R.string.otp_cannot_apply))
                }
            }


        }

        val otpToRestore = savedInstanceState?.getString("OTP")
        if (otpToRestore != null) {
            val otpConfig = OTPConfig.fromUri(Uri.parse(otpToRestore))
            otpConfig?.let {
                otpViewer.otpConfig = it
                updateUIFromOTPConfig(it)
            }
        }
        else {
            loadOtpFromIntent()
        }

        updateCounterOrPeriodLabels()


        otpViewer.start()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        otpViewer.otpConfig?.let {
            outState.putString("OTP", it.toUri().toString())
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        otpViewer.stop()
    }

    private fun updateOtpAuthTextView() {
        val otpConfig = otpViewer.otpConfig
        if (otpConfig == null) {
            otpAuthTextView.text = ""
        }
        else {
            otpAuthTextView.text = otpConfig.toUri().toString()
        }

        otpViewer.refreshControllerVisibility()
    }


    private fun updateCounterOrPeriodLabels() {
        if (otpViewer.otpConfig?.mode == OTPMode.HOTP) {
            counterOrPeriodTextView.text = getString(R.string.otp_current_counter)
        }
        else if (otpViewer.otpConfig?.mode == OTPMode.TOTP) {
            counterOrPeriodTextView.text = getString(R.string.otp_period_in_seconds)
        }

    }

    private fun stashLastCounterOrPeriodContent() {
        if (otpViewer.otpConfig?.mode == OTPMode.HOTP) {
            lastPeriod = counterOrPeriodEditText.text.toString().toIntOrNull() ?: DEFAULT_OTP_PERIOD
            counterOrPeriodEditText.setText(lastCounter.toString())
        }
        else if (otpViewer.otpConfig?.mode == OTPMode.TOTP) {
            lastCounter =
                counterOrPeriodEditText.text.toString().toIntOrNull() ?: DEFAULT_OTP_COUNTER
            counterOrPeriodEditText.setText(lastPeriod.toString())

        }

    }

    private fun createEmptyOtpConfig(): OTPConfig? {

        val currentCredential = EncCredential.fromIntent(intent)

        masterSecretKey?.let { key ->

            val issuer = SecretService.decryptCommonString(key, currentCredential.name)
            val account = SecretService.decryptCommonString(key, currentCredential.user)

            return OTPConfig(
                DEFAULT_OTP_MODE,
                account,
                issuer,
                Key.empty(),
                DEFAULT_OTP_ALGORITHM,
                DEFAULT_OTP_PERIOD,
                DEFAULT_OTP_DIGITS
                )
        }

        return null

    }

    private fun loadOtpFromIntent() {

        val currentCredential = EncCredential.fromIntent(intent)

        masterSecretKey?.let { key ->

            val issuer = SecretService.decryptCommonString(key, currentCredential.name)
            val account = SecretService.decryptCommonString(key, currentCredential.user)

            currentCredential.otpData?.let { otpData ->
                val otpUriString = SecretService.decryptCommonString(key, otpData.encOtpAuthUri)
                val otpConfig = createOtpConfigFromUri(otpUriString)
                if (otpConfig != null) {
                    otpConfig.issuer = issuer ?: ""
                    otpConfig.account = account ?: ""
                    updateUIFromOTPConfig(otpConfig)
                }
                else {
                    toastText(this, R.string.otp_cannot_load_config)
                }
            }

        }

    }

    private fun updateUIFromOTPConfig(
        otpConfig: OTPConfig
    ) {

        if (otpConfig.mode == OTPMode.HOTP) {
            lastCounter = otpConfig.periodOrCounter

        } else if (otpConfig.mode == OTPMode.TOTP) {
            lastPeriod = otpConfig.periodOrCounter
        }

        otpModeSelection.setSelection(otpConfig.mode.ordinal)
        otpAlgorithmSelection.setSelection(otpConfig.algorithm.ordinal)

        sharedSecretEditText.setText(otpConfig.secretAsBase32())
        counterOrPeriodEditText.setText(otpConfig.periodOrCounter.toString())
        digitsEditText.setText(otpConfig.digits.toString())

        updateOtpAuthTextView()
    }

    private fun createOtpConfigFromUri(
        otpUriString: String
    ): OTPConfig? {
        try {
            val otpUri = Uri.parse(otpUriString)
            val otpConfig = OTPConfig.fromUri(otpUri)
            return otpConfig
        } catch (e: Exception) {
            DebugInfo.logException("OTP", "Cannot parse URI $otpUriString", e)
            return null
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (Session.isDenied()) {
            return false
        }

        menuInflater.inflate(R.menu.menu_configure_otp, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return false
        }

        if (id == R.id.menu_copy_otpauth) {
            val otpConfig = otpViewer.otpConfig
            if (otpConfig != null) {
                ClipboardUtil.copy("OTP-Config", otpConfig.toUri().toString(), this, isSensible = true)
                toastText(this, R.string.copied_to_clipboard)
            }
            else {
                toastText(this, R.string.otp_cannot_export)
            }

            return true
        }

        if (id == R.id.menu_export_otp) {

            val otpConfig = otpViewer.otpConfig
            if (otpConfig == null) {
                toastText(this, R.string.otp_cannot_export)
                return false
            }

            val tempKey = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, this)

            val encHead =
                SecretService.encryptCommonString(
                    tempKey,
                    getString(R.string.otp_config_headline)
                )
            val encSub =
                SecretService.encryptCommonString(tempKey, getString(R.string.otp_exported_otp_desc))
            val encQrcHeader = SecretService.encryptCommonString(
                tempKey,
                otpConfig.getLabel())
            val encQrc = SecretService.encryptCommonString(tempKey, otpConfig.toUri().toString())

            val intent = Intent(this, QrCodeActivity::class.java)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_HEADLINE, encHead)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE_HEADER, encQrcHeader)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE, encQrc)
            intent.putExtra(QrCodeActivity.EXTRA_COLOR, Color.RED)

            // will be bypassed to NfcActivity
            intent.putExtra(NfcActivity.EXTRA_WITH_APP_RECORD, true)

            startActivity(intent)

            return true
        }

        if (id == R.id.menu_delete_otp) {

            val otpConfig = otpViewer.otpConfig
            if (otpConfig == null) {
                toastText(this, getString(R.string.nothing_to_delete))
                return false
            }

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.otp_remove_config_title, otpConfig.getLabel()))
                .setMessage(R.string.otp_remove_config_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialog, _ ->
                    dialog.dismiss()
                    masterSecretKey?.let { key ->

                        val data = Intent()
                        data.putEncryptedExtra(EXTRA_CREDENTIAL_OTP_DATA, Encrypted.empty())

                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                }
                .setNegativeButton(android.R.string.no, null)
                .show()

            return true
        }


        return super.onOptionsItemSelected(item)
    }


    override fun lock() {
        finish()
    }

    override fun getLayoutId() = R.layout.activity_config_otp

    override fun handleScannedData(scanned: String) {
        val otpConfig = createOtpConfigFromUri(scanned)
        if (otpConfig != null) {
            updateUIFromOTPConfig(otpConfig)
        }
        else {
            toastText(this, getString(R.string.otp_cannot_scann_otp_config))
        }
    }


}


