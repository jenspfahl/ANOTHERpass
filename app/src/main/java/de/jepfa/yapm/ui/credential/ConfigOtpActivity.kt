package de.jepfa.yapm.ui.credential

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
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.widget.addTextChangedListener
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.OtpData
import de.jepfa.yapm.model.otp.OTPAlgorithm
import de.jepfa.yapm.model.otp.OTPConfig
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_ALGORITHM
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_COUNTER
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_DIGITS
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_MODE
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_PERIOD
import de.jepfa.yapm.model.otp.OTPMode
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.DebugInfo


class ConfigOtpActivity : SecureActivity() {


    private lateinit var otpModeSelection: AppCompatSpinner
    private lateinit var otpAlgorithmSelection: AppCompatSpinner
    private lateinit var sharedSecretEditText: EditText
    private lateinit var counterOrPeriodEditText: EditText
    private lateinit var digitsEditText: EditText
    private lateinit var counterOrPeriodTextView: TextView
    private lateinit var otpAuthTextView: TextView

    private var issuer: String? = null
    private var account: String? = null
    private var secret: Key? = null
    private var otpMode = DEFAULT_OTP_MODE
    private var otpAlgorithm = DEFAULT_OTP_ALGORITHM
    private var counter = DEFAULT_OTP_COUNTER
    private var period = DEFAULT_OTP_PERIOD


    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_otp)



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
                if (otpMode != newOtpMode) {
                    otpMode = newOtpMode

                    updateCounterOrPeriodTextView()
                    updateOtpAuthTextView()
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                otpMode = DEFAULT_OTP_MODE
                updateOtpAuthTextView()
            }
        }
        otpModeSelection.setSelection(otpMode.ordinal)


        otpAlgorithmSelection = findViewById(R.id.otp_algo_selection)
        val otpAlgorithmSelectionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            OTPAlgorithm.entries
                .map { it.uiName})
        otpAlgorithmSelection.adapter = otpAlgorithmSelectionAdapter
        otpAlgorithmSelection.onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                otpAlgorithm = OTPAlgorithm.entries[position]
                updateOtpAuthTextView()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                otpAlgorithm = DEFAULT_OTP_ALGORITHM
                updateOtpAuthTextView()
            }
        }
        otpAlgorithmSelection.setSelection(otpAlgorithm.ordinal)


        counterOrPeriodTextView = findViewById(R.id.text_otp_counter_or_period)
        sharedSecretEditText = findViewById(R.id.edit_otp_shared_secret)
        sharedSecretEditText.addTextChangedListener {
            updateOtpAuthTextView()
        }

        counterOrPeriodEditText = findViewById(R.id.edit_otp_counter_or_period)
        counterOrPeriodEditText.addTextChangedListener {
            updateOtpAuthTextView()
        }

        digitsEditText = findViewById(R.id.edit_otp_digits)
        digitsEditText.addTextChangedListener {
            updateOtpAuthTextView()
        }
        digitsEditText.setText(DEFAULT_OTP_DIGITS.toString())

        otpAuthTextView = findViewById(R.id.otpauth_text)

        val saveButton: Button = findViewById(R.id.button_save)
        saveButton.setOnClickListener {

            finish()

        }

        updateCounterOrPeriodTextView()
        loadOTP()

    }

    private fun updateOtpAuthTextView() {
        val cDigits = digitsEditText.text.toString().toIntOrNull()
        val cCounterOrPeriod = counterOrPeriodEditText.text.toString().toIntOrNull()
        if (cDigits != null && cCounterOrPeriod != null) {
            secret?.let {
                val newOTP = OTPConfig(
                    otpMode,
                    if (account.isNullOrBlank()) "" else account!!,
                    issuer ?: "",
                    it,
                    otpAlgorithm,
                    cDigits,
                    cCounterOrPeriod,
                )
                otpAuthTextView.text = newOTP.toUri().toString()
                updateCounterOrPeriodTextView()
            }
        }
    }

    private fun updateCounterOrPeriodTextView() {
        if (otpMode == OTPMode.HOTP) {
            counterOrPeriodTextView.setText("Current counter value:")
            period = counterOrPeriodEditText.text.toString().toIntOrNull() ?: DEFAULT_OTP_PERIOD
            counterOrPeriodEditText.setText(counter.toString())
        }
        else if (otpMode == OTPMode.TOTP) {
            counterOrPeriodTextView.setText("Renew-Period on seconds:")
            counter = counterOrPeriodEditText.text.toString().toIntOrNull() ?: DEFAULT_OTP_COUNTER
            counterOrPeriodEditText.setText(period.toString())

        }

    }

    private fun loadOTP() {

        val currentCredential = EncCredential.fromIntent(intent)

        masterSecretKey?.let { key ->

            issuer = SecretService.decryptCommonString(key, currentCredential.name)
            account = SecretService.decryptCommonString(key, currentCredential.user)

            currentCredential.otpData?.let { otpData ->
                val otpConfig = loadOtpConfig(key, otpData) ?: return

                otpMode = otpConfig.mode
                otpAlgorithm = otpConfig.algorithm
                secret = otpConfig.secret
                if (otpMode == OTPMode.HOTP) {
                    counter = otpConfig.periodOrCounter

                }
                else if (otpMode == OTPMode.TOTP) {
                    period = otpConfig.periodOrCounter
                }

                otpModeSelection.setSelection(otpMode.ordinal)
                otpAlgorithmSelection.setSelection(otpAlgorithm.ordinal)

                sharedSecretEditText.setText(otpConfig.secretAsBase32())
                counterOrPeriodEditText.setText(otpConfig.periodOrCounter.toString())
                digitsEditText.setText(otpConfig.digits.toString())

                updateOtpAuthTextView()
            }

        }

    }

    private fun loadOtpConfig(
        key: SecretKeyHolder,
        otpData: OtpData
    ): OTPConfig? {
        val otpUriString = SecretService.decryptCommonString(key, otpData.encOtpAuthUri)
        try {
            val otpUri = Uri.parse(otpUriString)
            //val otpUri = Uri.parse("otpauth://hotp/ACME%20Co:john.doe@email.com?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ&issuer=ACME%20Co&algorithm=SHA256&digits=6&counter=1")
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

        if (id == R.id.menu_delete_label) {


            return true
        }


        return super.onOptionsItemSelected(item)
    }


    override fun lock() {
        finish()
    }


}